package dev.rheava.program7.director;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.rheava.program7.Program7;
import dev.rheava.program7.advancement.P7Advancements;
import dev.rheava.program7.block.AssemblerBlock;
import dev.rheava.program7.block.LaunchCatapultBlock;
import dev.rheava.program7.block.ProbeCoreBlockEntity;
import dev.rheava.program7.block.StorageDeckBlock;
import dev.rheava.program7.config.P7Config;
import dev.rheava.program7.entity.CourierUnit;
import dev.rheava.program7.entity.DropPodEntity;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.entity.SniperDroneEntity;
import dev.rheava.program7.registry.P7Blocks;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The Program Director — the single brain of the mod, persisted per world.
 *
 * <p>Everything the Program knows lives here: how threatening this world has
 * proven so far (global threat), a per-player intel file built from surveyor
 * scans, the insertion schedule for the first drop pod, and the queue of
 * pending unit dispatches. Later phases add the base registry, resource
 * ledger, production queues, and era progression.
 */
public class ProgramDirectorState extends PersistentState {
	private static final PersistentState.Type<ProgramDirectorState> TYPE = new PersistentState.Type<>(
			ProgramDirectorState::new, ProgramDirectorState::fromNbt, null);

	public static final int MAX_THREAT = 100;
	/** Earliest insertion: 2 in-game days, plus up to 1 day of drift. */
	private static final long MIN_LANDING_DELAY = 48000L;
	private static final int LANDING_DELAY_DRIFT = 24000;
	/** Pods/probes land at least this far from the anchoring player — a base within casual walking distance defeats the point. */
	private static final int LANDING_MIN_DISTANCE = 1000;
	private static final int LANDING_MAX_DISTANCE = 1600;
	/**
	 * Raze the last base and the invasion regroups rather than ending: the
	 * Program waits this long (4 in-game days, plus up to 2 of drift) before
	 * re-inserting a fresh pod near a player, the same as the opening drop.
	 */
	private static final long REINSERTION_DELAY = 4L * 24000L;
	private static final int REINSERTION_DELAY_DRIFT = 2 * 24000;
	/** A pod only descends "live" if someone is close enough to watch it. */
	private static final double SIMULATED_DESCENT_RANGE = 160.0;
	/** How long an unwitnessed pod takes to descend before it impacts and the base forms. */
	private static final long UNWITNESSED_DESCENT_TIME = 60L;
	/** Broadcast volume for the unwitnessed descent roar / impact boom, so distant players still get the cue. */
	private static final float DESCENT_BROADCAST_VOLUME = 16.0f;

	/** Survive this long after landfall (7 in-game days) for the advancement. */
	private static final long SEVEN_DAYS = 7L * 24000L;
	/** Every active probe core gets resupplied from orbit this often. */
	private static final long RESUPPLY_INTERVAL = 3L * 24000L;
	/**
	 * How long the Program will tolerate silence before it starts standing
	 * down: 2 in-game days with no hostile contact before heat begins to
	 * fall. This is what lets a player who stops engaging get left alone.
	 */
	private static final long HEAT_DECAY_COOLDOWN = 2L * 24000L;
	/** Once decaying, drop 1 heat per this many ticks. */
	private static final int HEAT_DECAY_INTERVAL = 200;
	/** Heat at or below this, once dormant, counts as fully neutral. */
	private static final int NEUTRAL_THRESHOLD = 2;
	/** Radius searched for gun units currently pointed at a player, for {@code sustained_fire}. */
	private static final double SUSTAINED_FIRE_RANGE = 48.0;
	/** How many Program gun units aimed at once earns {@code sustained_fire}. */
	private static final int SUSTAINED_FIRE_COUNT = 10;

	/** What one attack drone costs the Program to field. */
	private static final Map<String, Integer> ATTACK_DRONE_COST = Map.of(
			Resources.IRON, 4, Resources.GUNPOWDER, 4, Resources.REDSTONE, 2);
	/** What one medium attack drone costs the Program to field. */
	private static final Map<String, Integer> MEDIUM_ATTACK_DRONE_COST = Map.of(
			Resources.IRON, 6, Resources.GUNPOWDER, 2, Resources.REDSTONE, 3);
	/** What one sniper drone costs the Program to field. */
	private static final Map<String, Integer> SNIPER_DRONE_COST = Map.of(
			Resources.IRON, 5, Resources.COPPER, 3, Resources.REDSTONE, 4);
	/**
	 * Waves start folding in Tier 2 hardware alongside the attack drones once
	 * the Program's global threat reading crosses this line, or enough
	 * separate scans have been filed to prove the contact isn't a fluke.
	 * There is no hard ceiling on this escalation yet — that lands with the
	 * base-attackability phase, which caps Tier 3 properly.
	 */
	private static final int TIER_2_THREAT = 8;
	/** Minimum filed scans before Tier 2 can unlock, even at low threat. */
	private static final int TIER_2_MIN_SCANS = 3;
	private static final double SNIPER_DRONE_CHANCE = 0.25;
	/**
	 * Global threat at which waves start folding in Tier 3 heavies on top of
	 * the Tier 2 escalation. This is the ceiling the fleet fields before any
	 * main base falls — Tier 4+ is post-first-kill content, so in v1.0 Tier 3
	 * is effectively the hard cap. Requires Tier 2 already unlocked.
	 */
	private static final int TIER_3_THREAT = 30;
	/** What one heavy attack drone (car-sized burst-fire gun flyer) costs to field. */
	private static final Map<String, Integer> HEAVY_ATTACK_DRONE_COST = Map.of(
			Resources.IRON, 12, Resources.COPPER, 6, Resources.REDSTONE, 6, Resources.GUNPOWDER, 4);
	/** What one recon helicopter (the elytra-hunter) costs to field. */
	private static final Map<String, Integer> RECON_HELICOPTER_COST = Map.of(
			Resources.IRON, 14, Resources.COPPER, 8, Resources.REDSTONE, 6);
	/**
	 * Heavy fabrication is exactly what the first base kill degrades, so once
	 * the relay's cut a Tier 3 heavy usually just fails to come off the line —
	 * a much steeper skip than the general {@link #FABRICATION_DEGRADE_SKIP}.
	 */
	private static final float FABRICATION_DEGRADE_SKIP_HEAVY = 0.75f;
	/** The starter stockpile every pod brings down with it. */
	private static final Map<String, Integer> POD_STOCKPILE = Map.of(
			Resources.IRON, 64, Resources.COPPER, 48, Resources.REDSTONE, 32,
			Resources.COAL, 64, Resources.GUNPOWDER, 32);
	/**
	 * Modest resource windfall dropped when a probe core is actually cracked
	 * open — a stack or two of salvage, not the full payoff. The real reward
	 * (psionic unlock / tier cap) is a later task.
	 */
	private static final Map<String, Integer> BASE_DESTROYED_LOOT = Map.of(
			Resources.IRON, 32, Resources.REDSTONE, 12);
	/**
	 * The big one-time windfall for cracking the FIRST main base — severing the
	 * psionic relay spills a real haul on top of the per-core loot above.
	 */
	private static final Map<String, Integer> FIRST_KILL_BONUS_LOOT = Map.of(
			Resources.IRON, 96, Resources.COPPER, 64, Resources.REDSTONE, 32,
			Resources.DIAMOND, 4, Resources.GOLD, 24);
	/** Losing the relay reels the whole fleet — its aggression posture takes this much of a hit. */
	private static final int FIRST_KILL_THREAT_RELIEF = 25;
	/** With fabrication degraded, an escalation wave has this chance to simply fail to be built. */
	private static final float FABRICATION_DEGRADE_SKIP = 0.5f;
	/** How often the Director checks whether any of its bases is under attack. */
	private static final int BASE_DEFENSE_INTERVAL = 60;
	/** A player must be within this many blocks of a core for it to rally defenders onto them. */
	private static final double BASE_DEFENSE_RANGE = 64.0;

