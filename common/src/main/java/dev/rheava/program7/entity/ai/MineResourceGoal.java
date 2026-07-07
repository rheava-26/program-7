package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.director.HarvestTargets;
import dev.rheava.program7.entity.HarvesterDroneEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Find the nearest block the Program wants, drive up to it, and grind it out
 * of the world — visible crack stages, mining noise, the lot. The block's
 * yield goes into the hopper as ledger units; nothing drops on the ground.
 */
public class MineResourceGoal extends Goal {
	private static final int SEARCH_RADIUS = 20;
	private static final int VERTICAL_RADIUS = 8;
	private static final double WORK_RANGE = 2.8;
	private static final int MINE_TICKS = 80;
	private static final int STUCK_LIMIT = 300;

	private final HarvesterDroneEntity drone;
	@Nullable
	private BlockPos target;
	private int progress;
	private int stuckTicks;
	private int searchCooldown;

	public MineResourceGoal(HarvesterDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.drone.isCargoFull()) {
			return false;
		}
		if (this.searchCooldown > 0) {
			this.searchCooldown--;
			return false;
		}
		this.searchCooldown = 40;
		this.target = this.findTarget();
		return this.target != null;
	}

	@Nullable
	private BlockPos findTarget() {
		for (BlockPos pos : BlockPos.iterateOutwards(this.drone.getBlockPos(),
				SEARCH_RADIUS, VERTICAL_RADIUS, SEARCH_RADIUS)) {
			BlockState state = this.drone.getWorld().getBlockState(pos);
			if (HarvestTargets.resourceFor(state) != null) {
				return pos.toImmutable();
			}
		}
		return null;
	}

	@Override
	public boolean shouldContinue() {
		return this.target != null
				&& !this.drone.isCargoFull()
				&& this.stuckTicks < STUCK_LIMIT
				&& HarvestTargets.resourceFor(this.drone.getWorld().getBlockState(this.target)) != null;
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.progress = 0;
		this.stuckTicks = 0;
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}
		double centerX = this.target.getX() + 0.5;
		double centerY = this.target.getY() + 0.5;
		double centerZ = this.target.getZ() + 0.5;
		this.drone.getLookControl().lookAt(centerX, centerY, centerZ);

		double distanceSq = this.drone.squaredDistanceTo(centerX, centerY, centerZ);
		if (distanceSq > WORK_RANGE * WORK_RANGE) {
			this.stuckTicks++;
			if (this.drone.getNavigation().isIdle()) {
				this.drone.getNavigation().startMovingTo(centerX, this.target.getY(), centerZ, 1.0);
			}
			return;
		}

		this.drone.getNavigation().stop();
		this.stuckTicks = 0;
		this.progress++;

		BlockState state = this.drone.getWorld().getBlockState(this.target);
		if (this.progress % 5 == 0) {
			this.drone.getWorld().playSound(null, this.target,
					state.getSoundGroup().getHitSound(), SoundCategory.BLOCKS, 0.5f, 1.0f);
		}
		this.drone.getWorld().setBlockBreakingInfo(this.drone.getId(), this.target,
				this.progress * 10 / MINE_TICKS);

		if (this.progress >= MINE_TICKS) {
			String resource = HarvestTargets.resourceFor(state);
			if (resource != null) {
				this.drone.addCargo(resource,
						HarvestTargets.yieldFor(resource, this.drone.getRandom()));
			}
			this.drone.getWorld().breakBlock(this.target, false, this.drone);
			this.target = null;
		}
	}

	@Override
	public void stop() {
		if (this.target != null) {
			this.drone.getWorld().setBlockBreakingInfo(this.drone.getId(), this.target, -1);
			this.target = null;
		}
		this.progress = 0;
		this.drone.getNavigation().stop();
	}
}
