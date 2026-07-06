package dev.rheava.program7.director;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AttackDroneEntity;
import dev.rheava.program7.entity.DropPodEntity;
import dev.rheava.program7.registry.P7Blocks;
import dev.rheava.program7.registry.P7Entities;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
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
	/** A pod only descends "live" if someone is close enough to watch it. */
	private static final double SIMULATED_DESCENT_RANGE = 160.0;

	private int globalThreat = 0;
	private int scansCompleted = 0;
	private long landingDeadline = -1L;
	private boolean podDeployed = false;
	@Nullable
	private BlockPos probeCorePos = null;
	private final Map<UUID, PlayerIntel> intel = new HashMap<>();
	private final List<PendingDispatch> dispatches = new ArrayList<>();

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
				this.deployPod(world, anchor, 300, 600);
			}
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
			DropPodEntity pod = P7Entities.DROP_POD.create(world);
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
		deployProbeAt(world, site);
		Program7.LOGGER.info("[Program 7] Probe inserted at {} (unwitnessed)", site.toShortString());
	}

	/**
	 * Resolve a landing at the given position: place the probe core, spawn
	 * the recon complement, register the base. Shared by the live pod's
	 * impact and unwitnessed instant landings.
	 */
	public static void deployProbeAt(ServerWorld world, BlockPos pos) {
		world.setBlockState(pos, P7Blocks.PROBE_CORE.getDefaultState());
		DropPodEntity.spawnLandingComplement(world, pos);

		ProgramDirectorState state = get(world);
		state.podDeployed = true;
		state.probeCorePos = pos;
		state.markDirty();
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

		this.scansCompleted++;
		// Each confirmed contact raises the Program's overall alert posture,
		// scaled by how dangerous the contact looked.
		this.globalThreat = MathHelper.clamp(this.globalThreat + 1 + record.riskTier(), 0, MAX_THREAT);

		// Proportional response: the Director answers a filed scan with
		// attack drones matched to the risk tier, after a short mustering
		// delay. (Tier 2/3 base responses arrive with the base itself.)
		if (this.podDeployed) {
			this.dispatches.add(new PendingDispatch(player.getUuid(), record.riskTier(),
					200 + player.getRandom().nextInt(200)));
		}
		this.markDirty();
	}

	private void executeDispatch(ServerWorld world, PendingDispatch dispatch) {
		ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(dispatch.playerId);
		if (player == null || player.getServerWorld() != world || player.isDead()) {
			return;
		}
		for (int i = 0; i < dispatch.count; i++) {
			double angle = world.getRandom().nextDouble() * Math.PI * 2.0;
			double distance = 40.0 + world.getRandom().nextDouble() * 20.0;
			int x = (int) (player.getX() + Math.cos(angle) * distance);
			int z = (int) (player.getZ() + Math.sin(angle) * distance);
			world.getChunk(new BlockPos(x, 64, z));
			int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
			double y = Math.max(surfaceY + 12, player.getY() + 10);

			AttackDroneEntity drone = P7Entities.ATTACK_DRONE.create(world);
			if (drone != null) {
				drone.refreshPositionAndAngles(x + 0.5, y, z + 0.5,
						world.getRandom().nextFloat() * 360.0f, 0.0f);
				drone.setTarget(player);
				world.spawnEntity(drone);
			}
		}
	}

	public int getGlobalThreat() {
		return this.globalThreat;
	}

	public void setGlobalThreat(int threat) {
		this.globalThreat = MathHelper.clamp(threat, 0, MAX_THREAT);
		this.markDirty();
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

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		nbt.putInt("GlobalThreat", this.globalThreat);
		nbt.putInt("ScansCompleted", this.scansCompleted);
		nbt.putLong("LandingDeadline", this.landingDeadline);
		nbt.putBoolean("PodDeployed", this.podDeployed);
		if (this.probeCorePos != null) {
			nbt.putIntArray("ProbeCorePos", new int[] {
					this.probeCorePos.getX(), this.probeCorePos.getY(), this.probeCorePos.getZ()});
		}

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
		return nbt;
	}

	public static ProgramDirectorState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		ProgramDirectorState state = new ProgramDirectorState();
		state.globalThreat = nbt.getInt("GlobalThreat");
		state.scansCompleted = nbt.getInt("ScansCompleted");
		state.landingDeadline = nbt.contains("LandingDeadline") ? nbt.getLong("LandingDeadline") : -1L;
		state.podDeployed = nbt.getBoolean("PodDeployed");
		if (nbt.contains("ProbeCorePos")) {
			int[] pos = nbt.getIntArray("ProbeCorePos");
			if (pos.length == 3) {
				state.probeCorePos = new BlockPos(pos[0], pos[1], pos[2]);
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
}