	/** Never more than this many outposts under construction at once. */
	private static final int MAX_ACTIVE_SITES = 2;
	/** How often the Program even considers founding a new outpost. */
	private static final int SITE_FOUNDING_CHECK_INTERVAL = 200;
	/** Minimum spacing between one outpost founding and the next. */
	private static final long MIN_SITE_FOUNDING_INTERVAL = 12000L;
	/** Same search band used for probe re-insertion, just centered on a core instead of a player. */
	private static final int SITE_MIN_DISTANCE = 60;
	private static final int SITE_MAX_DISTANCE = 140;
	/** Upfront cost to break ground on a brand new outpost. */
	private static final Map<String, Integer> SITE_FOUNDING_COST = Map.of(
			Resources.IRON, 6, Resources.WOOD, 6);
	/** How often queued progress advances, independent of whether anyone's watching. */
	private static final int BUILD_INTERVAL = 40;
	/** What one step of blueprint progress costs — deliberately cheap. */
	private static final Map<String, Integer> BUILD_STEP_COST = Map.of(Resources.IRON, 1);
	/** Visible placement catch-up rate once a site's chunk is loaded. */
	private static final int MAX_PLACEMENTS_PER_TICK = 2;

	private int globalThreat = 0;
	private int scansCompleted = 0;
	private long landingDeadline = -1L;
	private long landedAt = -1L;
	private boolean podDeployed = false;
	@Nullable
	private BlockPos probeCorePos = null;
	/** An unwitnessed pod mid-descent: its target site, or null when none is falling. */
	@Nullable
	private BlockPos pendingLandingSite = null;
	/** The tick the in-flight unwitnessed pod impacts and forms its base. */
	private long pendingLandingImpact = -1L;
	/** Every probe core site the Program has planted, live or since razed. */
	private final List<BlockPos> coreSites = new ArrayList<>();
	/**
	 * Whether the first probe core the Program ever planted has been
	 * cracked open by a player. This is the base-destroyed milestone flag; a
	 * later task reads it to gate the psionic unlock / tier cap.
	 */
	private boolean firstBaseKilled = false;
	/** Set once the first main base falls — the fleet's heavy fabrication is degraded thereafter. */
	private boolean fabricationDegraded = false;
	/** Outposts currently being built up incrementally — see {@link ConstructionSite}. */
	private final List<ConstructionSite> constructionSites = new ArrayList<>();
	/** Spacing gate so outposts don't all break ground back-to-back. */
	private long lastSiteFoundedTime = 0L;
	private long lastResupplyTime = 0L;
	/** The last time the Program had actual hostile contact with a player. */
	private long lastContactTime = 0L;
	private final Map<UUID, PlayerIntel> intel = new HashMap<>();
	private final List<PendingDispatch> dispatches = new ArrayList<>();
	/**
	 * The registry of drones currently swapped out for lightweight tokens
	 * because no player is nearby. See {@link VirtualFleet} for the
	 * materialize/dematerialize round-trip — v1 carries no off-screen
	 * behaviour, it's purely registry + persistence.
	 */
	private final VirtualFleet virtualFleet = new VirtualFleet();
	/** Fuel depots + per-cycle upkeep draw for the supply-lines first slice (see SUPPLY_LINES_SPEC.md §6). */
	private final SupplyNetwork supplyNetwork = new SupplyNetwork();
	/** Indirect-fire missions end to end (see {@link FireMissionManager} / ARTILLERY_AND_INDIRECT_FIRE.md §8). */
	private final FireMissionManager fireMissionManager = new FireMissionManager();
	/**
	 * The resource ledger. The Program spends this to field units and (in
	 * later phases) refills it by actually mining. An empty ledger means no
	 * reinforcements — starving the base is a real strategy.
	 */
	private final Map<String, Integer> resources = new HashMap<>();

	public static ProgramDirectorState get(ServerWorld world) {
		return world.getServer().getOverworld().getPersistentStateManager()
				.getOrCreate(TYPE, "program7_director");
	}

	/**
	 * Called every tick for the overworld. Drives the insertion schedule and
	 * the dispatch queue.
	 */
	public void tick(ServerWorld world) {
		if (!this.podDeployed) {
			if (this.landingDeadline < 0) {
				this.landingDeadline = world.getTime() + MIN_LANDING_DELAY
						+ world.getRandom().nextInt(LANDING_DELAY_DRIFT);
				this.markDirty();
			} else if (world.getTime() >= this.landingDeadline && !world.getPlayers().isEmpty()) {
				ServerPlayerEntity anchor = world.getPlayers()
						.get(world.getRandom().nextInt(world.getPlayers().size()));
				this.deployPod(world, anchor, LANDING_MIN_DISTANCE, LANDING_MAX_DISTANCE);
			}
		}

		if (this.pendingLandingSite != null && world.getTime() >= this.pendingLandingImpact) {
			this.resolvePendingLanding(world);
		}

		if (this.landedAt >= 0 && world.getTime() % 200 == 0
				&& world.getTime() - this.landedAt >= SEVEN_DAYS) {
			AdvancementEntry advancement = world.getServer().getAdvancementLoader()
					.get(Program7.id("seven_days"));
			if (advancement != null) {
				for (ServerPlayerEntity player : world.getPlayers()) {
					player.getAdvancementTracker().grantCriterion(advancement, "survived");
				}
			}
		}

		// Low-frequency advancement checks: nothing here is time-critical, so
		// this only needs to run a couple of times a second.
		if (world.getTime() % 40 == 0) {
			boolean neutral = this.isNeutral(world.getTime());
			for (ServerPlayerEntity p : world.getPlayers()) {
				if (neutral) {
					// The Program has stood down entirely — it no longer reads
					// this player as a threat worth escalating against.
					P7Advancements.grant(p, "incredible_performance");
				}

				int gunsOnTarget = world.getEntitiesByClass(ProgramDroneEntity.class,
						p.getBoundingBox().expand(SUSTAINED_FIRE_RANGE),
						d -> d.getTarget() == p && d.isRangedAttacker()).size();
				if (gunsOnTarget >= SUSTAINED_FIRE_COUNT) {
					P7Advancements.grant(p, "sustained_fire");
				}
			}
		}

		if (world.getTime() - this.lastResupplyTime >= RESUPPLY_INTERVAL) {
			this.pruneDeadCoreSites(world);
			if (!this.coreSites.isEmpty()) {
				for (BlockPos site : this.coreSites) {
					OrbitalResupplyEvent.runAt(world, site);
				}
				this.lastResupplyTime = world.getTime();
				this.markDirty();
			}
		}

		// Heat is a two-way dial: if the Program hasn't had hostile contact in
		// a while, it stands down rather than staying wound up forever.
		if (world.getTime() - this.lastContactTime > HEAT_DECAY_COOLDOWN
				&& world.getTime() % HEAT_DECAY_INTERVAL == 0 && this.globalThreat > 0) {
			this.globalThreat--;
			this.markDirty();
		}

		if (world.getTime() % BASE_DEFENSE_INTERVAL == 0) {
			this.tickBaseDefense(world);
		}

		if (!this.dispatches.isEmpty()) {
			Iterator<PendingDispatch> iterator = this.dispatches.iterator();
			boolean changed = false;
			while (iterator.hasNext()) {
				PendingDispatch dispatch = iterator.next();
				dispatch.ticksLeft--;
				if (dispatch.ticksLeft <= 0) {
					this.executeDispatch(world, dispatch);
					iterator.remove();
					changed = true;
				}
			}
			if (changed) {
				this.markDirty();
			}
		}

		if (this.tier2Unlocked() && world.getTime() % SITE_FOUNDING_CHECK_INTERVAL == 0
				&& this.constructionSites.size() < MAX_ACTIVE_SITES
				&& world.getTime() - this.lastSiteFoundedTime >= MIN_SITE_FOUNDING_INTERVAL
				&& !this.coreSites.isEmpty()) {
			this.tryFoundConstructionSite(world);
		}

		if (!this.constructionSites.isEmpty()) {
			this.tickConstructionSites(world);
		}

		if (this.virtualFleet.tick(world)) {
			this.markDirty();
		}

		if (this.supplyNetwork.tick(world, this)) {
			this.markDirty();
		}

		if (this.fireMissionManager.tick(world, this)) {
			this.markDirty();
		}
	}

