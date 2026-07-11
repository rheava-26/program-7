package dev.rheava.program7.block;

import net.minecraft.block.FenceBlock;

/**
 * The mechanical barrier post ringing a fresh Program landing pad (see
 * {@code director.BasePad}). Plain subclass of vanilla's {@link FenceBlock}
 * — reuses its connection logic (posts join to adjacent solid blocks and
 * other barrier posts exactly like a nether brick fence joins to other
 * fences) so the ring reads as a continuous mechanical perimeter without
 * reimplementing any of that shape/connection math.
 */
public class BarrierPostBlock extends FenceBlock {
	public BarrierPostBlock(Settings settings) {
		super(settings);
	}
}
