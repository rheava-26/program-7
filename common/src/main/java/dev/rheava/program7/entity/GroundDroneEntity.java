package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.GunAttackGoal;
import dev.rheava.program7.entity.ai.PatrolPerimeterGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Tier 1 perimeter unit: a knee-high armored car with a small turreted
 * gun. It exists to keep zombies and skeletons off the Program's hardware —
 * it patrols a ring around the assembler that built it and opens up on any
 * hostile mob that wanders in. It only turns its gun on a player who shoots
 * first.
 */
public class GroundDroneEntity extends ProgramDroneEntity {
	@Nullable
	private BlockPos homePos = null;

	public GroundDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 6;
	}

	public static DefaultAttributeContainer.Builder createGroundDroneAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4)
				// +50% acquisition range so this tier engages a bit sooner;
				// gun range (below) stays tight — this is not a standoff unit.
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 36.0)
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0)
				.add(EntityAttributes.GENERIC_ARMOR, 4.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6);
	}

	@Override
	protected boolean isFlier() {
		return false;
	}

	@Override
	protected void initGoals() {
		// fireInterval 25->10: perimeter pest control still needs to feel
		// like real automatic fire, ~2 rounds/sec instead of 0.8 (see #1).
		this.goalSelector.add(1, new GunAttackGoal(this, 1.0, 14.0, 10, 3.5f));
		this.goalSelector.add(3, new PatrolPerimeterGoal(this, 0.8, 12));
		this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.7));
		this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		this.goalSelector.add(6, new LookAroundGoal(this));

		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, null));
	}

	@Nullable
	public BlockPos getHomePos() {
		return this.homePos;
	}

	public void setHomePos(@Nullable BlockPos homePos) {
		this.homePos = homePos;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 120;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5f;
	}

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.ARMORED_VEHICLE;
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
