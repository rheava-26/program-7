package dev.rheava.program7.entity.ai;

import net.minecraft.util.math.Vec3d;

/**
 * The drag-aware ballistic solve, factored out of {@link HowitzerAttackGoal}
 * so {@link MortarAttackGoal} (and every artillery type after it) shares one
 * correct implementation instead of each hand-rolling its own — this is the
 * BACKLOG "review findings" fix for the mortar's naive {@code dx / FLIGHT_TICKS}
 * constant-speed backfill, which assumes no drag and lands short.
 *
 * <p>Every {@link dev.rheava.program7.entity.AbstractShellEntity} subclass is
 * a {@code ThrownEntity}, which bleeds {@link #DRAG}(=1%) of its horizontal
 * speed every tick and falls under its own tuned gravity. A naive backfill
 * (constant speed over a fixed flight time) ignores that decay and
 * undershoots worse the farther the round has to fly. Instead this simulates
 * the vertical arc from the muzzle down to the target's relative height to
 * find the true airtime, accumulating the horizontal decay sum
 * (1 + d + d^2 + ...) over exactly those ticks; the launch speed that
 * actually covers the horizontal distance is then {@code dx / decaySum}. The
 * sim mirrors {@code ThrownEntity}'s own integration order: move by the
 * current velocity, then apply drag, then gravity.
 */
public final class BallisticSolver {
	/** {@code ThrownEntity}'s own per-tick horizontal drag — every shell subclass inherits this, unmodified. */
	public static final double DRAG = 0.99;
	/** Safety cap on the trajectory sim so a target the arc can't reach (far above the tube) can't spin the solve forever. */
	private static final int MAX_FLIGHT_TICKS = 600;

	private BallisticSolver() {
	}

	/**
	 * Solves the horizontal launch velocity (x, z) needed for a shell fired
	 * with vertical launch speed {@code launchVelocityY} and gravity
	 * {@code gravity} to travel from {@code (0, 0)} to {@code (dx, targetRelY, dz)}
	 * relative to the muzzle. Returns a {@link Vec3d} whose {@code y} is
	 * always {@code launchVelocityY} unchanged, so callers can hand the
	 * result straight to {@code Entity#setVelocity}.
	 */
	public static Vec3d solve(double dx, double dz, double targetRelY, double launchVelocityY, double gravity) {
		double vy = launchVelocityY;
		double y = 0.0;
		double decaySum = 0.0;
		double factor = 1.0;
		for (int k = 0; k < MAX_FLIGHT_TICKS; k++) {
			decaySum += factor;
			y += vy;
			vy = vy * DRAG - gravity;
			factor *= DRAG;
			if (vy < 0.0 && y <= targetRelY) {
				break;
			}
		}
		double vx = decaySum > 1.0E-6 ? dx / decaySum : 0.0;
		double vz = decaySum > 1.0E-6 ? dz / decaySum : 0.0;
		return new Vec3d(vx, launchVelocityY, vz);
	}

	/** Convenience overload taking muzzle and aim points directly. */
	public static Vec3d solve(Vec3d muzzle, Vec3d aim, double launchVelocityY, double gravity) {
		return solve(aim.x - muzzle.x, aim.z - muzzle.z, aim.y - muzzle.y, launchVelocityY, gravity);
	}
}
