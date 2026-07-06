package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.SurveyorDroneEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Idle patrol: drift to a nearby open-air position and hold. Keeps the drone
 * looking busy (and audibly whirring around) between scans.
 */
public class HoverWanderGoal extends Goal {
	private final SurveyorDroneEntity drone;

	public HoverWanderGoal(SurveyorDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		return this.drone.getNavigation().isIdle() && this.drone.getRandom().nextInt(30) == 0;
	}

	@Override
	public boolean shouldContinue() {
		return !this.drone.getNavigation().isIdle() && !this.drone.isRetreating();
	}

	@Override
	public void start() {
		Random random = this.drone.getRandom();
		for (int attempt = 0; attempt < 8; attempt++) {
			BlockPos pos = this.drone.getBlockPos().add(
					random.nextInt(17) - 8,
					random.nextInt(7) - 2,
					random.nextInt(17) - 8);
			if (this.drone.getWorld().isAir(pos)) {
				this.drone.getNavigation().startMovingTo(
						pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0);
				return;
			}
		}
	}
}
