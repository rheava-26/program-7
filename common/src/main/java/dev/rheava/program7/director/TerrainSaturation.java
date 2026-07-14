package dev.rheava.program7.director;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dev.rheava.program7.Program7;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;

/**
 * The artillery doc's §5a bombardment-saturation accumulator: "a lightweight
 * grid, decaying slowly when fire stops... a cell's saturation climbs,
 * terrain there degrades in stages: intact → cratered surface → rubble →
 * pulverized." A sibling organ of {@link FireMissionManager}, owned by
 * {@link ProgramDirectorState}, following the exact same shape as that
 * manager's dwell heatmap (a coarse, decaying, NBT-persisted grid) — this is
 * that idea pointed at the ground instead of at players.
 *
 * <p><b>Per-round restraint, cumulative erosion</b> (doc §5a): a single
 * impact never demolishes anything on its own — {@link #recordImpact} steps
 * at most <em>one</em> stage per block per call, so only genuinely sustained
 * fire on the same spot walks a block all the way from intact stone down to
 * a pulverized crater. Reuses {@link
 * dev.rheava.program7.entity.ai.HitscanImpact}'s erosion guards
 * (mobGriefing-gated, unbreakable/{@code program7}-namespace blocks never
 * touched) with a staged block-downgrade instead of straight chip-to-break.
 *
 * <p><b>Known scope limit (v1):</b> {@link #DEGRADE_NEXT} only covers the
 * common overworld stone/soil families — an unlisted block instead falls
 * back to a coarse "pulverize straight to air past a high threshold" rule
 * (see {@link #tryDegrade}) rather than a full staged chain. A fuller
 * per-block table is a later content pass, not new architecture.
 */
public final class TerrainSaturation {
	/** Column cell size — coarse on purpose, matches the doc's "lightweight grid." */
	private static final int CELL_SHIFT = 2; // 4x4-block columns
	/** Saturation added per impact, before the shell's own weight multiplier. */
	private static final float BASE_WEIGHT = 1.0f;
	private static final float MAX_SATURATION = 400.0f;
	/** Decay rate, saturation per tick — a cell left alone sheds this every tick since its last touch (lazy, computed on read, not swept). */
	private static final float DECAY_PER_TICK = 0.01f;
	/** Cells decayed below this are pruned outright. */
	private static final float PRUNE_THRESHOLD = 0.5f;
	/** How often {@link #tick} sweeps for prunable cells — this is upkeep only, not the decay itself (decay is lazy/on-read). */
	private static final int PRUNE_INTERVAL = 1200;
	/** Hard cap on the grid so a long, spread-out siege can't grow it unbounded. */
	private static final int MAX_CELLS = 512;

	/** Stage threshold = {@code STAGE_THRESHOLD_BASE + blastResistance * STAGE_THRESHOLD_BLAST_SCALE}, mirroring {@code HitscanImpact}'s shape. */
	private static final float STAGE_THRESHOLD_BASE = 4.0f;
	private static final float STAGE_THRESHOLD_BLAST_SCALE = 2.0f;
	/** How many threshold-multiples an unlisted block needs before the coarse fallback just removes it. */
	private static final float FALLBACK_STAGE_COUNT = 4.0f;
	/** Radius (blocks, horizontal) of the footprint degraded per impact — a shell roughs up its landing spot, not just the one block it clips. */
	private static final int DEGRADE_RADIUS_XZ = 1;

	/** Staged downgrade chain: a block maps to the next-weaker one it degrades into. Absent from this map = the coarse fallback (see class doc). */
	private static final Map<Block, Block> DEGRADE_NEXT = buildDegradeTable();

