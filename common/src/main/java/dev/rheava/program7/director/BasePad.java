package dev.rheava.program7.director;

import dev.rheava.program7.Program7;
import dev.rheava.program7.block.AmmoBoxBlock;
import dev.rheava.program7.config.P7Config;
import dev.rheava.program7.entity.LogisticsDroneEntity;
import dev.rheava.program7.registry.P7Blocks;
import dev.rheava.program7.registry.P7Entities;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameRules;

/**
 * Builds the mechanical furniture a fresh Program base stamps into the
 * ground around it: a plated, barrier-ringed landing pad at the impact site
 * (see {@link #build}), and a smaller plated stockpile apron seeded with a
 * starter cluster of ammo boxes plus a couple of dedicated logistics drones
 * to run them (see {@link #buildStockpile}).
 *
 * <p>Both respect the {@code mobGriefing} gamerule (no clearing at all when
 * it's off) and {@link P7Config.DiggingPolicy} (PROTECT/MINIMAL only ever
 * clear recognizably natural terrain — dirt, stone, logs, leaves, sand, ice,
 * snow, flowers, saplings — never anything that reads as player-built;
 * AGGRESSIVE clears through anything). A column that isn't clearable under
 * the current policy is just skipped rather than forced, so a pad landing
 * next to an existing player build leaves it alone instead of paving over it.
 */
public final class BasePad {
	/** Chebyshev radius (in blocks) of the landing pad's plated floor and barrier ring. */
	public static final int PAD_RADIUS = 6;
	/** Chebyshev radius of the smaller stockpile apron. */
	public static final int STOCKPILE_RADIUS = 3;
	/** How many ammo-runner logistics drones a freshly built stockpile starts with. */
	private static final int STARTER_AMMO_RUNNERS = 2;
	/** How many ammo box stacks (each 1-4 "crates") seed a fresh stockpile. */
	private static final int STARTER_AMMO_STACKS = 3;

	private BasePad() {
	}

	/**
	 * Flatten a small area around {@code center} (the probe core's own
	 * position) and pave it: {@link P7Blocks#PLATING} replaces the ground
	 * one layer *under* the core's own level (so the core ends up standing
	 * on the new floor exactly like everything else around it, never
	 * overwritten itself — see {@link #canBuildAt}), three blocks of
	 * headroom are cleared above the floor so the pad reads as a walkable
	 * plaza, and the rim is ringed with {@link P7Blocks#BARRIER_POST} at the
	 * core's own ground level. Idempotent-ish: safe to call more than once at
	 * the same site, though in practice it only ever runs once, from the
	 * landing resolution.
	 */
	public static void build(ServerWorld world, BlockPos center) {
		boolean mobGriefing = world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
		P7Config.DiggingPolicy policy = Program7.CONFIG.diggingPolicy;
		BlockState plating = P7Blocks.PLATING.get().getDefaultState();
		BlockState barrier = P7Blocks.BARRIER_POST.get().getDefaultState();

		for (int x = -PAD_RADIUS; x <= PAD_RADIUS; x++) {
			for (int z = -PAD_RADIUS; z <= PAD_RADIUS; z++) {
				BlockPos groundPos = center.add(x, 0, z);
				clearHeadroom(world, groundPos, mobGriefing, policy);

				BlockPos floorPos = groundPos.down();
				if (canBuildAt(world, floorPos, mobGriefing, policy)) {
					world.setBlockState(floorPos, plating);
				}

				boolean onRing = Math.max(Math.abs(x), Math.abs(z)) == PAD_RADIUS;
				if (onRing && world.getBlockState(groundPos).isReplaceable()) {
					world.setBlockState(groundPos, barrier);
				}
			}
		}
	}

