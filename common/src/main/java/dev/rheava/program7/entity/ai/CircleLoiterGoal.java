package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.AirUAVEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

/**
 * Station keeping for the Air UAV: hold a lazy circle above its launch
 * catapult at a fixed altitude and radius. Purely a MOVE goal — {@link
 * UAVSpotGoal} rides alongside it to handle looking around for a target
 * without interrupting the loiter.
 */
public class CircleLoiterGoal extends Goal {
	private static final double ANGLE_STEP_DEGREES = 25.0;

	private final AirUAVEntity uav;
	private final double radius;
	private final double altitude;
	private final double speed;
	private double loiterAngle;

	public CircleLoiterGoal(AirUAVEntity uav, double radius, double altitude, double speed) {
		this.uav = uav;
		this.radius = radius;
		this.altitude = altitude;
		this.speed = speed;
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		return this.uav.getHomePos() != null && !this.uav.isScrambled();
	}

	@Override
	public boolean shouldContinue() {
		return this.canStart();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		BlockPos home = this.uav.getHomePos();
		if (home == null || !this.uav.getNavigation().isIdle()) {
			return;
		}
		this.loiterAngle += Math.toRadians(ANGLE_STEP_DEGREES);
		double x = home.getX() + 0.5 + Math.cos(this.loiterAngle) * this.radius;
		double y = home.getY() + this.altitude;
		double z = home.getZ() + 0.5 + Math.sin(this.loiterAngle) * this.radius;
		this.uav.getNavigation().startMovingTo(x, y, z, this.speed);
	}
}
