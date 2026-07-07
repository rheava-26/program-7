package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.MortarAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * The Tier 2 fixed indirect fire: a bolted-down tube that lobs shells over
 * cover instead of shooting a straight line. Like the autogun turret it
 * never moves — it needs open sky over the tube to fire at all, so denying
 * it that (a roof, a canopy, a wall built up and over) is real counterplay.
 * The shell's whistle on the way down is the only warning you get.
 */
public class MortarEmplacementEntity extends ProgramDroneEntity {
	public MortarEmplacementEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 14;
	}

	public static DefaultAttributeContainer.Builder createMortarEmplacementAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
				.add(EntityAttributes.GENERIC_ARMOR, 4.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected boolean isFlier() {
		return false;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new MortarAttackGoal(this));
		this.goalSelector.add(7, new LookAroundGoal(this));

		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, null));
		this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	public boolean isPushable() {
		// Bolted down.
		return false;
	}

	@Override
	public void takeKnockback(double strength, double x, double z) {
		// Bolted down: knockback rattles it, nothing more.
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 160;
	}

	@Override
	protected float getSoundVolume() {
		return 0.4f;
	}
}
