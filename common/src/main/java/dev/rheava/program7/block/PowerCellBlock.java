package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

/**
 * Power cell: the charge cargo every drone needs alongside ammo — a drone that
 * runs dry on cells (or on ammo) breaks off and retreats. Dark casing with two
 * glowing cyan charge rings and a top readout. Cooks off hard when lit; a
 * battery bank is not something you want to shoot with fire. See
 * {@link CargoBlock}.
 */
public class PowerCellBlock extends CargoBlock {
	public static final MapCodec<PowerCellBlock> CODEC = createCodec(PowerCellBlock::new);

	public PowerCellBlock(Settings settings) {
		super(settings, 10, 3.0f);
	}

	@Override
	protected MapCodec<PowerCellBlock> getCodec() {
		return CODEC;
	}
}
