package dev.rheava.program7.block;

import java.util.Locale;

import net.minecraft.util.StringIdentifiable;

/**
 * Which cell of the 2x2 assembler multiblock a given {@code assembler} block
 * state occupies. {@link #MASTER} is the northwest corner — the only cell
 * that ever gets a real {@link AssemblerBlockEntity} (see
 * {@link AssemblerBlock#createBlockEntity}); the other three are inert
 * "slave" cells that exist purely to occupy the footprint and get cleaned up
 * together with the master when any one of the four is broken (see
 * {@link AssemblerBlock#onStateReplaced}).
 *
 * <p>{@link #dx()}/{@link #dz()} are this cell's offset from the master, so
 * any cell can recover the master's position with
 * {@code pos.add(-part.dx(), 0, -part.dz())}.
 */
public enum AssemblerPart implements StringIdentifiable {
	MASTER(0, 0),
	EAST(1, 0),
	SOUTH(0, 1),
	SOUTHEAST(1, 1);

	private final int dx;
	private final int dz;

	AssemblerPart(int dx, int dz) {
		this.dx = dx;
		this.dz = dz;
	}

	public int dx() {
		return this.dx;
	}

	public int dz() {
		return this.dz;
	}

	@Override
	public String asString() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
