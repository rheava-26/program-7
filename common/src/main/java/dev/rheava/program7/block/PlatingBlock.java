package dev.rheava.program7.block;

import net.minecraft.block.Block;

/**
 * Metal deck plate: the Program's landing-pad flooring (see {@code
 * director.BasePad}). A plain full solid block — no block entity, no
 * properties — laid down under and around every fresh landing site as the
 * pad floor, and again for base stockpile aprons. Kept as its own named
 * class (rather than a bare {@code new Block(...)} like {@code
 * METAL_SCAFFOLD}) so the pad-builder and any future recipes have a stable
 * type to check against.
 */
public class PlatingBlock extends Block {
	public PlatingBlock(Settings settings) {
		super(settings);
	}
}