	private static Map<Block, Block> buildDegradeTable() {
		Map<Block, Block> table = new HashMap<>();
		// Stone family -> rubble -> gravel -> sand -> gone.
		table.put(Blocks.STONE, Blocks.COBBLESTONE);
		table.put(Blocks.ANDESITE, Blocks.COBBLESTONE);
		table.put(Blocks.DIORITE, Blocks.COBBLESTONE);
		table.put(Blocks.GRANITE, Blocks.COBBLESTONE);
		table.put(Blocks.TUFF, Blocks.COBBLESTONE);
		table.put(Blocks.STONE_BRICKS, Blocks.COBBLESTONE);
		table.put(Blocks.COBBLESTONE, Blocks.GRAVEL);
		table.put(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
		table.put(Blocks.COBBLED_DEEPSLATE, Blocks.GRAVEL);
		table.put(Blocks.DEEPSLATE_BRICKS, Blocks.COBBLED_DEEPSLATE);
		table.put(Blocks.GRAVEL, Blocks.SAND);
		table.put(Blocks.SAND, Blocks.AIR);
		table.put(Blocks.SANDSTONE, Blocks.SAND);
		table.put(Blocks.RED_SANDSTONE, Blocks.RED_SAND);
		table.put(Blocks.RED_SAND, Blocks.AIR);
		// Soil family -> churned -> dust -> gone.
		table.put(Blocks.GRASS_BLOCK, Blocks.DIRT);
		table.put(Blocks.PODZOL, Blocks.DIRT);
		table.put(Blocks.MYCELIUM, Blocks.DIRT);
		table.put(Blocks.DIRT, Blocks.COARSE_DIRT);
		table.put(Blocks.COARSE_DIRT, Blocks.SAND);
		// Thin soft cover: one knock and it's gone.
		table.put(Blocks.SNOW_BLOCK, Blocks.AIR);
		return table;
	}

	/** Column saturation cells, keyed by a packed {@code (x >> CELL_SHIFT, z >> CELL_SHIFT)} coordinate. */
	private final Map<Long, Cell> cells = new HashMap<>();
	private int pruneCooldown = PRUNE_INTERVAL;

	/**
	 * Called from {@link
	 * dev.rheava.program7.entity.AbstractShellEntity#onImpact} on every
	 * indirect-fire munition's detonation: folds {@code weight} (a heavier
	 * munition like the howitzer/missile counts for more, see {@code
	 * AbstractShellEntity#saturationWeight}) into the impact's cell, then
	 * attempts a one-stage-per-block degrade pass over a small footprint
	 * around the impact.
	 */
	public void recordImpact(ServerWorld world, BlockPos impactPos, float weight) {
		long key = cellKey(impactPos.getX(), impactPos.getZ());
		Cell cell = this.cells.computeIfAbsent(key, k -> new Cell());
		long now = world.getTime();
		float decayed = decayedValue(cell, now);
		cell.saturation = Math.min(MAX_SATURATION, decayed + BASE_WEIGHT * weight);
		cell.lastTouchTick = now;

		for (int dx = -DEGRADE_RADIUS_XZ; dx <= DEGRADE_RADIUS_XZ; dx++) {
			for (int dz = -DEGRADE_RADIUS_XZ; dz <= DEGRADE_RADIUS_XZ; dz++) {
				for (int dy = 0; dy >= -1; dy--) {
					tryDegrade(world, impactPos.add(dx, dy, dz), cell.saturation);
				}
			}
		}
	}

	/** The decayed reading of {@code cell} at tick {@code now}, without mutating it. */
	private static float decayedValue(Cell cell, long now) {
		float elapsed = Math.max(0L, now - cell.lastTouchTick);
		return Math.max(0.0f, cell.saturation - DECAY_PER_TICK * elapsed);
	}

	/**
	 * Steps {@code pos} down at most one stage in {@link #DEGRADE_NEXT} if
	 * {@code cellSaturation} has crossed that block's own blast-resistance-
	 * scaled threshold. A block not in the table falls back to a coarse
	 * "pulverize straight to air past a high threshold" rule instead of a
	 * full staged chain (see class doc's scope-limit note).
	 */
	private static void tryDegrade(ServerWorld world, BlockPos pos, float cellSaturation) {
		if (!Program7.CONFIG.terrainDestruction) {
			return;
		}
		if (!world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
			return;
		}
		BlockState state = world.getBlockState(pos);
		if (state.isAir()) {
			return;
		}
		float hardness = state.getHardness(world, pos);
		if (hardness < 0.0f) {
			// Unbreakable (bedrock and the like) — never touched.
			return;
		}
		Identifier blockId = Registries.BLOCK.getId(state.getBlock());
		if (Program7.MOD_ID.equals(blockId.getNamespace())) {
			// Never grief the Program's own structures.
			return;
		}

		float blastResistance = state.getBlock().getBlastResistance();
		float threshold = STAGE_THRESHOLD_BASE + blastResistance * STAGE_THRESHOLD_BLAST_SCALE;
		if (cellSaturation < threshold) {
			return;
		}

		Block next = DEGRADE_NEXT.get(state.getBlock());
		if (next != null) {
			world.setBlockState(pos, next.getDefaultState());
			return;
		}
		if (cellSaturation >= threshold * FALLBACK_STAGE_COUNT) {
			world.removeBlock(pos, false);
		}
	}

	private static long cellKey(int x, int z) {
		int cx = x >> CELL_SHIFT;
		int cz = z >> CELL_SHIFT;
		return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
	}

	/**
	 * Coarse upkeep sweep, called from {@link ProgramDirectorState#tick} on a
	 * long cadence: prunes cells that have decayed under {@link
	 * #PRUNE_THRESHOLD} and, past the hard cap, drops the coldest ones. Decay
	 * itself is lazy (computed on read in {@link #decayedValue}) — this is
	 * only bookkeeping so the map doesn't grow forever.
	 */
	public boolean tick(ServerWorld world) {
		this.pruneCooldown--;
		if (this.pruneCooldown > 0) {
			return false;
		}
		this.pruneCooldown = PRUNE_INTERVAL;
		if (this.cells.isEmpty()) {
			return false;
		}
		long now = world.getTime();
		boolean changed = false;
		Iterator<Map.Entry<Long, Cell>> iterator = this.cells.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Long, Cell> entry = iterator.next();
			float decayed = decayedValue(entry.getValue(), now);
			if (decayed < PRUNE_THRESHOLD) {
				iterator.remove();
				changed = true;
			} else {
				// Bake the decayed value back in so it doesn't have to be
				// recomputed from an ever-growing tick delta.
				entry.getValue().saturation = decayed;
				entry.getValue().lastTouchTick = now;
			}
		}
		if (this.cells.size() > MAX_CELLS) {
			this.pruneColdestCells();
			changed = true;
		}
		return changed;
	}

