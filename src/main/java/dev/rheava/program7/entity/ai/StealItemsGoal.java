package dev.rheava.program7.entity.ai;

import java.util.EnumSet;
import java.util.List;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.SurveyorDroneEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;

/**
 * Leave your gear lying around and the Program repossesses it. The surveyor
 * swoops on dropped item stacks and carries them as cargo — shoot the thief
 * down and everything it took falls right back out.
 */
public class StealItemsGoal extends Goal {
	private static final double SEARCH_RANGE = 16.0;
	private static final double GRAB_RANGE = 2.0;
	private static final int STEAL_COOLDOWN = 100;

	private final SurveyorDroneEntity drone;
	@Nullable
	private ItemEntity target;
	private int cooldown;

	public StealItemsGoal(SurveyorDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (!Program7.CONFIG.itemStealing || this.drone.isRetreating() || this.drone.isCargoFull()) {
			return false;
		}
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}
		List<ItemEntity> items = this.drone.getWorld().getEntitiesByClass(ItemEntity.class,
				this.drone.getBoundingBox().expand(SEARCH_RANGE),
				item -> item.isAlive() && !item.getStack().isEmpty() && item.isOnGround());
		if (items.isEmpty()) {
			return false;
		}
		ItemEntity nearest = null;
		double best = Double.MAX_VALUE;
		for (ItemEntity item : items) {
			double distance = this.drone.squaredDistanceTo(item);
			if (distance < best) {
				best = distance;
				nearest = item;
			}
		}
		this.target = nearest;
		return this.target != null;
	}

	@Override
	public boolean shouldContinue() {
		return this.target != null && this.target.isAlive()
				&& !this.drone.isRetreating() && !this.drone.isCargoFull()
				&& this.drone.squaredDistanceTo(this.target) < 32.0 * 32.0;
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}
		this.drone.getLookControl().lookAt(this.target, 30.0f, 30.0f);
		this.drone.getNavigation().startMovingTo(
				this.target.getX(), this.target.getY() + 1.0, this.target.getZ(), 1.1);

		if (this.drone.squaredDistanceTo(this.target) <= GRAB_RANGE * GRAB_RANGE) {
			this.drone.addCargo(this.target.getStack().copy());
			this.target.discard();
			this.drone.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 0.8f);
			this.cooldown = STEAL_COOLDOWN;
			this.target = null;
		}
	}

	@Override
	public void stop() {
		this.target = null;
		this.drone.getNavigation().stop();
	}
}
