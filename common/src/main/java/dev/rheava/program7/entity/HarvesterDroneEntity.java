package dev.rheava.program7.entity;

import java.util.HashMap;
import java.util.Map;

import dev.rheava.program7.entity.ai.DepositCargoGoal;
import dev.rheava.program7.entity.ai.MineResourceGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/**
 * The economy on wheels: finds ore and wood the Program needs, grinds it out
 * of the ground, and hauls it home to the Director's ledger. Completely
 * unarmed — it panics when hurt. Killing haulers is economic warfare: every
 * dead harvester is a response the Director can't afford later.
 */
public class HarvesterDroneEntity extends ProgramDroneEntity {
	public static final int CARGO_CAPACITY = 12;

	/** Ledger units by resource key, mined but not yet delivered. */
	private final Map<String, Integer> cargo = new HashMap<>();

	public HarvesterDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 5;
	}

	public static DefaultAttributeContainer.Builder createHarvesterDroneAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4);
	}

	@Override
	protected boolean isFlier() {
		// Ground unit: knockback shoves it around but doesn't scramble it.
		return false;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new EscapeDangerGoal(this, 1.5));
		this.goalSelector.add(2, new DepositCargoGoal(this));
		this.goalSelector.add(3, new MineResourceGoal(this));
		this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.8));
		this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		this.goalSelector.add(6, new LookAroundGoal(this));
	}

	public int cargoTotal() {
		int total = 0;
		for (int amount : this.cargo.values()) {
			total += amount;
		}
		return total;
	}

	public boolean isCargoFull() {
		return this.cargoTotal() >= CARGO_CAPACITY;
	}

	public void addCargo(String resource, int amount) {
		this.cargo.merge(resource, amount, Integer::sum);
	}

	/** Hands over everything and empties the hopper. */
	public Map<String, Integer> drainCargo() {
		Map<String, Integer> drained = new HashMap<>(this.cargo);
		this.cargo.clear();
		return drained;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 100;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5f;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		NbtCompound cargoTag = new NbtCompound();
		this.cargo.forEach(cargoTag::putInt);
		nbt.put("ResourceCargo", cargoTag);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.cargo.clear();
		NbtCompound cargoTag = nbt.getCompound("ResourceCargo");
		for (String key : cargoTag.getKeys()) {
			this.cargo.put(key, cargoTag.getInt(key));
		}
	}
}
