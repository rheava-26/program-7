package dev.rheava.program7.entity;

import java.util.ArrayList;
import java.util.List;

import dev.rheava.program7.entity.ai.ExploreGoal;
import dev.rheava.program7.entity.ai.HoverWanderGoal;
import dev.rheava.program7.entity.ai.InertialFlightMoveControl;
import dev.rheava.program7.entity.ai.InvestigateDisturbanceGoal;
import dev.rheava.program7.entity.ai.LandAndChargeGoal;
import dev.rheava.program7.entity.ai.RaidStorageGoal;
import dev.rheava.program7.entity.ai.RetreatGoal;
import dev.rheava.program7.entity.ai.ScanPlayerGoal;
import dev.rheava.program7.entity.ai.StealItemsGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.World;

/**
 * The Surveyor Drone — Program 7's eyes on the ground.
 *
 * <p>It is not a fighter. It closes to standoff range, sweeps the target with
 * an escalating scan (beeping faster and faster the closer it is to done),
 * files a threat profile with the Program Director, then withdraws before you
 * can grab it. Killing one interrupts the report; its wreck holds its salvage
 * plus everything it stole.
 */
public class SurveyorDroneEntity extends ProgramDroneEntity {
	private static final int CARGO_CAPACITY = 3;

	private int scanCooldown = 0;
	/** Stolen goods. Ends up in the wreck when the drone is destroyed. */
	private final List<ItemStack> cargo = new ArrayList<>();

	public SurveyorDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new InertialFlightMoveControl(this, 20, true, 1.0f);
		this.experiencePoints = 5;
	}

	public static DefaultAttributeContainer.Builder createSurveyorDroneAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
				// Raised for symmetric "if you can see it, it can see you"
				// LOS-gated perception (InvestigateDisturbanceGoal). 160 is the
				// practical ceiling for this attribute; true whole-region
				// (~600 block) symmetry needs entity simulation distance / a
				// future virtualization layer — a known limit, not fixed here.
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 160.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new RetreatGoal(this));
		this.goalSelector.add(2, new ScanPlayerGoal(this));
		this.goalSelector.add(3, new InvestigateDisturbanceGoal(this));
		// Priority 4: battery management (see #4) — outranks exploring/idle
		// duty below it, but a scan/retreat/disturbance in progress above it
		// always wins, so a low-charge surveyor still finishes what it's doing
		// before peeling off to land.
		this.goalSelector.add(4, new LandAndChargeGoal(this));
		// Priority 5: "prioritize exploring above all else" (see #3) — sits
		// above the scavenging/idle goals below it so a fresh surveyor spends
		// its time fanning out over new ground instead of immediately settling
		// into raiding storage or hovering near its spawn point.
		this.goalSelector.add(5, new ExploreGoal(this));
		this.goalSelector.add(6, new StealItemsGoal(this));
		this.goalSelector.add(7, new RaidStorageGoal(this));
		this.goalSelector.add(8, new HoverWanderGoal(this));
		this.goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
		this.goalSelector.add(10, new LookAroundGoal(this));
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
	public void tickMovement() {
		super.tickMovement();
		if (!this.getWorld().isClient) {
			if (this.scanCooldown > 0) {
				this.scanCooldown--;
			}
		}
	}

	public boolean isScanReady() {
		return this.scanCooldown <= 0;
	}

	public void setScanCooldown(int ticks) {
		this.scanCooldown = ticks;
	}

	public boolean isCargoFull() {
		return this.cargo.size() >= CARGO_CAPACITY;
	}

	public void addCargo(ItemStack stack) {
		if (!stack.isEmpty()) {
			this.cargo.add(stack);
		}
	}

	@Override
	protected List<ItemStack> getExtraWreckSalvage() {
		// The thief's wreck holds everything it took.
		List<ItemStack> stolen = new ArrayList<>(this.cargo);
		this.cargo.clear();
		return stolen;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 60;
	}

	@Override
	protected float getSoundVolume() {
		// Bumped for the "distant menace" pass (see #6).
		return 1.0f;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("ScanCooldown", this.scanCooldown);
		NbtList cargoList = new NbtList();
		for (ItemStack stack : this.cargo) {
			cargoList.add(stack.encode(this.getRegistryManager()));
		}
		nbt.put("Cargo", cargoList);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.scanCooldown = nbt.getInt("ScanCooldown");
		this.cargo.clear();
		NbtList cargoList = nbt.getList("Cargo", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < cargoList.size(); i++) {
			ItemStack.fromNbt(this.getRegistryManager(), cargoList.getCompound(i))
					.ifPresent(this.cargo::add);
		}
	}
}
