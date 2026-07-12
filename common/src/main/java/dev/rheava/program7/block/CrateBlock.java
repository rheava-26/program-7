package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

/**
 * Shipping crate: the drone's general-cargo container for non-modded loot
 * (raw ore, blocks it clears off the map). Banded wood with a cyan faction
 * label. Stacks 1-4 like the rest of the {@link CargoBlock cargo} family; low
 * blast power since it's crates of rock, not ordnance.
 */
public class CrateBlock extends CargoBlock {
	public static final MapCodec<CrateBlock> CODEC = createCodec(CrateBlock::new);

	public CrateBlock(Settings settings) {
		super(settings, 9, 1.0f);
	}

	@Override
	protected MapCodec<CrateBlock> getCodec() {
		return CODEC;
	}
}
