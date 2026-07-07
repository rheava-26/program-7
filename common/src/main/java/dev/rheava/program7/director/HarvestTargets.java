package dev.rheava.program7.director;

import net.minecraft.block.BlockState;
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
		if (state.isIn(BlockTags.LOGS)) {
			return Resources.WOOD;
		}
		return null;
	}

	/** Ledger units extracted from one block. */
	public static int yieldFor(String resource, Random random) {
		return switch (resource) {
			case Resources.REDSTONE -> 3 + random.nextInt(3);
			case Resources.COPPER -> 2 + random.nextInt(3);
			case Resources.IRON, Resources.COAL -> 1 + random.nextInt(2);
			default -> 1;
		};
	}

	private HarvestTargets() {
	}
}
