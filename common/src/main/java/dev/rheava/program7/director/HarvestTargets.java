package dev.rheava.program7.director;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * What the Program's harvesters consider worth mining, and what a block is
 * worth in ledger units. Tag-based so modded ores that extend the vanilla
 * tags get harvested too (Pillar 5: work with the ecosystem).
 */
public final class HarvestTargets {
	@Nullable
	public static String resourceFor(BlockState state) {
		if (state.isIn(BlockTags.IRON_ORES)) {
			return Resources.IRON;
		}
		if (state.isIn(BlockTags.COPPER_ORES)) {
			return Resources.COPPER;
		}
		if (state.isIn(BlockTags.REDSTONE_ORES)) {
			return Resources.REDSTONE;
		}
		if (state.isIn(BlockTags.COAL_ORES)) {
			return Resources.COAL;
		}
		if (state.isIn(BlockTags.GOLD_ORES)) {
			return Resources.GOLD;
		}
		if (state.isIn(BlockTags.DIAMOND_ORES)) {
			return Resources.DIAMOND;
		}
		if (state.isOf(Blocks.ANCIENT_DEBRIS)) {
			return Resources.NETHERITE;
		}
		// Natural geode growth — safe to harvest without eating player builds.
		if (state.isOf(Blocks.AMETHYST_CLUSTER)) {
			return Resources.AMETHYST;
		}
		if (state.isIn(BlockTags.LOGS)) {
			return Resources.WOOD;
		}
		// GLOWSTONE and PRISMARINE are in the ledger's material tree but are NOT
		// wired as harvest targets yet: both are common player building blocks,
		// so mining them waits on the PROTECT digging-policy enforcement (don't
		// strip a player's decorative builds). They arrive via processing /
		// Nether + ocean expansion instead.
		return null;
	}

	/** Ledger units extracted from one block. */
	public static int yieldFor(String resource, Random random) {
		return switch (resource) {
			case Resources.REDSTONE -> 3 + random.nextInt(3);
			case Resources.COPPER, Resources.AMETHYST, Resources.GLOWSTONE, Resources.PRISMARINE ->
					2 + random.nextInt(3);
			case Resources.IRON, Resources.COAL, Resources.GOLD -> 1 + random.nextInt(2);
			// Diamond and netherite are precious: one unit per block, so a whole
			// tech tier's worth of them is a real, raid-worthy stockpile.
			case Resources.DIAMOND, Resources.NETHERITE -> 1;
			default -> 1;
		};
	}

	private HarvestTargets() {
	}
}
