package dev.rheava.program7.entity.ai;

import java.util.List;

import dev.rheava.program7.entity.BatteryCenterEntity;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

/**
 * The battery center's whole reason to exist: a standing field-repair aura.
 * Every {@link #REPAIR_INTERVAL} ticks it tops off every hurt Program unit in
 * range for a small trickle of health — not enough to turn a fight, but
 * enough that grinding down a supported position takes forever with the
 * truck still parked in it. No movement or look control: the battery center
 * keeps doing this while it wanders, follows the pack, or just sits still.
 */
public class RepairAuraGoal extends Goal {
	private static final double REPAIR_RANGE = 16.0;
	private static final int REPAIR_INTERVAL = 40;
	private static final float HEAL_AMOUNT = 1.0f;

	private final BatteryCenterEntity battery;
	private int ticksUntilRepair;

	public RepairAuraGoal(BatteryCenterEntity battery) {
		this.battery = battery;
		// Passive: no controls, so it never competes with movement/look goals.
	}

	@Override
	public boolean canStart() {
		return this.hasDamagedUnitNearby();
	}

	@Override
	public boolean shouldContinue() {
		return this.hasDamagedUnitNearby();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.ticksUntilRepair = REPAIR_INTERVAL;
	}

	@Override
	public void tick() {
		this.ticksUntilRepair--;
		if (this.ticksUntilRepair > 0) {
			return;
		}
		this.ticksUntilRepair = REPAIR_INTERVAL;
		this.repairNearbyUnits();
	}

	private boolean hasDamagedUnitNearby() {
		Box box = this.battery.getBoundingBox().expand(REPAIR_RANGE);
		return !this.battery.getWorld().getEntitiesByClass(ProgramDroneEntity.class, box, this::isRepairable).isEmpty();
	}

	private void repairNearbyUnits() {
		if (!(this.battery.getWorld() instanceof ServerWorld world)) {
			return;
		}
		Box box = this.battery.getBoundingBox().expand(REPAIR_RANGE);
		List<ProgramDroneEntity> units = world.getEntitiesByClass(ProgramDroneEntity.class, box, this::isRepairable);
		if (units.isEmpty()) {
			return;
		}
		for (ProgramDroneEntity unit : units) {
			unit.heal(HEAL_AMOUNT);
			world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
					unit.getX(), unit.getY() + unit.getHeight() * 0.5, unit.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
		}
		this.battery.playSound(P7Sounds.ASSEMBLER_WORKING.get(), 0.3f, 1.0f);
	}

	private boolean isRepairable(ProgramDroneEntity unit) {
		return unit != this.battery && unit.isAlive() && unit.getHealth() < unit.getMaxHealth();
	}
}
