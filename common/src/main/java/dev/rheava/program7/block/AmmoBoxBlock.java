package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

/**
 * Small-arms ammo box: the light-round cargo a logistics drone draws down to
 * refill turrets and drone guns. Stacks 1-4 like a sea pickle and cooks off if
 * lit — see {@link CargoBlock} for the shared behaviour. This is the physical
 * unit {@code AmmoRunGoal} consumes at a base's ammo stockpile (planted by
 * {@code director.BasePad}).
 */
public class AmmoBoxBlock extends CargoBlock {
	public static final MapCodec<AmmoBoxBlock> CODEC = createCodec(AmmoBoxBlock::new);

	public AmmoBoxBlock(Settings settings) {
		super(settings, 6, 1.5f);
	}

	@Override
	protected MapCodec<AmmoBoxBlock> getCodec() {
		return CODEC;
	}
}
