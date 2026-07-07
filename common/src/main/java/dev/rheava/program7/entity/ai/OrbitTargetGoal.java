package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.ReconHelicopterEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;

/**
 * Station keeping for the Recon Helicopter once it has a target: hold a
 * circle eight blocks above their head, out of easy melee and short-range
 * reach. Purely a MOVE goal — {@link SearchlightSpotGoal} rides alongside it
 * to keep the beam (and eventually the paint) locked on without the two
 * fighting over control of the airframe.
 *
 * <p>Orbiting above rather than at head height is the point: it forces the
 * counter into ranged fire, and a Knockback/Punch hit still scrambles the
 * helicopter through {@link dev.rheava.program7.entity.ProgramDroneEntity}'s
 * base handling — which cuts navigation entirely until the tumble settles,
 * so a good shot knocks the orbit into a crash regardless of what this goal
 * wants to do next.
 */
public class OrbitTargetGoal extends Goal {
	private static final double ANGLE_STEP_DEGREES = 18.0;
	private static final double VERTICAL_OFFSET = 8.0;

	private final ReconHelicopterEntity heli;
	private final double radius;
	private final double speed;
	private double orbitAngle;

	public OrbitTargetGoal(ReconHelicopterEntity heli, double radius, double speed) {
		this.heli = heli;
		this.radius = radius;
		this.speed = speed;
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		return this.heli.getTarget() != null && !this.heli.isScrambled();
	}

	@Override
	public boolean shouldContinue() {
		LivingEntity target = this.heli.getTarget();
		return target != null && target.isAlive() && !this.heli.isScrambled();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = this.heli.getTarget();
		if (target == null || !this.heli.getNavigation().isIdle()) {
			return;
		}
		// Persistent orbit angle: only advances when the last leg of the
		// circle finished, so the helicopter actually traces a ring instead
		// of chasing a moving point straight in.
		this.orbitAngle += Math.toRadians(ANGLE_STEP_DEGREES);
		double x = target.getX() + Math.cos(this.orbitAngle) * this.radius;
		double y = target.getY() + VERTICAL_OFFSET;
		double z = target.getZ() + Math.sin(this.orbitAngle) * this.radius;
		this.heli.getNavigation().startMovingTo(x, y, z, this.speed);
	}
}