	private void pruneColdestCells() {
		while (this.cells.size() > MAX_CELLS) {
			Long coldestKey = null;
			float coldest = Float.MAX_VALUE;
			for (Map.Entry<Long, Cell> entry : this.cells.entrySet()) {
				if (entry.getValue().saturation < coldest) {
					coldest = entry.getValue().saturation;
					coldestKey = entry.getKey();
				}
			}
			if (coldestKey == null) {
				return;
			}
			this.cells.remove(coldestKey);
		}
	}

	// ---- Persistence ---------------------------------------------------------------------------

	public NbtCompound toNbt() {
		NbtCompound tag = new NbtCompound();
		NbtList cellList = new NbtList();
		for (Map.Entry<Long, Cell> entry : this.cells.entrySet()) {
			NbtCompound cellTag = new NbtCompound();
			cellTag.putLong("Key", entry.getKey());
			cellTag.putFloat("Saturation", entry.getValue().saturation);
			cellTag.putLong("LastTouch", entry.getValue().lastTouchTick);
			cellList.add(cellTag);
		}
		tag.put("Cells", cellList);
		return tag;
	}

	public void readNbt(NbtCompound nbt) {
		this.cells.clear();
		NbtList cellList = nbt.getList("Cells", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < cellList.size(); i++) {
			NbtCompound cellTag = cellList.getCompound(i);
			Cell cell = new Cell();
			cell.saturation = cellTag.getFloat("Saturation");
			cell.lastTouchTick = cellTag.getLong("LastTouch");
			this.cells.put(cellTag.getLong("Key"), cell);
		}
	}

	private static final class Cell {
		float saturation;
		long lastTouchTick;
	}
}
