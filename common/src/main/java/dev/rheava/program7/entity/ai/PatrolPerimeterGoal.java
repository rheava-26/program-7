package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.GroundDroneEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Perimeter duty: units built by an assembler patrol a loose ring around
 * their home structure instead of wandering off across the map. Straying
 * too far (a long chase, knockback) triggers a walk back.
 */
public class PatrolPerimeterGoal extends Goal {
	private final GroundDroneEntity drone;
	private final double speed;
	private final int radius;

	public PatrolPerimeterGoal(GroundDroneEntity drone, double speed, int radius) {
		this.drone = drone;
		this.speed = speed;
		this.radius = radius;
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (this.drone.getHomePos() == null || !this.drone.getNavigation().isIdle()) {
			return false;
		}
		return this.isFarFromHome() || this.drone.getRandom().nextInt(40) == 0;
	}

	@Override
	public boolean shouldContinue() {
		return !this.drone.getNavigation().isIdle();
	}

	@Override
	public void start() {
		BlockPos home = this.drone.getHomePos();
		if (home == null) {
			return;
		}
		if (this.isFarFromHome()) {
			this.drone.getNavigation().startMovingTo(
					home.getX() + 0.5, home.getY(), home.getZ() + 0.5, this.speed);
			return;
		}
		Random random = this.drone.getRandom();
		BlockPos target = home.add(
				random.nextInt(this.radius * 2 + 1) - this.radius,
				0,
				random.nextInt(this.radius * 2 + 1) - this.radius);
		this.drone.getNavigation().startMovingTo(
				target.getX() + 0.5, target.getY(), target.getZ() + 0.5, this.speed);
	}

	private boolean isFarFromHome() {
		BlockPos home = this.drone.getHomePos();
		return home != null && !this.drone.getBlockPos().isWithinDistance(home, this.radius * 2.0);
	}
}
