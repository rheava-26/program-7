package dev.rheava.program7.director;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.rheava.program7.Program7;
import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.entity.AirUAVEntity;
import dev.rheava.program7.entity.IndirectFireUnit;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.entity.ReconHelicopterEntity;
import dev.rheava.program7.entity.ScoutCarEntity;
import dev.rheava.program7.entity.SurveyorDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The Director organ for indirect fire — the artillery doc's
 * {@code FireMissionManager} (see {@code ARTILLERY_AND_INDIRECT_FIRE.md} §8).
 * Sibling of {@link VirtualFleet} and {@link SupplyNetwork}: a plain class,
 * owned field on {@link ProgramDirectorState}, ticked from {@code
 * ProgramDirectorState.tick}, with its own NBT round-trip.
 *
 * <p>It owns a fire mission per firing unit through the doc's §2 loop:
 * acquisition (three target sources — a live observer, counter-battery, or a
 * player dwell heatmap), ranging walk-in, and the accuracy model (§3) whose
 * error radius grows with <em>chunk</em> distance as a hard floor and tightens
 * with spotting and ranging progress. Any mount implementing {@link
 * IndirectFireUnit} is a client — the howitzer and mortar first, with the
 * MLRS, naval, and missile families riding the same generic loop.
 *
 * <p><b>Battery fire</b> (doc §4 "several tubes ranging and firing together
 * on one target"): when several same-{@link IndirectFireUnit#battery()}
 * tubes stand within {@link #BATTERY_RADIUS} of each other and would
 * otherwise be assigned the same designation, they're handed the exact same
 * {@link FireMission} object rather than independent copies — every tube's
 * shot advances the shared ranging progress, so a battery walks fire in
 * faster and fires for effect together, denser than any one tube alone.
 *
 * <p>A mission whose tube is loaded fires through its own attack goal
 * (real shell entity, real magazine); one whose tube has unloaded is picked
 * up here instead by {@link #resolveOffscreenMissions} (doc §7 "off-screen
 * statistical resolution") — no real projectile, just a rolled impact,
 * optional real damage if the target chunk happens to be loaded, and always
 * an acoustic report, so a distant war keeps making noise even with nobody
 * standing at either end of it. Active missions and the dwell grid persist
 * in NBT so a restart mid-barrage resolves rather than dropping shells into
 * the void, the same discipline as {@link VirtualFleet}.
 */
public final class FireMissionManager {
	/** Reassess external target sources / missions on this cadence (~1s). Self-observed fire is refreshed live from the goal every tick. */
	private static final int SCAN_INTERVAL = 20;
	/** Tubes of the same battery key within this many blocks of each other share one mission. */
	private static final double BATTERY_RADIUS = 32.0;

	// ---- Accuracy model (§3 CEP). error = baseSpread / (spotting * ranging), floored at baseSpread-scaled range term so range can never be fully cancelled. ----
	/** How fast the error floor grows with distance, measured in CHUNKS (tight up close, loose far). */
	private static final double RANGE_FACTOR_PER_CHUNK = 0.6;
	/** A live observer (eyes-on) divides the error by this — spotting tightens, but see {@link #RANGE_FLOOR_FRACTION}. */
	private static final double SPOTTING_TIGHTEN = 1.8;
	/** The hard floor: even a perfectly spotted, fully ranged shot keeps this fraction of its range-driven spread. Long fire always scatters. */
	private static final double RANGE_FLOOR_FRACTION = 0.3;
	/** rangingProgress climbs 1→this as fire is walked in (§2 step 3); each step tightens the error. */
	private static final int MAX_RANGING_STEPS = 4;

	/** Target drifts this far from where ranging was last anchored → the walk-in resets to wide (§2: moving makes you a worse target). */
	private static final double RANGING_RESET_DISTANCE = 6.0;

	/** No refresh for this long → the mission goes stale: fire falls back to wide, unobserved (doc §2 "losing the observer kicks it back to ranging"). */
	private static final int MISSION_STALE_TICKS = 60;
	/** No refresh for this long → the mission is dropped entirely. */
	private static final int MISSION_DROP_TICKS = 200;

	// ---- Off-screen statistical resolution (§7): a mission whose tube isn't loaded still fires, on a coarse generic cadence, without a real shell entity. ----
	/** How often (ticks) an off-screen mission rolls a statistical shot — coarser than any real tube's own cadence, since this is background simulation, not the main event. */
	private static final int OFFSCREEN_FIRE_INTERVAL = 200;
	// Off-screen impact audio reuses P7Sounds.MORTAR_IMPACT's own registered
	// fixed audible range (192 blocks, see P7Sounds) via ProgramAcoustics —
	// no separate radius constant needed here.
	//
	// Off-screen shooter-position estimate fallback: the target itself, since
	// an unloaded tube's exact position isn't tracked once its entity unloads.
	// Range/spotting terms in currentSpread degrade to "just the base spread"
	// when shooter and target coincide, which is an acceptable simplification
	// for a barrage the player isn't anywhere near anyway.

	// ---- Counter-battery (§2 acquisition #3): a far-off projectile hit publishes its shooter's position as a short-lived candidate. ----
	/** A hit closer than this isn't counter-battery — it's just a fight, handled by direct fire. */
	public static final double COUNTER_BATTERY_MIN_DISTANCE = 24.0;
	/** How long a recorded counter-battery origin stays a candidate. 10s. */
	private static final int COUNTER_BATTERY_LIFETIME = 200;

	// ---- Player dwell heatmap (§2 acquisition #2): a coarse per-chunk "time spent here" accumulator that decays when the player leaves. ----
	/** Heat a player adds to its own chunk each scan. */
	private static final int DWELL_GAIN = 2;
	/** Per-cell heat cap. */
	private static final int DWELL_MAX = 40;
	/** Heat an unoccupied cell sheds each scan. */
	private static final int DWELL_DECAY = 1;
	/** A cell this hot is a candidate fire position even with no live observer — "standing still makes you a better target." */
	private static final int DWELL_HOT_THRESHOLD = 24;
	/** Hard cap on the grid so it stays lightweight; the coldest cells are pruned past this. */
	private static final int DWELL_MAX_CELLS = 256;

	// Priority when a tube can see several candidate sources: live observer > counter-battery > dwell.
	private static final int PRIORITY_OBSERVER = 3;
	private static final int PRIORITY_COUNTER = 2;
	private static final int PRIORITY_DWELL = 1;

	private int tickCounter = 0;
	/** Active missions, keyed by the firing unit's UUID. Battery-mates share a value reference (see class doc). */
	private final Map<UUID, FireMission> missions = new HashMap<>();
	/** Live counter-battery candidates, keyed by the offending player's UUID. */
	private final Map<UUID, Candidate> counterBattery = new HashMap<>();
	/** The dwell heatmap, keyed by {@link ChunkPos#toLong()}. */
	private final Map<Long, DwellCell> dwell = new HashMap<>();

	/**
	 * Ticked every tick from the Director; internally throttled to one pass
	 * every {@link #SCAN_INTERVAL} ticks. Rebuilds the external target sources,
	 * (re)assigns missions to loaded tubes, and ages out stale state.
	 * Returns {@code true} when persistent state changed, so the caller knows
	 * whether to mark dirty.
	 */
	public boolean tick(ServerWorld world, ProgramDirectorState director) {
		this.tickCounter++;
		if (this.tickCounter < SCAN_INTERVAL) {
			return false;
		}
		this.tickCounter = 0;
		long now = world.getTime();

		boolean changed = this.expireCounterBattery(now);
		changed |= this.accumulateDwell(world);

		// One pass over the loaded entities: collect firing units (anything
		// implementing IndirectFireUnit) and any recon observer currently
		// holding eyes on a player (the live-eyes designation).
		List<Tube> tubes = new ArrayList<>();
		List<Designation> designations = new ArrayList<>();
		for (Entity entity : world.iterateEntities()) {
			if (entity instanceof MobEntity mob && mob instanceof IndirectFireUnit unit && mob.isAlive()) {
				tubes.add(new Tube(mob, unit));
			} else if (entity instanceof ProgramDroneEntity drone && isObserver(drone)) {
				ProgramDroneEntity.AlertState alert = drone.getAlertState();
				if (alert != ProgramDroneEntity.AlertState.TRACKING
						&& alert != ProgramDroneEntity.AlertState.ENGAGING) {
					continue;
				}
				LivingEntity spotted = drone.getTarget();
				if (spotted instanceof PlayerEntity && spotted.isAlive()) {
					designations.add(new Designation(spotted.getPos(), spotted.getUuid(),
							true, PRIORITY_OBSERVER));
				}
			}
		}

		// Counter-battery origins and hot dwell cells round out the candidate set.
		for (Candidate candidate : this.counterBattery.values()) {
			designations.add(new Designation(candidate.pos, candidate.playerId, false, PRIORITY_COUNTER));
		}
		for (Map.Entry<Long, DwellCell> entry : this.dwell.entrySet()) {
			DwellCell cell = entry.getValue();
			if (cell.heat < DWELL_HOT_THRESHOLD) {
				continue;
			}
			ChunkPos chunk = new ChunkPos(entry.getKey());
			Vec3d pos = new Vec3d(chunk.getStartX() + 8.0, cell.y, chunk.getStartZ() + 8.0);
			designations.add(new Designation(pos, cell.playerId, false, PRIORITY_DWELL));
		}

		changed |= this.assignMissions(tubes, designations, now);
		changed |= this.resolveOffscreenMissions(world, director, now);
		changed |= this.pruneMissions(world, now);
		return changed;
	}

	/**
	 * The doc's §7 off-screen statistical resolution: a mission whose tube's
	 * chunk isn't currently loaded can't rely on its attack goal ticking (AI
	 * only runs on loaded entities), so it would otherwise just sit there
	 * doing nothing until a player wanders back into range. Instead, on a
	 * coarse generic cadence (no real magazine to check — the tube entity
	 * itself isn't loaded to ask), roll a shot statistically: work out an
	 * impact point from the mission's own CEP, apply real block/entity
	 * damage if the target's chunk happens to be loaded (typically because a
	 * <em>different</em> player is standing there even though the gun itself
	 * is far off), and always broadcast the report so a distant player can
	 * hear "a barrage over the horizon" even with neither chunk loaded.
	 *
	 * <p><b>Known simplification (v1):</b> unlike a loaded tube, this never
	 * debits the tube's own onboard magazine (inaccessible on an unloaded
	 * entity) — an off-screen mission fires for free rather than truly
	 * starving out. It also doesn't defer/replay damage once an unloaded
	 * target chunk later loads (the doc's "applies... if and when that area
	 * loads"); a shot rolled while the target is unloaded is simply a report
	 * with no physical effect. Both are documented gaps, not silent bugs.
	 */
	private boolean resolveOffscreenMissions(ServerWorld world, ProgramDirectorState director, long now) {
		if (this.missions.isEmpty()) {
			return false;
		}
		boolean changed = false;
		for (Map.Entry<UUID, FireMission> entry : this.missions.entrySet()) {
			if (world.getEntity(entry.getKey()) != null) {
				// Loaded — its own attack goal handles real firing.
				continue;
			}
			FireMission mission = entry.getValue();
			if (mission.targetPos == null) {
				continue;
			}
			mission.offscreenCooldown -= SCAN_INTERVAL;
			if (mission.offscreenCooldown > 0) {
				continue;
			}
			mission.offscreenCooldown = OFFSCREEN_FIRE_INTERVAL;
			this.resolveStatisticalShot(world, director, mission);
			changed = true;
		}
		return changed;
	}

	/**
	 * Rolls one off-screen shot for {@code mission}: a scattered impact point
	 * around its target (the shooter's exact position isn't tracked once its
	 * entity unloads, so the spread math degrades to "just the mission's own
	 * CEP" rather than a true range-scaled figure — an acceptable
	 * simplification for a barrage the player isn't anywhere near). Real
	 * damage only lands if the impact's chunk is loaded; the audio always
	 * broadcasts.
	 */
	private void resolveStatisticalShot(ServerWorld world, ProgramDirectorState director, FireMission mission) {
		Vec3d target = mission.targetPos;
		double spread = this.currentSpread(mission, target);
		Vec3d impact = target.add(
				(world.getRandom().nextDouble() * 2.0 - 1.0) * spread,
				0.0,
				(world.getRandom().nextDouble() * 2.0 - 1.0) * spread);

		ProgramAcoustics.emit(world, impact, P7Sounds.MORTAR_IMPACT.get(), SoundCategory.HOSTILE, 1.6f, 0.7f);

		BlockPos impactBlock = BlockPos.ofFloored(impact);
		boolean chunkLoaded = world.getChunk(impactBlock.getX() >> 4, impactBlock.getZ() >> 4,
				ChunkStatus.FULL, false) != null;
		if (chunkLoaded) {
			World.ExplosionSourceType sourceType = Program7.CONFIG.terrainDestruction
					? World.ExplosionSourceType.MOB
					: World.ExplosionSourceType.NONE;
			world.createExplosion(null, impact.x, impact.y, impact.z, mission.impactPower, sourceType);
			director.getTerrainSaturation().recordImpact(world, impactBlock, mission.impactPower);
		}

		this.onShotFired(mission);
	}

	/** The recon layer the doc points at — anything carrying the AlertState ramp as a spotter, not a tube itself. */
	private static boolean isObserver(ProgramDroneEntity drone) {
		return drone instanceof SurveyorDroneEntity || drone instanceof AirUAVEntity
				|| drone instanceof ReconHelicopterEntity || drone instanceof ScoutCarEntity;
	}

	private boolean expireCounterBattery(long now) {
		if (this.counterBattery.isEmpty()) {
			return false;
		}
		return this.counterBattery.values().removeIf(candidate -> candidate.expiryTick <= now);
	}

	/**
	 * Fold every online player's current chunk into the heatmap, then decay the
	 * cells nobody touched this scan and prune the grid back under its cap. A
	 * cell records the player's last Y so a dwell-driven shot lands at the
	 * right height, not just the right column.
	 */
	private boolean accumulateDwell(ServerWorld world) {
		boolean changed = false;
		Set<Long> touched = new HashSet<>();
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.isSpectator()) {
				continue;
			}
			long key = player.getChunkPos().toLong();
			DwellCell cell = this.dwell.computeIfAbsent(key, k -> new DwellCell());
			cell.heat = Math.min(DWELL_MAX, cell.heat + DWELL_GAIN);
			cell.y = player.getY();
			cell.playerId = player.getUuid();
			touched.add(key);
			changed = true;
		}

		Iterator<Map.Entry<Long, DwellCell>> iterator = this.dwell.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Long, DwellCell> entry = iterator.next();
			if (touched.contains(entry.getKey())) {
				continue;
			}
			entry.getValue().heat -= DWELL_DECAY;
			changed = true;
			if (entry.getValue().heat <= 0) {
				iterator.remove();
			}
		}

		if (this.dwell.size() > DWELL_MAX_CELLS) {
			this.pruneColdestCells();
			changed = true;
		}
		return changed;
	}

	/** Drop the coldest cells until the grid is back at its cap. */
	private void pruneColdestCells() {
		while (this.dwell.size() > DWELL_MAX_CELLS) {
			Long coldestKey = null;
			int coldest = Integer.MAX_VALUE;
			for (Map.Entry<Long, DwellCell> entry : this.dwell.entrySet()) {
				if (entry.getValue().heat < coldest) {
					coldest = entry.getValue().heat;
					coldestKey = entry.getKey();
				}
			}
			if (coldestKey == null) {
				return;
			}
			this.dwell.remove(coldestKey);
		}
	}

	/**
	 * For every loaded tube that isn't already self-observing a live target of
	 * its own, pick the highest-priority (then nearest) in-range designation
	 * and drive its mission onto it — sharing a battery-mate's mission object
	 * when one's already firing on the same designation nearby (§4 battery
	 * fire).
	 */
	private boolean assignMissions(List<Tube> tubes, List<Designation> designations, long now) {
		if (tubes.isEmpty() || designations.isEmpty()) {
			return false;
		}
		boolean changed = false;
		for (Tube tube : tubes) {
			LivingEntity own = tube.mob().getTarget();
			if (own != null && own.isAlive()) {
				// It has line of sight itself — the goal self-designates every
				// tick; the manager leaves that mission alone here.
				continue;
			}
			Vec3d shooter = tube.mob().getPos();
			Designation best = null;
			double bestDistance = 0.0;
			for (Designation designation : designations) {
				double dx = designation.pos().x - shooter.x;
				double dz = designation.pos().z - shooter.z;
				double distance = Math.sqrt(dx * dx + dz * dz);
				if (distance < tube.spec().indirectMinRange() || distance > tube.spec().indirectMaxRange()) {
					continue;
				}
				if (best == null || designation.priority() > best.priority()
						|| (designation.priority() == best.priority() && distance < bestDistance)) {
					best = designation;
					bestDistance = distance;
				}
			}
			if (best != null) {
				FireMission mission = this.missions.get(tube.mob().getUuid());
				if (mission == null) {
					FireMission batteryMate = this.findBatteryMate(tube, best, now);
					mission = batteryMate != null ? batteryMate
							: new FireMission(tube.spec().indirectBaseSpread(), tube.spec().indirectImpactPower());
					this.missions.put(tube.mob().getUuid(), mission);
				}
				this.applyDesignation(mission, best, now);
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * Looks for another live tube already assigned a mission on the same
	 * target player as {@code designation}, of the same {@link
	 * IndirectFireUnit#battery()} key and within {@link #BATTERY_RADIUS} of
	 * {@code tube} — if found, its mission is returned so the new tube joins
	 * the same battery instead of starting its own independent ranging
	 * walk-in. The battery key/position lookup rides {@link
	 * #mateBatteryKeyCache}/{@link #mateBatteryPosCache}, small per-scan
	 * caches rebuilt every {@link #pruneMissions} pass (one scan interval,
	 * ~1s, behind live) so this doesn't need to re-resolve every mission's
	 * owning entity from the world on every assignment.
	 */
	@Nullable
	private FireMission findBatteryMate(Tube tube, Designation designation, long now) {
		String key = tube.spec().battery();
		if (key == null) {
			return null;
		}
		for (Map.Entry<UUID, FireMission> entry : this.missions.entrySet()) {
			if (entry.getKey().equals(tube.mob().getUuid())) {
				continue;
			}
			FireMission candidate = entry.getValue();
			if (candidate.targetPlayerId == null || !candidate.targetPlayerId.equals(designation.playerId())) {
				continue;
			}
			if (now - candidate.lastTouchTick > MISSION_STALE_TICKS) {
				continue;
			}
			if (!key.equals(this.mateBatteryKeyCache.get(entry.getKey()))) {
				continue;
			}
			Vec3d matePos = this.mateBatteryPosCache.get(entry.getKey());
			if (matePos == null || matePos.distanceTo(tube.mob().getPos()) > BATTERY_RADIUS) {
				continue;
			}
			return candidate;
		}
		return null;
	}

	// Small per-scan caches (rebuilt every pruneMissions pass) so
	// findBatteryMate can check a mission's originating tube's battery key
	// and position without threading extra state through FireMission itself
	// (which stays a pure target-state record, not a tube registry).
	private final Map<UUID, String> mateBatteryKeyCache = new HashMap<>();
	private final Map<UUID, Vec3d> mateBatteryPosCache = new HashMap<>();

	/** Point a mission at a fresh designation, resetting the walk-in when the spotting state flips or the target has moved. */
	private void applyDesignation(FireMission mission, Designation designation, long now) {
		if (mission.observed != designation.spotted()) {
			mission.rangingProgress = 1;
		}
		mission.observed = designation.spotted();
		mission.priority = designation.priority();
		mission.targetPlayerId = designation.playerId();
		mission.setTarget(designation.pos(), RANGING_RESET_DISTANCE);
		mission.lastTouchTick = now;
	}

	/**
	 * Age missions out: drop one whose firing unit is gone or long silent, and
	 * knock a merely-stale one back to wide/unobserved so a lost observer or a
	 * moved target undoes the walk-in.
	 */
	private boolean pruneMissions(ServerWorld world, long now) {
		this.mateBatteryKeyCache.clear();
		this.mateBatteryPosCache.clear();
		if (this.missions.isEmpty()) {
			return false;
		}
		boolean changed = false;
		Iterator<Map.Entry<UUID, FireMission>> iterator = this.missions.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, FireMission> entry = iterator.next();
			Entity shooter = world.getEntity(entry.getKey());
			// A loaded, confirmed-dead tube: its mission is truly over, drop it.
			// A null lookup means the tube's chunk is merely unloaded, NOT
			// that it's gone — keeping the mission is the whole reason it's
			// persisted in NBT (a barrage has to survive a restart or the tube's
			// chunk unloading). Those fall through to the idle TTL below, which
			// ages out a genuinely abandoned mission on its own.
			if (shooter instanceof MobEntity mob && mob instanceof IndirectFireUnit unit) {
				if (!mob.isAlive()) {
					iterator.remove();
					changed = true;
					continue;
				}
				this.mateBatteryKeyCache.put(entry.getKey(), unit.battery());
				this.mateBatteryPosCache.put(entry.getKey(), mob.getPos());
			}
			long idle = now - entry.getValue().lastTouchTick;
			if (idle > MISSION_DROP_TICKS) {
				iterator.remove();
				changed = true;
			} else if (idle > MISSION_STALE_TICKS
					&& (entry.getValue().observed || entry.getValue().rangingProgress > 1)) {
				entry.getValue().observed = false;
				entry.getValue().rangingProgress = 1;
				changed = true;
			}
		}
		return changed;
	}

	// ---- Client (attack goal) hooks ----------------------------------------------------------

	/**
	 * Called from a tube's attack goal every tick it holds a live line-of-sight
	 * target: self-observation is the top-priority, eyes-on source, so it
	 * (re)builds this unit's mission directly rather than waiting on the scan.
	 */
	public <T extends MobEntity & IndirectFireUnit> FireMission updateSelfObserved(T shooter, LivingEntity target,
			long now) {
		FireMission mission = this.missions.computeIfAbsent(shooter.getUuid(),
				id -> new FireMission(shooter.indirectBaseSpread(), shooter.indirectImpactPower()));
		this.applyDesignation(mission,
				new Designation(target.getPos(), target.getUuid(), true, PRIORITY_OBSERVER), now);
		return mission;
	}

	/** The mission currently assigned to {@code shooter}, or null if it has nothing to fire on. */
	@Nullable
	public FireMission missionFor(Entity shooter) {
		return this.missions.get(shooter.getUuid());
	}

	/**
	 * The current error radius (blocks) for {@code mission} fired from {@code
	 * shooterPos}, per the §3 model: a range floor that grows with chunk
	 * distance, divided by spotting and ranging progress but never below the
	 * floor.
	 */
	public double currentSpread(FireMission mission, Vec3d shooterPos) {
		Vec3d target = mission.targetPos;
		if (target == null) {
			return mission.baseSpread;
		}
		double dx = target.x - shooterPos.x;
		double dz = target.z - shooterPos.z;
		double chunks = Math.sqrt(dx * dx + dz * dz) / 16.0;
		double rangeScale = mission.baseSpread * (1.0 + chunks * RANGE_FACTOR_PER_CHUNK);
		double spotting = mission.observed ? SPOTTING_TIGHTEN : 1.0;
		double error = rangeScale / (spotting * mission.rangingProgress);
		double floor = rangeScale * RANGE_FLOOR_FRACTION;
		return Math.max(error, floor);
	}

	/**
	 * Report that {@code mission} just put a round downrange. Fire only walks
	 * in while an observer keeps eyes on (§2 step 3), so an unobserved mission
	 * (counter-battery / dwell, no live spotter) never tightens — it stays a
	 * wide ranging shot until a spotter arrives. Battery-mates share the same
	 * {@link FireMission} object, so any tube's shot advances the walk-in for
	 * the whole battery.
	 */
	public void onShotFired(FireMission mission) {
		if (mission != null && mission.observed && mission.rangingProgress < MAX_RANGING_STEPS) {
			mission.rangingProgress++;
		}
	}

	/**
	 * Record a far-off projectile hit's origin as a counter-battery candidate
	 * (§2 acquisition #3). Called from the Program-unit damage path; the
	 * candidate is a fire target until it ages out.
	 */
	public void reportCounterBattery(UUID shooterPlayerId, Vec3d origin, long now) {
		this.counterBattery.put(shooterPlayerId, new Candidate(origin, shooterPlayerId, now + COUNTER_BATTERY_LIFETIME));
	}

	public int activeMissionCount() {
		return this.missions.size();
	}

	// ---- Persistence ---------------------------------------------------------------------------

	public NbtCompound toNbt() {
		NbtCompound tag = new NbtCompound();

		NbtList missionList = new NbtList();
		for (Map.Entry<UUID, FireMission> entry : this.missions.entrySet()) {
			NbtCompound missionTag = entry.getValue().toNbt();
			missionTag.putUuid("Shooter", entry.getKey());
			missionList.add(missionTag);
		}
		tag.put("Missions", missionList);

		NbtList dwellList = new NbtList();
		for (Map.Entry<Long, DwellCell> entry : this.dwell.entrySet()) {
			NbtCompound cellTag = new NbtCompound();
			cellTag.putLong("Key", entry.getKey());
			cellTag.putInt("Heat", entry.getValue().heat);
			cellTag.putDouble("Y", entry.getValue().y);
			if (entry.getValue().playerId != null) {
				cellTag.putUuid("Player", entry.getValue().playerId);
			}
			dwellList.add(cellTag);
		}
		tag.put("Dwell", dwellList);
		return tag;
	}

	public void readNbt(NbtCompound nbt) {
		this.missions.clear();
		this.dwell.clear();
		this.counterBattery.clear();

		NbtList missionList = nbt.getList("Missions", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < missionList.size(); i++) {
			NbtCompound missionTag = missionList.getCompound(i);
			if (!missionTag.containsUuid("Shooter")) {
				continue;
			}
			// Battery-sharing is a live-scan optimization (see class doc); a
			// restart flattens shared missions back to one independent object
			// per shooter, which the next scan will re-share if they're still
			// firing on the same designation.
			this.missions.put(missionTag.getUuid("Shooter"), FireMission.fromNbt(missionTag));
		}

		NbtList dwellList = nbt.getList("Dwell", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < dwellList.size(); i++) {
			NbtCompound cellTag = dwellList.getCompound(i);
			DwellCell cell = new DwellCell();
			cell.heat = cellTag.getInt("Heat");
			cell.y = cellTag.getDouble("Y");
			if (cellTag.containsUuid("Player")) {
				cell.playerId = cellTag.getUuid("Player");
			}
			this.dwell.put(cellTag.getLong("Key"), cell);
		}
	}

	// ---- Data ----------------------------------------------------------------------------------

	/** One candidate firing unit for this scan pass — the loaded mob plus its {@link IndirectFireUnit} facet. */
	private record Tube(MobEntity mob, IndirectFireUnit spec) {
	}

	/** A candidate target position for one scan pass. */
	private record Designation(Vec3d pos, UUID playerId, boolean spotted, int priority) {
	}

	/** A live counter-battery origin, valid until {@code expiryTick}. */
	private static final class Candidate {
		final Vec3d pos;
		final UUID playerId;
		final long expiryTick;

		Candidate(Vec3d pos, UUID playerId, long expiryTick) {
			this.pos = pos;
			this.playerId = playerId;
			this.expiryTick = expiryTick;
		}
	}

	/** One dwell heatmap cell: accumulated heat plus the player's last Y in that chunk. */
	private static final class DwellCell {
		int heat;
		double y;
		@Nullable
		UUID playerId;
	}

	/**
	 * One indirect-fire mission's live state (doc §2/§3): where it's shooting,
	 * whether a spotter has eyes on, how far the walk-in has advanced, and the
	 * anchor position the walk-in resets against when the target moves. Shared
	 * by every tube in a battery (see class doc) — there is one of these per
	 * <em>mission</em>, not strictly per tube.
	 */
	public static final class FireMission {
		@Nullable
		private UUID targetPlayerId;
		@Nullable
		private Vec3d targetPos;
		/** Where ranging was last anchored — the target drifting far from here resets the walk-in. */
		@Nullable
		private Vec3d acquiredPos;
		private int rangingProgress = 1;
		private boolean observed;
		private int priority;
		private long lastTouchTick;
		/** This mission's signature CEP (§3) — set from the owning tube's {@link IndirectFireUnit#indirectBaseSpread()} when the mission is created. */
		private double baseSpread;
		/** Roughly how big a bang this mission's munition makes — set from {@link IndirectFireUnit#indirectImpactPower()}; used only by off-screen statistical resolution, which has no real shell entity to ask. */
		private float impactPower;
		/** Ticks until the next off-screen statistical shot, decremented only while this mission's tube is unloaded (see {@link #resolveOffscreenMissions}). */
		private int offscreenCooldown = OFFSCREEN_FIRE_INTERVAL;

		private FireMission(double baseSpread, float impactPower) {
			this.baseSpread = baseSpread;
			this.impactPower = impactPower;
		}

		@Nullable
		public Vec3d targetPos() {
			return this.targetPos;
		}

		/**
		 * The live target player's UUID, if this mission currently has one —
		 * the guided-missile family (see {@code MissileAttackGoal}) resolves
		 * this against the world to home in on the actual entity rather than
		 * a fixed point, when the mission is currently spotted (see {@link
		 * #isObserved()}).
		 */
		@Nullable
		public UUID targetPlayerId() {
			return this.targetPlayerId;
		}

		public boolean isObserved() {
			return this.observed;
		}

		public int rangingProgress() {
			return this.rangingProgress;
		}

		/**
		 * Slew onto a fresh target position. A drift beyond {@code resetDistance}
		 * from the ranging anchor re-anchors here and kicks the walk-in back to
		 * wide; a small nudge follows the target without losing progress.
		 */
		private void setTarget(Vec3d pos, double resetDistance) {
			if (this.acquiredPos == null || pos.distanceTo(this.acquiredPos) > resetDistance) {
				this.rangingProgress = 1;
				this.acquiredPos = pos;
			}
			this.targetPos = pos;
		}

		private NbtCompound toNbt() {
			NbtCompound tag = new NbtCompound();
			if (this.targetPlayerId != null) {
				tag.putUuid("Target", this.targetPlayerId);
			}
			if (this.targetPos != null) {
				tag.putDouble("TX", this.targetPos.x);
				tag.putDouble("TY", this.targetPos.y);
				tag.putDouble("TZ", this.targetPos.z);
			}
			if (this.acquiredPos != null) {
				tag.putDouble("AX", this.acquiredPos.x);
				tag.putDouble("AY", this.acquiredPos.y);
				tag.putDouble("AZ", this.acquiredPos.z);
			}
			tag.putInt("Ranging", this.rangingProgress);
			tag.putBoolean("Observed", this.observed);
			tag.putInt("Priority", this.priority);
			tag.putLong("LastTouch", this.lastTouchTick);
			tag.putDouble("BaseSpread", this.baseSpread);
			tag.putFloat("ImpactPower", this.impactPower);
			return tag;
		}

		private static FireMission fromNbt(NbtCompound tag) {
			FireMission mission = new FireMission(tag.contains("BaseSpread") ? tag.getDouble("BaseSpread") : 1.0,
					tag.contains("ImpactPower") ? tag.getFloat("ImpactPower") : 2.0f);
			if (tag.containsUuid("Target")) {
				mission.targetPlayerId = tag.getUuid("Target");
			}
			if (tag.contains("TX")) {
				mission.targetPos = new Vec3d(tag.getDouble("TX"), tag.getDouble("TY"), tag.getDouble("TZ"));
			}
			if (tag.contains("AX")) {
				mission.acquiredPos = new Vec3d(tag.getDouble("AX"), tag.getDouble("AY"), tag.getDouble("AZ"));
			}
			mission.rangingProgress = Math.max(1, tag.getInt("Ranging"));
			mission.observed = tag.getBoolean("Observed");
			mission.priority = tag.getInt("Priority");
			mission.lastTouchTick = tag.getLong("LastTouch");
			return mission;
		}
	}
}
