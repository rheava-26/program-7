package dev.rheava.program7.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.rheava.program7.entity.ai.InertialFlightMoveControl;
import dev.rheava.program7.entity.ai.SupplyRunGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Transport Drone — a heavy flying courier hauling two crates' worth of
 * the Director's payment out to an assembler mid-build. Slower and tougher
 * than the Logistics Drone, but it has no fight in it either: shoot it down
 * mid-run and its cargo spills out as salvage, the resources gone before the
 * assembler ever saw them.
 */
public class TransportDroneEntity extends ProgramDroneEntity implements CourierUnit {
	@Nullable
	private BlockPos destination = null;
	private String job = "";
	private final Map<String, Integer> cargo = new HashMap<>();

	public TransportDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new InertialFlightMoveControl(this, 20, true, 3.0f);
		this.experiencePoints = 6;
	}

	public static DefaultAttributeContainer.Builder createTransportDroneAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.4)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
				.add(EntityAttributes.GENERIC_ARMOR, 2.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new SupplyRunGoal(this));
		this.goalSelector.add(2, new LookAroundGoal(this));
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation navigation = new BirdNavigation(this, world);
		navigation.setCanPathThroughDoors(false);
		navigation.setCanSwim(false);
		navigation.setCanEnterOpenDoors(true);
		return navigation;
	}

	/** Loads the cargo and points the drone at the assembler waiting for it. */
	public void beginMission(BlockPos destination, String job, Map<String, Integer> cargo) {
		this.destination = destination;
		this.job = job;
		this.cargo.clear();
		this.cargo.putAll(cargo);
	}

	@Nullable
	@Override
	public BlockPos getDestination() {
		return this.destination;
	}

	@Override
	public String getJob() {
		return this.job;
	}

	@Override
	public Map<String, Integer> getCargo() {
		return this.cargo;
	}

	@Override
	public void clearMission() {
		this.destination = null;
		this.job = "";
		this.cargo.clear();
	}

	@Override
	protected List<ItemStack> getExtraWreckSalvage() {
		// This is the whole point: shoot the courier down and take the payment.
		return CourierUnit.cargoToItems(this.cargo);
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
	protected ArmorProfile armorProfile() {
		return ArmorProfile.LIGHT;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (this.destination != null) {
			nbt.putIntArray("Destination", new int[] {
					this.destination.getX(), this.destination.getY(), this.destination.getZ()});
		}
		nbt.putString("Job", this.job);
		NbtCompound cargoTag = new NbtCompound();
		this.cargo.forEach(cargoTag::putInt);
		nbt.put("Cargo", cargoTag);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("Destination")) {
			int[] pos = nbt.getIntArray("Destination");
			if (pos.length == 3) {
				this.destination = new BlockPos(pos[0], pos[1], pos[2]);
			}
		}
		this.job = nbt.getString("Job");
		this.cargo.clear();
		NbtCompound cargoTag = nbt.getCompound("Cargo");
		for (String key : cargoTag.getKeys()) {
			this.cargo.put(key, cargoTag.getInt(key));
		}
	}
}
