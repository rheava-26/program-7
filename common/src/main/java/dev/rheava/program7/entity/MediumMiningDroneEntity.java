package dev.rheava.program7.entity;

import java.util.HashMap;
import java.util.Map;

import dev.rheava.program7.entity.ai.DepositCargoGoal;
import dev.rheava.program7.entity.ai.HoverWanderGoal;
import dev.rheava.program7.entity.ai.InertialFlightMoveControl;
import dev.rheava.program7.entity.ai.MineResourceGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/**
 * Tier 2 workhorse: the harvester's airborne cousin, same mine-and-bank job
 * but faster and with a bigger hopper — flies straight over terrain the
 * wheeled unit has to path around. Completely unarmed — it panics when hurt.
 *
 * <p>Cave-boring and light beacons are a later pass; out of scope here.
 */
public class MediumMiningDroneEntity extends ProgramDroneEntity implements CargoHauler {
	public static final int CARGO_CAPACITY = 24;

	/** Ledger units by resource key, mined but not yet delivered. */
	private final Map<String, Integer> cargo = new HashMap<>();

	public MediumMiningDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new InertialFlightMoveControl(this, 20, true, 2.5f);
		this.experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createMediumMiningDroneAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
				.add(EntityAttributes.GENERIC_ARMOR, 2.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new EscapeDangerGoal(this, 1.5));
		this.goalSelector.add(2, new DepositCargoGoal(this));
		this.goalSelector.add(3, new MineResourceGoal(this));
		this.goalSelector.add(4, new HoverWanderGoal(this));
		this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation navigation = new BirdNavigation(this, world);
		navigation.setCanPathThroughDoors(false);
		navigation.setCanSwim(false);
		navigation.setCanEnterOpenDoors(true);
		return navigation;
	}

	@Override
	public int cargoTotal() {
		int total = 0;
		for (int amount : this.cargo.values()) {
			total += amount;
		}
		return total;
	}

	@Override
	public boolean isCargoFull() {
		return this.cargoTotal() >= CARGO_CAPACITY;
	}

	@Override
	public void addCargo(String resource, int amount) {
		this.cargo.merge(resource, amount, Integer::sum);
	}

	/** Hands over everything and empties the hopper. */
	@Override
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