	/**
	 * Break ground on a new outpost, anchored a fair distance from one of the
	 * Program's existing core sites. Founding costs a modest upfront payment
	 * — if the ledger can't cover it, this attempt is simply skipped and
	 * retried at the next check.
	 */
	private void tryFoundConstructionSite(ServerWorld world) {
		BlockPos core = this.coreSites.get(world.getRandom().nextInt(this.coreSites.size()));
		BlockPos site = pickLandingSite(world, core, SITE_MIN_DISTANCE, SITE_MAX_DISTANCE);
		if (site == null) {
			return; // try again next check
		}
		if (!this.tryConsume(SITE_FOUNDING_COST)) {
			return;
		}
		this.constructionSites.add(new ConstructionSite(site));
		this.lastSiteFoundedTime = world.getTime();
		this.markDirty();
		Program7.LOGGER.info("[Program 7] New outpost construction founded at {}", site.toShortString());
	}

	/**
	 * Advance every active outpost. Two clocks run independently:
	 * <ul>
	 *   <li>Time progress ({@code progress}) ticks up on a fixed cadence as
	 *   long as the ledger can pay for it, whether or not anyone is anywhere
	 *   near the site — this is what lets a player leave and come back to
	 *   find the outpost further along.</li>
	 *   <li>Visible placement ({@code placedIndex}) only advances while the
	 *   site's chunk is actually loaded, catching up toward {@code progress}
	 *   at up to {@link #MAX_PLACEMENTS_PER_TICK} blocks a tick — so a player
	 *   who wanders back in sees it briefly catch up, then settle into
	 *   building at pace.</li>
	 * </ul>
	 */
	private void tickConstructionSites(ServerWorld world) {
		List<BlockPlacement> blueprint = supplyDepotBlueprint();
		Iterator<ConstructionSite> iterator = this.constructionSites.iterator();
		boolean changed = false;
		while (iterator.hasNext()) {
			ConstructionSite site = iterator.next();

			if (site.progress < blueprint.size()) {
				if (site.buildCooldown > 0) {
					site.buildCooldown--;
				} else {
					site.buildCooldown = BUILD_INTERVAL;
					if (this.tryConsume(BUILD_STEP_COST)) {
						site.progress++;
						changed = true;
					}
				}
			}

			int chunkX = site.origin.getX() >> 4;
			int chunkZ = site.origin.getZ() >> 4;
			// getChunk(..., false) returns null instead of forcing a load, so this
			// is a pure "is it actually loaded right now" check.
			boolean chunkLoaded = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
			if (site.placedIndex < site.progress && chunkLoaded) {
				int placedThisTick = 0;
				while (site.placedIndex < site.progress && placedThisTick < MAX_PLACEMENTS_PER_TICK) {
					this.placeBlueprintBlock(world, site.origin, blueprint.get(site.placedIndex));
					site.placedIndex++;
					placedThisTick++;
					changed = true;
				}
			}

			if (site.progress >= blueprint.size() && site.placedIndex >= blueprint.size()) {
				world.playSound(null, site.origin, P7Sounds.ASSEMBLER_COMPLETE.get(), SoundCategory.BLOCKS,
						1.0f, 1.0f);
				world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
						site.origin.getX() + 0.5, site.origin.getY() + 2.0, site.origin.getZ() + 0.5,
						12, 0.6, 0.6, 0.6, 0.02);
				// Every finished outpost is a fuel depot too — plant it in the
				// blueprint's open center hatch, so supply lines reach forward.
				BlockPos hatch = site.origin.up();
				if (world.getBlockState(hatch).isReplaceable()) {
					world.setBlockState(hatch, P7Blocks.FUEL_PLANT.get().getDefaultState());
					this.supplyNetwork.registerDepot(hatch, SupplyNetwork.SUPPLY_FUEL,
							SupplyNetwork.FUEL_PLANT_RADIUS, SupplyNetwork.FUEL_PLANT_CAPACITY);
				}
				Program7.LOGGER.info("[Program 7] Outpost construction complete at {}", site.origin.toShortString());
				iterator.remove();
				changed = true;
			}
		}
		if (changed) {
			this.markDirty();
		}
	}

	/**
	 * Place a single blueprint block if — and only if — it's safe to. Never
	 * overwrites a non-replaceable (solid/player-placed) block regardless of
	 * {@link P7Config.DiggingPolicy}; under {@code PROTECT}/{@code MINIMAL}
	 * this additionally leaves a gap rather than filling in a replaceable
	 * block that isn't plain air (e.g. a player's water feature) — only
	 * {@code AGGRESSIVE} will build straight through those too. A skipped
	 * block just leaves a hole in the outpost; {@code placedIndex} still
	 * advances so construction doesn't stall on one blocked spot forever.
	 */
	private void placeBlueprintBlock(ServerWorld world, BlockPos origin, BlockPlacement placement) {
		BlockPos pos = origin.add(placement.offset());
		BlockState current = world.getBlockState(pos);
		if (!current.isReplaceable()) {
			return; // never overwrite existing solid/player-placed blocks
		}
		if (Program7.CONFIG.diggingPolicy != P7Config.DiggingPolicy.AGGRESSIVE
				&& !current.isAir() && !current.getFluidState().isEmpty()) {
			return; // PROTECT/MINIMAL: leave water features etc. alone
		}

		world.setBlockState(pos, placement.state());
		world.playSound(null, pos, P7Sounds.ASSEMBLER_WORKING.get(), SoundCategory.BLOCKS,
				0.5f, 0.9f + world.getRandom().nextFloat() * 0.2f);
		world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.03);
	}

	private static List<BlockPlacement> blueprintCache = null;

	/**
	 * The "supply depot" blueprint: a 5x5 scaffold floor with an open center
	 * hatch, four 3-tall corner posts, and a partial roof frame connecting
	 * their tops. 40 placements total, ordered bottom-up (floor, then posts,
	 * then roof) so watching {@code placedIndex} climb reads as the
	 * structure visibly rising out of the ground.
	 */
	private static List<BlockPlacement> supplyDepotBlueprint() {
		if (blueprintCache != null) {
			return blueprintCache;
		}
		List<BlockPlacement> blueprint = new ArrayList<>();
		BlockState scaffold = P7Blocks.METAL_SCAFFOLD.get().getDefaultState();

		// Layer 0: 5x5 floor, center left open as a hatch. 24 blocks.
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (x == 0 && z == 0) {
					continue;
				}
				blueprint.add(new BlockPlacement(new BlockPos(x, 0, z), scaffold));
			}
		}

		// Layers 1-3: four corner posts, 3 tall each. 12 blocks.
		int[][] corners = {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
		for (int y = 1; y <= 3; y++) {
			for (int[] corner : corners) {
				blueprint.add(new BlockPlacement(new BlockPos(corner[0], y, corner[1]), scaffold));
			}
		}

		// Layer 4: partial roof frame joining the post-tops at each edge midpoint. 4 blocks.
		int[][] edgeMidpoints = {{0, -2}, {0, 2}, {-2, 0}, {2, 0}};
		for (int[] mid : edgeMidpoints) {
			blueprint.add(new BlockPlacement(new BlockPos(mid[0], 4, mid[1]), scaffold));
		}

		blueprintCache = List.copyOf(blueprint);
		return blueprintCache;
	}

	/** One blueprint step: a relative offset from a {@link ConstructionSite}'s origin, and the state to set there. */
	private record BlockPlacement(BlockPos offset, BlockState state) {
	}

	/**
	 * Pick a landing site and bring the pod down. If a player is close enough
	 * to the site, a live {@link DropPodEntity} descends for them to watch;
	 * otherwise the landing resolves instantly and they only hear the distant
	 * impact.
	 */
	public void deployPod(ServerWorld world, ServerPlayerEntity anchor, int minDistance, int maxDistance) {
		BlockPos site = pickLandingSite(world, anchor.getBlockPos(), minDistance, maxDistance);
		if (site == null) {
			return; // try again next tick
		}
		this.podDeployed = true;
		this.markDirty();

		// Load the target chunk so block placement / entity spawn succeeds.
		world.getChunk(site);

		boolean witnessed = world.getPlayers().stream()
				.anyMatch(p -> p.getBlockPos().isWithinDistance(site, SIMULATED_DESCENT_RANGE));
		if (witnessed) {
			DropPodEntity pod = P7Entities.DROP_POD.get().create(world);
			if (pod != null) {
				pod.refreshPositionAndAngles(site.getX() + 0.5,
						Math.min(site.getY() + 140, world.getTopY() - 8),
						site.getZ() + 0.5, 0.0f, 0.0f);
				pod.setVelocity(0.0, -0.8, 0.0);
				world.spawnEntity(pod);
				Program7.LOGGER.info("[Program 7] Drop pod inbound at {}", site.toShortString());
				return;
			}
		}
		// Unwitnessed: don't blink a base into existence. Simulate the descent —
		// broadcast the sky roar now, form the base on impact a few seconds
		// later (resolved in tick()) — so even a far-off insertion reads as an
		// event, not an instant. The high broadcast volume carries the cue out
		// to distant players.
		this.pendingLandingSite = site;
		this.pendingLandingImpact = world.getTime() + UNWITNESSED_DESCENT_TIME;
		world.playSound(null, site.getX() + 0.5, site.getY() + 120, site.getZ() + 0.5,
				P7Sounds.DROP_POD_DESCENT.get(), SoundCategory.HOSTILE, DESCENT_BROADCAST_VOLUME, 0.8f);
		this.markDirty();
		Program7.LOGGER.info("[Program 7] Probe descending (unwitnessed) toward {}", site.toShortString());
	}

	/**
	 * Land an unwitnessed pod that has finished its simulated descent: the
	 * impact boom, then the base forms. Scheduled by {@link #deployPod}'s
	 * unwitnessed branch and fired from {@link #tick} once the impact tick
	 * arrives; persisted across a restart so a base never gets stranded
	 * mid-flight.
	 */
	private void resolvePendingLanding(ServerWorld world) {
		BlockPos site = this.pendingLandingSite;
		this.pendingLandingSite = null;
		this.pendingLandingImpact = -1L;
		this.markDirty();
		if (site == null) {
			return;
		}
		world.getChunk(site);
		world.playSound(null, site.getX() + 0.5, site.getY() + 0.5, site.getZ() + 0.5,
				P7Sounds.DROP_POD_IMPACT.get(), SoundCategory.HOSTILE, DESCENT_BROADCAST_VOLUME, 1.0f);
		deployProbeAt(world, site);
		Program7.LOGGER.info("[Program 7] Probe impact (unwitnessed) at {}", site.toShortString());
	}

	/**
	 * Resolve a landing at the given position: place the probe core, spawn
	 * the recon complement, register the base. Shared by the live pod's
	 * impact and unwitnessed instant landings.
	 */
	public static void deployProbeAt(ServerWorld world, BlockPos pos) {
		world.setBlockState(pos, P7Blocks.PROBE_CORE.get().getDefaultState());
		// Flatten/plate/ring the landing site before anything else goes down.
		BasePad.build(world, pos);
		placeAssembler(world, pos);
		placeLaunchCatapult(world, pos);
		placeFuelPlant(world, pos);
		// Physical stores the player can raid the base for — seeded with what
		// this pod arrived carrying (see StorageDeckBlock; drones skip it).
		StorageDeckBlock.plant(world, pos, CourierUnit.cargoToItems(POD_STOCKPILE));
		DropPodEntity.spawnLandingComplement(world, pos);

		ProgramDirectorState state = get(world);
		state.podDeployed = true;
		state.probeCorePos = pos;
		if (!state.coreSites.contains(pos)) {
			state.coreSites.add(pos);
		}
		if (state.landedAt < 0) {
			state.landedAt = world.getTime();
		}
		// Every pod arrives with a stockpile; re-insertions restock the war.
		POD_STOCKPILE.forEach(state::addResource);
		// Clear a plated stockpile apron by the core and seed the ammo runners.
		BasePad.buildStockpile(world, pos, state.getSupplyNetwork());
		state.markDirty();
	}

	/** Drop any core site whose block is no longer actually a probe core. */
	private void pruneDeadCoreSites(ServerWorld world) {
		this.coreSites.removeIf(site -> !world.getBlockState(site).isOf(P7Blocks.PROBE_CORE.get()));
	}

	/**
	 * A probe core was actually cracked open by a player (see {@link
	 * dev.rheava.program7.block.ProbeCoreBlock#onStateReplaced}). Retires the
	 * site, flags the base-destroyed milestone, and drops a modest resource
	 * windfall at the wreck — the full payoff (psionic unlock / tier cap) is
	 * a later task, this just makes the base destructible with a real
	 * consequence attached.
	 */
	public void onBaseDestroyed(ServerWorld world, BlockPos pos) {
		boolean wasFirst = !this.firstBaseKilled;
		this.coreSites.remove(pos);
		this.firstBaseKilled = true;
		this.markDirty();

		for (ItemStack stack : CourierUnit.cargoToItems(BASE_DESTROYED_LOOT)) {
			ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
		}
		Program7.LOGGER.info("[Program 7] Probe core destroyed at {}", pos.toShortString());

		if (wasFirst) {
			this.onFirstBaseKilled(world, pos);
		}

		// The invasion regroups rather than ending: with the last core site
		// gone, schedule a fresh insertion a few days out.
		if (this.coreSites.isEmpty()) {
			this.scheduleReinsertion(world);
		}
	}

	/**
	 * Every base is gone — but the Program doesn't quit, it regroups. Reset the
	 * insertion state so {@link #tick}'s normal landing path fires a fresh pod
	 * near a player after {@link #REINSERTION_DELAY}. The relay stays severed
	 * (fabrication degraded) and the first-kill payoff is one-time, so a
	 * re-insertion is renewed pressure, not a clean slate.
	 */
	private void scheduleReinsertion(ServerWorld world) {
		this.podDeployed = false;
		this.probeCorePos = null;
		this.landingDeadline = world.getTime() + REINSERTION_DELAY
				+ world.getRandom().nextInt(REINSERTION_DELAY_DRIFT);
		this.markDirty();
		Program7.LOGGER.info("[Program 7] Last base lost — re-insertion scheduled for tick {}.",
				this.landingDeadline);
	}

	/**
	 * The first main base falling is the win-condition payoff: a big resource
	 * windfall on top of the per-core loot, the fleet's aggression reeling, its
	 * heavy fabrication degraded from here on, and everyone in the world earns
	 * the "sever the relay" mark. (The full player psionic-power unlock is the
	 * later player-tech phase; this lands the fleet-side consequences now.)
	 */
	private void onFirstBaseKilled(ServerWorld world, BlockPos pos) {
		for (ItemStack stack : CourierUnit.cargoToItems(FIRST_KILL_BONUS_LOOT)) {
			ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
		}
		this.globalThreat = Math.max(0, this.globalThreat - FIRST_KILL_THREAT_RELIEF);
		this.fabricationDegraded = true;
		this.markDirty();
		for (ServerPlayerEntity player : world.getPlayers()) {
			P7Advancements.grant(player, "sever_the_relay");
		}
		Program7.LOGGER.info("[Program 7] First base destroyed — psionic relay severed, fabrication degraded.");
	}

	public boolean isFirstBaseKilled() {
		return this.firstBaseKilled;
	}

	public boolean isFabricationDegraded() {
		return this.fabricationDegraded;
	}

	public void addResource(String key, int amount) {
		this.resources.merge(key, amount, Integer::sum);
		this.markDirty();
	}

	public int getResource(String key) {
		return this.resources.getOrDefault(key, 0);
	}

	/** Atomically pay a cost, or pay nothing if any part is unaffordable. */
	public boolean tryConsume(Map<String, Integer> cost) {
		for (Map.Entry<String, Integer> entry : cost.entrySet()) {
			if (this.getResource(entry.getKey()) < entry.getValue()) {
				return false;
			}
		}
		cost.forEach((key, amount) -> this.resources.merge(key, -amount, Integer::sum));
		this.markDirty();
		return true;
	}

	public Map<String, Integer> getResources() {
		return Map.copyOf(this.resources);
	}

	/**
	 * Plant the base's assembler a short distance from the probe core. Scans
	 * the horizontal offsets at distance 3 for solid, unobstructed ground;
	 * falls back to due north of the core if the terrain doesn't cooperate.
	 */
	private static void placeAssembler(ServerWorld world, BlockPos core) {
		BlockState masterState = P7Blocks.ASSEMBLER.get().getDefaultState();
		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos base = core.offset(direction, 3);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos candidate = base.up(dy);
				if (AssemblerBlock.footprintClear(world, candidate)
						&& world.getBlockState(candidate.down()).isSolidBlock(world, candidate.down())) {
					AssemblerBlock.placeMultiblock(world, candidate, masterState);
					return;
				}
			}
		}
		AssemblerBlock.placeMultiblock(world, core.north(3), masterState);
	}

	/**
	 * Plant the launch catapult a short distance from the probe core, facing
	 * away from it so the sling fires outward. Same scan pattern as {@link
	 * #placeAssembler}, just a longer offset; falls back to due south of the
	 * core, facing south, if the terrain doesn't cooperate.
	 */
	private static void placeLaunchCatapult(ServerWorld world, BlockPos core) {
		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos base = core.offset(direction, 5);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos candidate = base.up(dy);
				if (world.getBlockState(candidate).isReplaceable()
						&& world.getBlockState(candidate.down()).isSolidBlock(world, candidate.down())) {
					world.setBlockState(candidate, P7Blocks.LAUNCH_CATAPULT.get().getDefaultState()
							.with(LaunchCatapultBlock.FACING, direction));
					return;
				}
			}
		}
		world.setBlockState(core.south(5), P7Blocks.LAUNCH_CATAPULT.get().getDefaultState()
				.with(LaunchCatapultBlock.FACING, Direction.SOUTH));
	}

	/**
	 * Plant the fuel plant a short distance from the probe core. Same scan
	 * pattern as {@link #placeAssembler}, just a longer offset (assembler sits
	 * at 3, storage deck 4, catapult 5 — no collisions); falls back to due west
	 * of the core if the terrain doesn't cooperate. The depot itself registers
	 * on the block entity's first tick (see {@code FuelPlantBlockEntity}), so
	 * nothing else needs to happen here.
	 */
	private static void placeFuelPlant(ServerWorld world, BlockPos core) {
		// Offset 7: just outside the BasePad barrier ring (radius 6) so the scan
		// lands on real terrain instead of always failing on the ring posts and
		// overwriting one, and short of the ammo stockpile apron (offset 8).
		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos base = core.offset(direction, 7);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos candidate = base.up(dy);
				if (world.getBlockState(candidate).isReplaceable()
						&& world.getBlockState(candidate.down()).isSolidBlock(world, candidate.down())) {
					world.setBlockState(candidate, P7Blocks.FUEL_PLANT.get().getDefaultState());
					return;
				}
			}
		}
		world.setBlockState(core.west(7), P7Blocks.FUEL_PLANT.get().getDefaultState());
	}

	@Nullable
	private static BlockPos pickLandingSite(ServerWorld world, BlockPos anchor, int minDistance, int maxDistance) {
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = world.getRandom().nextDouble() * Math.PI * 2.0;
			double distance = minDistance + world.getRandom().nextDouble() * (maxDistance - minDistance);
			int x = anchor.getX() + (int) (Math.cos(angle) * distance);
			int z = anchor.getZ() + (int) (Math.sin(angle) * distance);

			world.getChunk(new BlockPos(x, 64, z)); // load so the heightmap is real
			int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
			BlockPos site = new BlockPos(x, y, z);
			// Don't drop the Program into an ocean.
			if (world.getFluidState(site.down()).isEmpty()) {
				return site;
			}
		}
		return null;
	}

	public void recordScan(ServerPlayerEntity player, ScanRecord record) {
		PlayerIntel entry = this.intel.computeIfAbsent(player.getUuid(), uuid -> new PlayerIntel());
		entry.riskTier = record.riskTier();
		entry.weaponProfile = record.weaponProfile();
		entry.elytra = record.elytra();
		entry.deaths = record.deaths();
		entry.lastScanTime = player.getServerWorld().getTime();
		this.lastContactTime = entry.lastScanTime;

		this.scansCompleted++;
		// Each confirmed contact raises the Program's overall alert posture,
		// scaled by how dangerous the contact looked.
		this.globalThreat = MathHelper.clamp(this.globalThreat + 1 + record.riskTier(), 0, MAX_THREAT);

		// Proportional response: the Director answers a filed scan with
		// attack drones matched to the risk tier, after a short mustering
		// delay — but only as many as the stockpile can pay for.
		// (Tier 2/3 base responses arrive with the base itself.)
		if (this.podDeployed) {
			int affordable = 0;
			for (int i = 0; i < record.riskTier(); i++) {
				if (this.tryConsume(ATTACK_DRONE_COST)) {
					affordable++;
				}
			}
			if (affordable > 0) {
				this.dispatches.add(new PendingDispatch(player.getUuid(), affordable,
						200 + player.getRandom().nextInt(200)));
			}
		}
		this.markDirty();
	}

	/**
	 * Whether the Program has earned the right to field Tier 2 hardware.
	 *
	 * <p>This is deliberately capability/threat-driven, not calendar-driven:
	 * the Program escalates once it has had enough hostile contact — a high
	 * enough global threat reading, or enough filed scans — to justify
	 * fielding heavier hardware, never simply because a fixed number of
	 * in-game days have passed.
	 */
	private boolean tier2Unlocked() {
		return this.globalThreat >= TIER_2_THREAT || this.scansCompleted >= TIER_2_MIN_SCANS;
	}

	/**
	 * Whether the fleet has earned the right to field Tier 3 heavies: Tier 2
	 * already open and the global threat reading up past {@link #TIER_3_THREAT}.
	 * This is the top of the escalation ladder in v1.0 — nothing heavier is
	 * fielded until a main base falls and opens the post-v1.0 tiers.
	 */
	private boolean tier3Unlocked() {
		return this.tier2Unlocked() && this.globalThreat >= TIER_3_THREAT;
	}

	private void executeDispatch(ServerWorld world, PendingDispatch dispatch) {
		ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(dispatch.playerId);
		if (player == null || player.getServerWorld() != world || player.isDead()) {
			return;
		}
		this.lastContactTime = world.getTime();

		PlayerIntel intel = this.getIntel(dispatch.playerId);
		String profile = intel != null ? intel.weaponProfile : ScanRecord.PROFILE_NONE;
		boolean elytra = intel != null && intel.elytra;

		this.dispatchRushers(world, player, dispatch, profile);
		this.dispatchEscalation(world, player, profile);
		this.dispatchInterception(world, player, elytra);
		this.dispatchHeavy(world, player, elytra);
	}

	/**
	 * Field the base wave of attack drones ("rushers"), sized to counter how
	 * this player fights.
	 *
	 * <p>{@code dispatch.count} rushers were already paid for back in {@link
	 * #recordScan}, so they're spawned here unpaid — only deviations from
	 * that base count need their own ledger entries. A ranged/bow player is
	 * punished for standing off by getting one extra rusher (paid for on the
	 * spot) thrown at them, since a suicide drone closing distance is exactly
	 * what an archer struggles to answer. A melee/sword player, who shreds
	 * anything that gets within arm's reach, gets fewer rushers instead —
	 * feeding them cannon fodder is a losing trade.
	 */
	private void dispatchRushers(ServerWorld world, ServerPlayerEntity player, PendingDispatch dispatch, String profile) {
		int baseCount = dispatch.count;
		if (ScanRecord.PROFILE_MELEE.equals(profile)) {
			baseCount = Math.max(1, dispatch.count - 1);
		}
		for (int i = 0; i < baseCount; i++) {
			this.spawnEscort(world, player, P7Entities.ATTACK_DRONE.get().create(world));
		}

		if (ScanRecord.PROFILE_RANGED.equals(profile) && this.tryConsume(ATTACK_DRONE_COST)) {
			// Swarm-the-archer response: one more suicide rusher, freshly paid
			// for, to close distance on a player who wants to stay stationary.
			this.spawnEscort(world, player, P7Entities.ATTACK_DRONE.get().create(world));
		}
	}

	/**
	 * Field Tier 2 hardware once the Program has earned the right to
	 * ({@link #tier2Unlocked()}), adapted to the player's weapon profile.
	 *
	 * <p>Ranged/bow players already out-range a sniper drone, so snipers are
	 * skipped entirely for them — only a medium attack drone is added to
	 * pressure them. Melee/sword players are the opposite case: standoff
	 * harassers (a sniper drone plus a medium attack drone) keep the fight at
	 * range where their sword can't reach. Unknown/no profile keeps the
	 * original balanced behavior — a medium attack drone plus a chance-based
	 * sniper drone.
	 */
	private void dispatchEscalation(ServerWorld world, ServerPlayerEntity player, String profile) {
		if (!this.tier2Unlocked()) {
			return;
		}
		// Fabrication degraded after the first base fell: heavy escalation waves
		// often just fail to come together now.
		if (this.fabricationDegraded && world.getRandom().nextFloat() < FABRICATION_DEGRADE_SKIP) {
			return;
		}

		if (ScanRecord.PROFILE_MELEE.equals(profile)) {
			if (this.tryConsume(SNIPER_DRONE_COST)) {
				this.spawnEscort(world, player, P7Entities.SNIPER_DRONE.get().create(world));
			}
			if (this.tryConsume(MEDIUM_ATTACK_DRONE_COST)) {
				this.spawnEscort(world, player, P7Entities.MEDIUM_ATTACK_DRONE.get().create(world));
			}
			return;
		}

		if (ScanRecord.PROFILE_RANGED.equals(profile)) {
			if (this.tryConsume(MEDIUM_ATTACK_DRONE_COST)) {
				this.spawnEscort(world, player, P7Entities.MEDIUM_ATTACK_DRONE.get().create(world));
			}
			return;
		}

		// Unknown/no profile: the original balanced response.
		if (this.tryConsume(MEDIUM_ATTACK_DRONE_COST)) {
			this.spawnEscort(world, player, P7Entities.MEDIUM_ATTACK_DRONE.get().create(world));
		}
		if (world.getRandom().nextDouble() < SNIPER_DRONE_CHANCE
				&& this.tryConsume(SNIPER_DRONE_COST)) {
			this.spawnEscort(world, player, P7Entities.SNIPER_DRONE.get().create(world));
		}
	}

	/**
	 * The elytra answer (Phase 3): a gliding player who can outrun a ground
	 * escort draws a fast interceptor the moment Tier 2 is open — a medium
	 * attack drone peeled off to chase them down, on top of whatever the
	 * profile escalation already sent. Being airborne is its own threat flag.
	 */
	private void dispatchInterception(ServerWorld world, ServerPlayerEntity player, boolean elytra) {
		if (!elytra || !this.tier2Unlocked()) {
			return;
		}
		if (this.fabricationDegraded && world.getRandom().nextFloat() < FABRICATION_DEGRADE_SKIP) {
			return;
		}
		if (this.tryConsume(MEDIUM_ATTACK_DRONE_COST)) {
			this.spawnEscort(world, player, P7Entities.MEDIUM_ATTACK_DRONE.get().create(world));
		}
	}

	/**
	 * Field a Tier 3 heavy once the fleet has earned it ({@link
	 * #tier3Unlocked()}) — the top of the v1.0 escalation ladder. A gliding
	 * player draws a recon helicopter, a flyer built to hunt other flyers and
	 * pin them; everyone else meets a heavy attack drone, the car-sized
	 * burst-fire gun flyer. Both are airborne, so {@link #spawnEscort}'s aerial
	 * drop suits them (tracked/naval Tier 3 units stay base-composition units,
	 * where they can be placed on real ground).
	 */
	private void dispatchHeavy(ServerWorld world, ServerPlayerEntity player, boolean elytra) {
		if (!this.tier3Unlocked()) {
			return;
		}
		// Heavy fabrication is the first casualty of a severed relay.
		float skip = this.fabricationDegraded ? FABRICATION_DEGRADE_SKIP_HEAVY : 0.0f;
		if (skip > 0.0f && world.getRandom().nextFloat() < skip) {
			return;
		}
		if (elytra && this.tryConsume(RECON_HELICOPTER_COST)) {
			this.spawnEscort(world, player, P7Entities.RECON_HELICOPTER.get().create(world));
			return;
		}
		if (this.tryConsume(HEAVY_ATTACK_DRONE_COST)) {
			this.spawnEscort(world, player, P7Entities.HEAVY_ATTACK_DRONE.get().create(world));
		}
	}

	/**
	 * A base being mined out fights back: while a probe core reads as under
	 * attack, it rallies a fresh defender onto the nearest attacker, paid from
	 * the ledger — so starving the Program throttles its defense too. Heavier
	 * units come once the fleet has reached Tier 2.
	 */
	private void tickBaseDefense(ServerWorld world) {
		if (this.coreSites.isEmpty()) {
			return;
		}
		long now = world.getTime();
		for (BlockPos site : this.coreSites) {
			if (!(world.getBlockEntity(site) instanceof ProbeCoreBlockEntity core) || !core.isUnderAttack(now)) {
				continue;
			}
			ServerPlayerEntity attacker = null;
			double best = BASE_DEFENSE_RANGE * BASE_DEFENSE_RANGE;
			for (ServerPlayerEntity p : world.getPlayers()) {
				double d = p.squaredDistanceTo(site.getX() + 0.5, site.getY() + 0.5, site.getZ() + 0.5);
				if (d < best) {
					best = d;
					attacker = p;
				}
			}
			if (attacker == null) {
				continue;
			}
			// Its base is under fire — the fleet stays wound up rather than decaying.
			this.lastContactTime = now;
			if (this.tier2Unlocked() && world.getRandom().nextBoolean()) {
				if (this.tryConsume(MEDIUM_ATTACK_DRONE_COST)) {
					this.spawnEscort(world, attacker, P7Entities.MEDIUM_ATTACK_DRONE.get().create(world));
				}
			} else if (this.tryConsume(ATTACK_DRONE_COST)) {
				this.spawnEscort(world, attacker, P7Entities.ATTACK_DRONE.get().create(world));
			}
		}
	}

	/** Drop one Program flier in near the player, targeting them immediately. */
	private void spawnEscort(ServerWorld world, ServerPlayerEntity player, @Nullable ProgramDroneEntity drone) {
		if (drone == null) {
			return;
		}
		double angle = world.getRandom().nextDouble() * Math.PI * 2.0;
		// Snipers now hold range out to ~90 blocks (SniperDroneEntity /
		// SniperAttackGoal), so drop them in farther out than the rest of the
		// escort — otherwise they'd spawn well inside their own standoff
		// distance and have to visibly retreat before they can even fire.
		double distance = drone instanceof SniperDroneEntity
				? 60.0 + world.getRandom().nextDouble() * 30.0
				: 40.0 + world.getRandom().nextDouble() * 20.0;
		int x = (int) (player.getX() + Math.cos(angle) * distance);
		int z = (int) (player.getZ() + Math.sin(angle) * distance);
		world.getChunk(new BlockPos(x, 64, z));
		int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
		double y = Math.max(surfaceY + 12, player.getY() + 10);

		drone.refreshPositionAndAngles(x + 0.5, y, z + 0.5,
				world.getRandom().nextFloat() * 360.0f, 0.0f);
		drone.setTarget(player);
		world.spawnEntity(drone);
	}

	public int getGlobalThreat() {
		return this.globalThreat;
	}

	public void setGlobalThreat(int threat) {
		this.globalThreat = MathHelper.clamp(threat, 0, MAX_THREAT);
		this.markDirty();
	}

	/** Alias for {@link #getGlobalThreat()} — the Program's current "heat". */
	public int getHeat() {
		return this.globalThreat;
	}

	/**
	 * A coarse "escalation tier" reading for the datapad: 1 = Tier 1 probe
	 * response only, 2 = Tier 2 unlocked (medium/standoff units), 3 = the
	 * heavy tier is loose (a main base has already fallen). Deliberately a
	 * display estimate, not the hard production cap.
	 */
	public int currentTierEstimate() {
		if (this.firstBaseKilled) {
			return 3;
		}
		return this.tier2Unlocked() ? 2 : 1;
	}

	/** Whether it's been long enough since the last hostile contact that heat has started (or could start) falling. */
	public boolean isDormant(long worldTime) {
		return worldTime - this.lastContactTime > HEAT_DECAY_COOLDOWN;
	}

	/** Whether the Program has fully stood down: dormant and effectively cooled off. */
	public boolean isNeutral(long worldTime) {
		return this.globalThreat <= NEUTRAL_THRESHOLD && this.isDormant(worldTime);
	}

	/** A short label for the Program's current posture toward the player, for display purposes. */
	public String posture(long worldTime) {
		if (this.globalThreat >= 60) {
			return "HUNTING";
		}
		if (this.globalThreat >= 25) {
			return "ACTIVE";
		}
		if (this.isNeutral(worldTime)) {
			return "NEUTRAL";
		}
		if (this.isDormant(worldTime)) {
			return "DORMANT";
		}
		return "WATCHFUL";
	}

	public int getScansCompleted() {
		return this.scansCompleted;
	}

	public boolean isPodDeployed() {
		return this.podDeployed;
	}

	@Nullable
	public BlockPos getProbeCorePos() {
		return this.probeCorePos;
	}

	public PlayerIntel getIntel(UUID playerId) {
		return this.intel.get(playerId);
	}

	public VirtualFleet getVirtualFleet() {
		return this.virtualFleet;
	}

	public SupplyNetwork getSupplyNetwork() {
		return this.supplyNetwork;
	}

	public FireMissionManager getFireMissionManager() {
		return this.fireMissionManager;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		nbt.putInt("GlobalThreat", this.globalThreat);
		nbt.putInt("ScansCompleted", this.scansCompleted);
		nbt.putLong("LandingDeadline", this.landingDeadline);
		nbt.putLong("LandedAt", this.landedAt);
		nbt.putBoolean("PodDeployed", this.podDeployed);
		if (this.probeCorePos != null) {
			nbt.putIntArray("ProbeCorePos", new int[] {
					this.probeCorePos.getX(), this.probeCorePos.getY(), this.probeCorePos.getZ()});
		}
		if (this.pendingLandingSite != null) {
			nbt.putIntArray("PendingLandingSite", new int[] {
					this.pendingLandingSite.getX(), this.pendingLandingSite.getY(), this.pendingLandingSite.getZ()});
			nbt.putLong("PendingLandingImpact", this.pendingLandingImpact);
		}
		nbt.putLong("LastResupplyTime", this.lastResupplyTime);
		nbt.putLong("LastContactTime", this.lastContactTime);
		nbt.putBoolean("FirstBaseKilled", this.firstBaseKilled);
		nbt.putBoolean("FabricationDegraded", this.fabricationDegraded);

		NbtList coreSitesList = new NbtList();
		for (BlockPos site : this.coreSites) {
			NbtCompound tag = new NbtCompound();
			tag.putIntArray("Pos", new int[] {site.getX(), site.getY(), site.getZ()});
			coreSitesList.add(tag);
		}
		nbt.put("CoreSites", coreSitesList);

		nbt.putLong("LastSiteFoundedTime", this.lastSiteFoundedTime);
		NbtList constructionSitesList = new NbtList();
		for (ConstructionSite site : this.constructionSites) {
			constructionSitesList.add(site.toNbt());
		}
		nbt.put("ConstructionSites", constructionSitesList);

		NbtList intelList = new NbtList();
		this.intel.forEach((uuid, entry) -> {
			NbtCompound tag = new NbtCompound();
			tag.putUuid("Player", uuid);
			tag.putInt("RiskTier", entry.riskTier);
			tag.putString("WeaponProfile", entry.weaponProfile);
			tag.putBoolean("Elytra", entry.elytra);
			tag.putInt("Deaths", entry.deaths);
			tag.putLong("LastScanTime", entry.lastScanTime);
			intelList.add(tag);
		});
		nbt.put("Intel", intelList);

		NbtList dispatchList = new NbtList();
		for (PendingDispatch dispatch : this.dispatches) {
			NbtCompound tag = new NbtCompound();
			tag.putUuid("Player", dispatch.playerId);
			tag.putInt("Count", dispatch.count);
			tag.putInt("TicksLeft", dispatch.ticksLeft);
			dispatchList.add(tag);
		}
		nbt.put("Dispatches", dispatchList);

		NbtCompound resourceTag = new NbtCompound();
		this.resources.forEach(resourceTag::putInt);
		nbt.put("Resources", resourceTag);

		nbt.put("VirtualFleet", this.virtualFleet.toNbt(registryLookup));
		nbt.put("SupplyNetwork", this.supplyNetwork.toNbt());
		nbt.put("FireMissions", this.fireMissionManager.toNbt());
		return nbt;
	}

	public static ProgramDirectorState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		ProgramDirectorState state = new ProgramDirectorState();
		state.globalThreat = nbt.getInt("GlobalThreat");
		state.scansCompleted = nbt.getInt("ScansCompleted");
		state.landingDeadline = nbt.contains("LandingDeadline") ? nbt.getLong("LandingDeadline") : -1L;
		state.landedAt = nbt.contains("LandedAt") ? nbt.getLong("LandedAt") : -1L;
		state.podDeployed = nbt.getBoolean("PodDeployed");
		if (nbt.contains("ProbeCorePos")) {
			int[] pos = nbt.getIntArray("ProbeCorePos");
			if (pos.length == 3) {
				state.probeCorePos = new BlockPos(pos[0], pos[1], pos[2]);
			}
		}
		if (nbt.contains("PendingLandingSite")) {
			int[] pos = nbt.getIntArray("PendingLandingSite");
			if (pos.length == 3) {
				state.pendingLandingSite = new BlockPos(pos[0], pos[1], pos[2]);
				state.pendingLandingImpact = nbt.getLong("PendingLandingImpact");
			}
		}
		state.lastResupplyTime = nbt.contains("LastResupplyTime") ? nbt.getLong("LastResupplyTime") : 0L;
		state.lastContactTime = nbt.contains("LastContactTime") ? nbt.getLong("LastContactTime") : 0L;
		state.firstBaseKilled = nbt.contains("FirstBaseKilled") && nbt.getBoolean("FirstBaseKilled");
		state.fabricationDegraded = nbt.contains("FabricationDegraded") && nbt.getBoolean("FabricationDegraded");

		NbtList coreSitesList = nbt.getList("CoreSites", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < coreSitesList.size(); i++) {
			int[] pos = coreSitesList.getCompound(i).getIntArray("Pos");
			if (pos.length == 3) {
				state.coreSites.add(new BlockPos(pos[0], pos[1], pos[2]));
			}
		}

		state.lastSiteFoundedTime = nbt.contains("LastSiteFoundedTime") ? nbt.getLong("LastSiteFoundedTime") : 0L;
		NbtList constructionSitesList = nbt.getList("ConstructionSites", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < constructionSitesList.size(); i++) {
			ConstructionSite site = ConstructionSite.fromNbt(constructionSitesList.getCompound(i));
			if (site != null) {
				state.constructionSites.add(site);
			}
		}

		NbtList intelList = nbt.getList("Intel", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < intelList.size(); i++) {
			NbtCompound tag = intelList.getCompound(i);
			PlayerIntel entry = new PlayerIntel();
			entry.riskTier = tag.getInt("RiskTier");
			entry.weaponProfile = tag.getString("WeaponProfile");
			entry.elytra = tag.getBoolean("Elytra");
			entry.deaths = tag.getInt("Deaths");
			entry.lastScanTime = tag.getLong("LastScanTime");
			state.intel.put(tag.getUuid("Player"), entry);
		}

		NbtList dispatchList = nbt.getList("Dispatches", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < dispatchList.size(); i++) {
			NbtCompound tag = dispatchList.getCompound(i);
			state.dispatches.add(new PendingDispatch(
					tag.getUuid("Player"), tag.getInt("Count"), tag.getInt("TicksLeft")));
		}

		NbtCompound resourceTag = nbt.getCompound("Resources");
		for (String key : resourceTag.getKeys()) {
			state.resources.put(key, resourceTag.getInt(key));
		}

		if (nbt.contains("VirtualFleet")) {
			state.virtualFleet.readNbt(nbt.getCompound("VirtualFleet"), registryLookup);
		}
		if (nbt.contains("SupplyNetwork")) {
			state.supplyNetwork.readNbt(nbt.getCompound("SupplyNetwork"));
		}
		if (nbt.contains("FireMissions")) {
			state.fireMissionManager.readNbt(nbt.getCompound("FireMissions"));
		}
		return state;
	}

	public static class PlayerIntel {
		public int riskTier = 1;
		public String weaponProfile = ScanRecord.PROFILE_NONE;
		public boolean elytra = false;
		public int deaths = 0;
		public long lastScanTime = 0L;
	}

	private static class PendingDispatch {
		final UUID playerId;
		final int count;
		int ticksLeft;

		PendingDispatch(UUID playerId, int count, int ticksLeft) {
			this.playerId = playerId;
			this.count = count;
			this.ticksLeft = ticksLeft;
		}
	}

	/**
	 * A single outpost under incremental construction. {@code progress} is
	 * how far the blueprint has advanced on the Director's own clock —
	 * ticking up whether or not the chunk is loaded — while {@code
	 * placedIndex} is how many of those steps have actually been placed as
	 * real blocks in the world; it can only catch up to {@code progress}
	 * while the chunk is loaded. {@code buildCooldown} is the countdown to
	 * the next progress step.
	 */
	static final class ConstructionSite {
		BlockPos origin;
		int progress;
		int placedIndex;
		int buildCooldown;

		ConstructionSite(BlockPos origin) {
			this.origin = origin;
			this.progress = 0;
			this.placedIndex = 0;
			this.buildCooldown = 0;
		}

		NbtCompound toNbt() {
			NbtCompound tag = new NbtCompound();
			tag.putIntArray("Origin", new int[] {this.origin.getX(), this.origin.getY(), this.origin.getZ()});
			tag.putInt("Progress", this.progress);
			tag.putInt("PlacedIndex", this.placedIndex);
			tag.putInt("BuildCooldown", this.buildCooldown);
			return tag;
		}

		@Nullable
		static ConstructionSite fromNbt(NbtCompound tag) {
			int[] pos = tag.getIntArray("Origin");
			if (pos.length != 3) {
				return null;
			}
			ConstructionSite site = new ConstructionSite(new BlockPos(pos[0], pos[1], pos[2]));
			site.progress = tag.getInt("Progress");
			site.placedIndex = tag.getInt("PlacedIndex");
			site.buildCooldown = tag.getInt("BuildCooldown");
			return site;
		}
	}
}
