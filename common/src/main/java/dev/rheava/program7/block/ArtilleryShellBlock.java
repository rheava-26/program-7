package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

/**
 * Artillery shell: the single large round drones physically carry to howitzers
 * and the gunboat's deck gun. Brass casing, copper driving band, olive ogive,
 * bright cyan fuze. The heaviest cook-off in the {@link CargoBlock cargo}
 * family — a lit shell dump goes up like a magazine detonation.
 */
public class ArtilleryShellBlock extends CargoBlock {
	public static final MapCodec<ArtilleryShellBlock> CODEC = createCodec(ArtilleryShellBlock::new);

	public ArtilleryShellBlock(Settings settings) {
		super(settings, 13, 3.5f);
	}

	@Override
	protected MapCodec<ArtilleryShellBlock> getCodec() {
		return CODEC;
	}
}
