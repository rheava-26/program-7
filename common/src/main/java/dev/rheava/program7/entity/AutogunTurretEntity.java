package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.GunAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

/**
 * The Tier 1 fixed defense: a twin-barrel autogun bolted onto a pedestal.
 * It never moves — the assembler plants it and it holds that ground,
 * panning idly until a hostile mob (or whoever shot it) enters its arc.
 * Higher rate of fire and tougher plating than the ground drone, paid for
 * with total immobility: break line of sight and it's helpless.
 */
public class AutogunTurretEntity extends ProgramDroneEntity {
	/** Matches the gun's own engagement range — see {@link #initGoals}. */
	private static final double ALARM_DETECTION_RANGE = 20.0;

	public AutogunTurretEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 8;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		// A player walking into range for the first time gets a contact
		// klaxon — bolted-down defenses don't get the sneak-up-quiet
		// treatment the mobile units do.
		this.tickApproachAlarm(ALARM_DETECTION_RANGE, 10);
	}

	public static DefaultAttributeContainer.Builder createAutogunTurretAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
				.add(EntityAttributes.GENERIC_ARMOR, 8.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected boolean isFlier() {
		return false;
	}

	@Override
	protected void initGoals() {
		// fireInterval 12->6: a twin-barrel autogun should hose, not plink —
		// this brings it to ~3.3 rounds/sec instead of ~1.7 (see #1).
		this.goalSelector.add(1, new GunAttackGoal(this, 0.0, 20.0, 6, 3.0f));
		this.goalSelector.add(7, new LookAroundGoal(this));

		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, null));
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

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.ARMORED_VEHICLE;
	}

	@Override
	public boolean isRangedAttacker() {
		return true;
	}
}
