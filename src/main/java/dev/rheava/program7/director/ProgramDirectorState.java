package dev.rheava.program7.director;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.PersistentState;

/**
 * The Program Director — the single brain of the mod, persisted per world.
 *
 * <p>Everything the Program knows lives here: how threatening this world has
 * proven so far (global threat), and a per-player intel file built from
 * surveyor scans. Later phases read this state to decide drone production
 * splits, lockdown mode, and which counter-loadouts to field against each
 * player's fighting style.
 */
public class ProgramDirectorState extends PersistentState {
	private static final PersistentState.Type<ProgramDirectorState> TYPE = new PersistentState.Type<>(
			ProgramDirectorState::new, ProgramDirectorState::fromNbt, null);

	public static final int MAX_THREAT = 100;

	private int globalThreat = 0;
	private int scansCompleted = 0;
	private final Map<UUID, PlayerIntel> intel = new HashMap<>();

	public static ProgramDirectorState get(ServerWorld world) {
		return world.getServer().getOverworld().getPersistentStateManager()
				.getOrCreate(TYPE, "program7_director");
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
		this.markDirty();
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

	public PlayerIntel getIntel(UUID playerId) {
		return this.intel.get(playerId);
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		nbt.putInt("GlobalThreat", this.globalThreat);
		nbt.putInt("ScansCompleted", this.scansCompleted);

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
		return nbt;
	}

	public static ProgramDirectorState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		ProgramDirectorState state = new ProgramDirectorState();
		state.globalThreat = nbt.getInt("GlobalThreat");
		state.scansCompleted = nbt.getInt("ScansCompleted");

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
		return state;
	}

	public static class PlayerIntel {
		public int riskTier = 1;
		public String weaponProfile = ScanRecord.PROFILE_NONE;
		public boolean elytra = false;
		public int deaths = 0;
		public long lastScanTime = 0L;
	}
}
