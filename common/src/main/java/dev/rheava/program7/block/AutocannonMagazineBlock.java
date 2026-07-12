package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

/**
 * Autocannon magazine: the tall, thin medium-weapon cargo feeding IFV and
 * gunship autocannons. A glowing cyan round-count window runs up the front.
 * Stacks 1-4 and cooks off harder than a small-arms box — see
 * {@link CargoBlock}.
 */
public class AutocannonMagazineBlock extends CargoBlock {
	public static final MapCodec<AutocannonMagazineBlock> CODEC = createCodec(AutocannonMagazineBlock::new);

	public AutocannonMagazineBlock(Settings settings) {
		super(settings, 11, 2.0f);
	}

	@Override
	protected MapCodec<AutocannonMagazineBlock> getCodec() {
		return CODEC;
	}
}
