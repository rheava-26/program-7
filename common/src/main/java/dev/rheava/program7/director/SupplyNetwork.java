package dev.rheava.program7.director;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dev.rheava.program7.registry.P7Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The Director organ for supply-line depots — see {@code SUPPLY_LINES_SPEC.md}
 * §6. Sibling of {@link VirtualFleet}: a plain class, owned field on {@link
 * ProgramDirectorState}, ticked from {@code ProgramDirectorState.tick}, with
 * its own NBT round-trip.
 *
 * <p>Per-unit upkeep state (grace, the degraded flag) lives on the units
 * themselves (see {@link dev.rheava.program7.entity.ProgramDroneEntity}) —
 * this manager only owns the depot list: where the stock is, how much of it
 * there is, and how far it reaches.
 */
public final class SupplyNetwork {
	/** Coarse upkeep cycle: 10s. Same order as the assembler's evaluate interval. */
	public static final int CYCLE_TICKS = 200;
	/** The only supply type this slice knows about — depot stock, never a ledger key. */
	public static final String SUPPLY_FUEL = "fuel";

	/** Fuel plant provision radius (sphere, center = block pos). */
	public static final int FUEL_PLANT_RADIUS = 64;
	/** Fuel plant stock cap. */
	public static final int FUEL_PLANT_CAPACITY = 240;
	/** FUEL produced per coal consumed from the ledger, at most once per cycle per depot. */
	public static final int FUEL_PER_COAL = 8;

	private final List<Depot> depots = new ArrayList<>();

	/**
	 * Coarse cycle: runs only on ticks aligned to {@link #CYCLE_TICKS}. For
	 * every depot: (1) if its chunk is loaded and the block underneath it is
	 * no longer a fuel plant, drop the depot (the {@code pruneDeadCoreSites}
	 * pattern — pistons, explosions, {@code /setblock} can't leave ghosts);
	 * (2) otherwise, if there's room for a full coal's worth of fuel and the
	 * ledger can pay one coal, refill. Refill runs whether or not the chunk
	 * is loaded — a depot that only refills while watched would punish
	 * players for exploring.
	 *
	 * @return true if anything about the depot list actually changed, so the
	 *         caller knows whether to mark its persistent state dirty.
	 */
	public boolean tick(ServerWorld world, ProgramDirectorState director) {
		if (world.getTime() % CYCLE_TICKS != 0 || this.depots.isEmpty()) {
			return false;
		}

		boolean changed = false;
		Iterator<Depot> iterator = this.depots.iterator();
		while (iterator.hasNext()) {
			Depot depot = iterator.next();

			int chunkX = depot.pos.getX() >> 4;
			int chunkZ = depot.pos.getZ() >> 4;
			boolean chunkLoaded = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
			if (chunkLoaded && !isDepotBlockPresent(world, depot)) {
				iterator.remove();
				changed = true;
				continue;
			}

			if (depot.stock <= depot.capacity - FUEL_PER_COAL
					&& director.tryConsume(Map.of(Resources.COAL, 1))) {
				depot.stock += FUEL_PER_COAL;
				changed = true;
			}
		}
		return changed;
	}

	/** Whether the block actually backing this depot is still standing. Only fuel plants exist so far. */
	private static boolean isDepotBlockPresent(ServerWorld world, Depot depot) {
		if (SUPPLY_FUEL.equals(depot.supplyType)) {
			return world.getBlockState(depot.pos).isOf(P7Blocks.FUEL_PLANT.get());
		}
		return true;
	}

	/**
	 * Register a new depot at {@code pos}. Idempotent — re-registering an
	 * existing position is a no-op and keeps its current stock, so a
	 * block-entity's "register on first tick" call can never reset a depot
	 * that survived a chunk unload/reload.
	 */
	public void registerDepot(BlockPos pos, String supplyType, int radius, int capacity) {
		if (this.depotAt(pos) != null) {
			return;
		}
		Depot depot = new Depot();
		depot.pos = pos.toImmutable();
		depot.supplyType = supplyType;
		depot.radius = radius;
		depot.capacity = capacity;
		depot.stock = capacity;
		this.depots.add(depot);
	}

	public void removeDepot(BlockPos pos) {
		this.depots.removeIf(depot -> depot.pos.equals(pos));
	}

	/**
	 * Find the nearest in-range depot of {@code supplyType} whose stock
	 * covers the full {@code amount} (no partial draws — a dry depot reads
	 * as dry), debit it, and report success.
	 */
	public boolean drawSupply(BlockPos pos, String supplyType, int amount) {
		Depot nearest = null;
		double nearestDistanceSq = Double.MAX_VALUE;
		for (Depot depot : this.depots) {
			if (!depot.supplyType.equals(supplyType) || depot.stock < amount) {
				continue;
			}
			double distanceSq = depot.pos.getSquaredDistance(pos);
			if (distanceSq > (double) depot.radius * depot.radius) {
				continue;
			}
			if (distanceSq < nearestDistanceSq) {
				nearestDistanceSq = distanceSq;
				nearest = depot;
			}
		}
		if (nearest == null) {
			return false;
		}
		nearest.stock -= amount;
		return true;
	}

	/** Block-entity queries (tells, debug): the depot registered exactly at this position, if any. */
	@Nullable
	public Depot depotAt(BlockPos pos) {
		for (Depot depot : this.depots) {
			if (depot.pos.equals(pos)) {
				return depot;
			}
		}
		return null;
	}

	/** Read-only snapshot of the current depots, for display purposes (datapad, later). */
	public List<Depot> getDepots() {
		return List.copyOf(this.depots);
	}

	public NbtCompound toNbt() {
		NbtCompound tag = new NbtCompound();
		NbtList list = new NbtList();
		for (Depot depot : this.depots) {
			NbtCompound depotTag = new NbtCompound();
			depotTag.putIntArray("Pos", new int[] {depot.pos.getX(), depot.pos.getY(), depot.pos.getZ()});
			depotTag.putString("SupplyType", depot.supplyType);
			depotTag.putInt("Radius", depot.radius);
			depotTag.putInt("Capacity", depot.capacity);
			depotTag.putInt("Stock", depot.stock);
			list.add(depotTag);
		}
		tag.put("Depots", list);
		return tag;
	}

	public void readNbt(NbtCompound nbt) {
		this.depots.clear();
		if (!nbt.contains("Depots")) {
			return;
		}
		NbtList list = nbt.getList("Depots", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound depotTag = list.getCompound(i);
			int[] pos = depotTag.getIntArray("Pos");
			if (pos.length != 3) {
				continue;
			}
			Depot depot = new Depot();
			depot.pos = new BlockPos(pos[0], pos[1], pos[2]);
			depot.supplyType = depotTag.getString("SupplyType");
			depot.radius = depotTag.getInt("Radius");
			depot.capacity = depotTag.getInt("Capacity");
			depot.stock = depotTag.getInt("Stock");
			this.depots.add(depot);
		}
	}

	/**
	 * A single supply depot. Plain mutable fields, public: this is the
	 * single source of truth for {@code stock}, read directly by the owning
	 * block entity's tell logic, the owning block's break-salvage spill, and
	 * (later) the datapad supply panel.
	 */
	public static final class Depot {
		public BlockPos pos;
		public String supplyType;
		public int radius;
		public int capacity;
		public int stock;
	}
}
