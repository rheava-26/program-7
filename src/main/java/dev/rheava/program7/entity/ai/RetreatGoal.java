package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.SurveyorDroneEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

/**
 * Break contact after a completed scan: climb and put distance between the
 * drone and whoever it just profiled. Surveyors don't fight — they leave.
 */
public class RetreatGoal extends Goal {
	private final SurveyorDroneEntity drone;

	public RetreatGoal(SurveyorDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		return this.drone.isRetreating();
	}

	@Override
	public boolean shouldContinue() {
		return this.drone.isRetreating();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity threat = this.drone.getRetreatFrom();
		if (threat == null) {
			return;
		}
		if (this.drone.getNavigation().isIdle()) {
			Vec3d away = this.drone.getPos().subtract(threat.getPos());
			if (away.lengthSquared() < 1.0E-4) {
				away = new Vec3d(1.0, 0.0, 0.0);
			}
			Vec3d goal = this.drone.getPos().add(away.normalize().multiply(12.0)).add(0.0, 5.0, 0.0);
			this.drone.getNavigation().startMovingTo(goal.x, goal.y, goal.z, 1.4);
		}
	}

	@Override
	public void stop() {
		this.drone.getNavigation().stop();
	}
}
