package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.CircleLoiterGoal;
import dev.rheava.program7.entity.ai.InertialFlightMoveControl;
import dev.rheava.program7.entity.ai.UAVSpotGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Air UAV: a fixed-wing spotter slung off a {@link
 * dev.rheava.program7.block.LaunchCatapultBlock}. It never fights — it just
 * circles its launch site at altitude, watches for a player, and paints them
 * for every armed drone in earshot. Shoot it down and the base upstairs goes
 * blind until the catapult can afford to sling another one up.
 */
public class AirUAVEntity extends ProgramDroneEntity {
	/** Below this horizontal speed the airframe would stall rather than glide. */
	private static final double MIN_FORWARD_SPEED = 0.15;
	private static final double STALL_THRUST = 0.15;

	@Nullable
	private BlockPos homePos = null;

	public AirUAVEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new InertialFlightMoveControl(this, 10, true, 2.0f);
		this.experiencePoints = 6;
	}

	public static DefaultAttributeContainer.Builder createAirUAVAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new UAVSpotGoal(this));
		this.goalSelector.add(2, new CircleLoiterGoal(this, 28.0, 20.0, 1.0));
		this.goalSelector.add(3, new LookAroundGoal(this));

		// No target selectors: unarmed spotter. UAVSpotGoal hands contacts off
		// to whatever armed hardware is already in range.
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation navigation = new BirdNavigation(this, world);
		navigation.setCanPathThroughDoors(false);
		navigation.setCanSwim(false);
		navigation.setCanEnterOpenDoors(true);
		return navigation;
	}

	@Nullable
	public BlockPos getHomePos() {
		return this.homePos;
	}

	public void setHomePos(@Nullable BlockPos homePos) {
		this.homePos = homePos;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		// Fixed wings stall rather than hover: while under control (not mid-scramble
		// tumble) and airborne, keep a minimum forward airspeed instead of stopping dead.
		if (!this.getWorld().isClient && !this.isScrambled() && !this.isOnGround()) {
			Vec3d velocity = this.getVelocity();
			if (velocity.horizontalLength() < MIN_FORWARD_SPEED) {
				double yawRad = Math.toRadians(this.getYaw());
				double addX = -Math.sin(yawRad) * STALL_THRUST;
				double addZ = Math.cos(yawRad) * STALL_THRUST;
				this.setVelocity(velocity.add(addX, 0.0, addZ));
			}
		}
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 140;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5f;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (this.homePos != null) {
			nbt.putIntArray("HomePos", new int[] {
					this.homePos.getX(), this.homePos.getY(), this.homePos.getZ()});
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("HomePos")) {
			int[] pos = nbt.getIntArray("HomePos");
			if (pos.length == 3) {
				this.homePos = new BlockPos(pos[0], pos[1], pos[2]);
			}
		}
	}
}
