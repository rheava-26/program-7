package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.AttackDroneEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;

/**
 * Run the target down and light the fuse at contact range. There is no
 * defusing — once armed, the drone is committed, so kiting it into terrain
 * (or your enemies) is legitimate counterplay.
 */
public class ChaseAndDetonateGoal extends Goal {
	private static final double ARM_DISTANCE = 2.75;

	private final AttackDroneEntity drone;

	public ChaseAndDetonateGoal(AttackDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity target = this.drone.getTarget();
		return target != null && target.isAlive();
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
		LivingEntity target = this.drone.getTarget();
		if (target == null) {
			return;
		}
		this.drone.getLookControl().lookAt(target, 30.0f, 30.0f);
		this.drone.getNavigation().startMovingTo(
				target.getX(), target.getBodyY(0.5), target.getZ(), 1.4);

		if (this.drone.squaredDistanceTo(target) <= ARM_DISTANCE * ARM_DISTANCE) {
			this.drone.arm();
		}
	}
}