	/**
	 * A smaller plated apron a short distance from the core — the base's
	 * ammo/crate stockpile. Seeds it with a few starter ammo box stacks and
	 * spawns a couple of dedicated ammo-runner logistics drones (created
	 * without a build-payment mission, so {@code SupplyRunGoal} never claims
	 * them and they fall straight to {@code AmmoRunGoal} instead), then
	 * registers the spot as a {@link SupplyNetwork} ammo depot so {@code
	 * AmmoRunGoal} can find it efficiently instead of scanning the whole map.
	 * Same terrain-scan idiom as {@code ProgramDirectorState.placeAssembler}:
	 * tries each horizontal direction at a fixed offset, falls back to a
	 * fixed offset if nothing looks clear.
	 */
	public static void buildStockpile(ServerWorld world, BlockPos core, SupplyNetwork supplyNetwork) {
		BlockPos site = findStockpileSite(world, core);
		buildFlatArea(world, site, STOCKPILE_RADIUS);

		supplyNetwork.registerDepot(site, SupplyNetwork.SUPPLY_AMMO,
				SupplyNetwork.AMMO_STOCKPILE_RADIUS, SupplyNetwork.AMMO_STOCKPILE_CAPACITY);

		BlockState ammoBox = P7Blocks.AMMO_BOX.get().getDefaultState();
		for (int i = 0; i < STARTER_AMMO_STACKS; i++) {
			BlockPos boxPos = site.add(i - 1, 0, 1);
			if (world.getBlockState(boxPos).isReplaceable()) {
				world.setBlockState(boxPos, ammoBox.with(AmmoBoxBlock.COUNT, 4));
			}
		}

		for (int i = 0; i < STARTER_AMMO_RUNNERS; i++) {
			LogisticsDroneEntity runner = P7Entities.LOGISTICS_DRONE.get().create(world);
			if (runner == null) {
				continue;
			}
			runner.refreshPositionAndAngles(site.getX() + 0.5, site.getY() + 1.5, site.getZ() + 0.5,
					world.getRandom().nextFloat() * 360.0f, 0.0f);
			// Deliberately no beginMission(...) call: an idle logistics drone
			// (no destination) never triggers SupplyRunGoal, so it's free to
			// pick up AmmoRunGoal work indefinitely instead of one-and-done
			// like a build-payment courier.
			world.spawnEntity(runner);
		}
	}

	/** {@link #build}'s flatten+plate loop, but at an arbitrary radius with no barrier ring — used by the stockpile apron. */
	private static void buildFlatArea(ServerWorld world, BlockPos center, int radius) {
		boolean mobGriefing = world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
		P7Config.DiggingPolicy policy = Program7.CONFIG.diggingPolicy;
		BlockState plating = P7Blocks.PLATING.get().getDefaultState();

		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				BlockPos groundPos = center.add(x, 0, z);
				clearHeadroom(world, groundPos, mobGriefing, policy);

				BlockPos floorPos = groundPos.down();
				if (canBuildAt(world, floorPos, mobGriefing, policy)) {
					world.setBlockState(floorPos, plating);
				}
			}
		}
	}

	/** Clears three blocks of walkable headroom starting at {@code groundPos} itself (never touches anything unclearable — see {@link #canBuildAt}). */
	private static void clearHeadroom(ServerWorld world, BlockPos groundPos, boolean mobGriefing,
			P7Config.DiggingPolicy policy) {
		for (int dy = 0; dy <= 2; dy++) {
			BlockPos cell = groundPos.up(dy);
			if (!world.getBlockState(cell).isAir() && canBuildAt(world, cell, mobGriefing, policy)) {
				world.setBlockState(cell, Blocks.AIR.getDefaultState());
			}
		}
	}

	private static boolean canBuildAt(ServerWorld world, BlockPos pos, boolean mobGriefing,
			P7Config.DiggingPolicy policy) {
		BlockState state = world.getBlockState(pos);
		if (state.isReplaceable()) {
			return true;
		}
		if (!mobGriefing) {
			return false; // mobGriefing off: never break existing solid terrain
		}
		if (policy == P7Config.DiggingPolicy.AGGRESSIVE) {
			return true;
		}
		return isClearableTerrain(state);
	}

	/** PROTECT/MINIMAL: only ever clear recognizably natural terrain, never anything that might be player-built. */
	private static boolean isClearableTerrain(BlockState state) {
		return state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
				|| state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES)
				|| state.isIn(BlockTags.SAND) || state.isIn(BlockTags.ICE)
				|| state.isIn(BlockTags.SNOW) || state.isIn(BlockTags.FLOWERS)
				|| state.isIn(BlockTags.SAPLINGS) || state.isIn(BlockTags.REPLACEABLE);
	}

	/** Same terrain scan as {@code ProgramDirectorState.placeAssembler}, offset 8 (past the fuel plant's 6). */
	private static BlockPos findStockpileSite(ServerWorld world, BlockPos core) {
		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos base = core.offset(direction, 8);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos candidate = base.up(dy);
				if (world.getBlockState(candidate).isReplaceable()
						&& world.getBlockState(candidate.down()).isSolidBlock(world, candidate.down())) {
					return candidate;
				}
			}
		}
		return core.north(8);
	}
}
