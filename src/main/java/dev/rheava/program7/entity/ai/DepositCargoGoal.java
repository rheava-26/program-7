package dev.rheava.program7.entity.ai;

import java.util.EnumSet;
import java.util.Map;

import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.entity.HarvesterDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Hopper's full — drive home and empty it into the Director's ledger.
 * This is the moment the Program actually gets richer, and the convoy leg
 * players can ambush.
 */
public class DepositCargoGoal extends Goal {
	private static final double DEPOSIT_RANGE = 3.5;
	private static final int STUCK_LIMIT = 600;

	private final HarvesterDroneEntity drone;
	@Nullable
	private BlockPos corePos;
	private int stuckTicks;

	public DepositCargoGoal(HarvesterDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (!this.drone.isCargoFull() || !(this.drone.getWorld() instanceof ServerWorld world)) {
			return false;
		}
		this.corePos = ProgramDirectorState.get(world).getProbeCorePos();
		return this.corePos != null;
	}

	@Override
	public boolean shouldContinue() {
		return this.corePos != null && this.drone.cargoTotal() > 0 && this.stuckTicks < STUCK_LIMIT;
	}

	@Override
	public void start() {
		this.stuckTicks = 0;
	}

	@Override
	public void tick() {
		if (this.corePos == null || !(this.drone.getWorld() instanceof ServerWorld world)) {
			return;
		}
		double centerX = this.corePos.getX() + 0.5;
		double centerY = this.corePos.getY() + 0.5;
		double centerZ = this.corePos.getZ() + 0.5;
		this.drone.getLookControl().lookAt(centerX, centerY, centerZ);

		if (this.drone.squaredDistanceTo(centerX, centerY, centerZ) > DEPOSIT_RANGE * DEPOSIT_RANGE) {
			this.stuckTicks++;
			if (this.drone.getNavigation().isIdle()) {
				this.drone.getNavigation().startMovingTo(centerX, this.corePos.getY(), centerZ, 1.0);
			}
			return;
		}

		ProgramDirectorState state = ProgramDirectorState.get(world);
		for (Map.Entry<String, Integer> entry : this.drone.drainCargo().entrySet()) {
			state.addResource(entry.getKey(), entry.getValue());
		}
		this.drone.playSound(P7Sounds.DRONE_SCAN_BEEP, 0.8f, 1.8f);
		world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
				centerX, centerY + 0.8, centerZ, 8, 0.4, 0.4, 0.4, 0.02);
	}

	@Override
	public void stop() {
		this.corePos = null;
		this.drone.getNavigation().stop();
	}
}
