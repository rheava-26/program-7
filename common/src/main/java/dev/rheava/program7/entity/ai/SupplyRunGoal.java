package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.block.AssemblerBlockEntity;
import dev.rheava.program7.entity.CourierUnit;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * The supply run shared by both couriers: navigate straight to the
 * destination the assembler handed out, hand over the cargo on arrival, and
 * dock — the courier's job is done, so it discards itself. If the assembler
 * didn't survive the trip, the payment spills out as loot instead of just
 * vanishing.
 */
public class SupplyRunGoal extends Goal {
	private static final double ARRIVAL_RANGE = 2.5;

	private final ProgramDroneEntity drone;
	private final CourierUnit courier;

	public <T extends ProgramDroneEntity & CourierUnit> SupplyRunGoal(T courier) {
		this.drone = courier;
		this.courier = courier;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		return this.courier.getDestination() != null && !this.drone.isScrambled();
	}

	@Override
	public boolean shouldContinue() {
		return this.courier.getDestination() != null && !this.drone.isScrambled();
	}

	@Override
	public void tick() {
		BlockPos destination = this.courier.getDestination();
		if (destination == null || !(this.drone.getWorld() instanceof ServerWorld world)) {
			return;
		}
		double centerX = destination.getX() + 0.5;
		double centerY = destination.getY() + 0.5;
		double centerZ = destination.getZ() + 0.5;
		this.drone.getLookControl().lookAt(centerX, centerY, centerZ);

		if (this.drone.squaredDistanceTo(centerX, centerY, centerZ) > ARRIVAL_RANGE * ARRIVAL_RANGE) {
			if (this.drone.getNavigation().isIdle()) {
				this.drone.getNavigation().startMovingTo(centerX, destination.getY(), centerZ, 1.0);
			}
			return;
		}

		this.deliver(world, destination);
	}

	private void deliver(ServerWorld world, BlockPos destination) {
		BlockEntity blockEntity = world.getBlockEntity(destination);
		if (blockEntity instanceof AssemblerBlockEntity assembler) {
			assembler.onSupplyDelivered(this.courier.getJob());
			this.drone.playSound(P7Sounds.ASSEMBLER_WORKING.get(), 0.4f, 1.1f);
			world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
					this.drone.getX(), this.drone.getY() + 0.5, this.drone.getZ(), 6, 0.3, 0.3, 0.3, 0.01);
		} else {
			// The assembler didn't make it: the payment gets dropped, not lost.
			for (ItemStack stack : CourierUnit.cargoToItems(this.courier.getCargo())) {
				this.drone.dropStack(stack);
			}
		}
		this.courier.clearMission();
		this.drone.discard();
	}

	@Override
	public void stop() {
		this.drone.getNavigation().stop();
	}
}
