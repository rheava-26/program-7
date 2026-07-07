package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.GunAttackGoal;
import dev.rheava.program7.entity.ai.HoverWanderGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Tier 2 workhorse: where the attack drone is a one-shot warhead, this one
 * just keeps strafing — closing to gun range and hosing its target down
 * until one of them stops moving.
 */
public class MediumAttackDroneEntity extends ProgramDroneEntity {
	public MediumAttackDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new FlightMoveControl(this, 20, true);
		this.experiencePoints = 10;
	}

	public static DefaultAttributeContainer.Builder createMediumAttackDroneAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.9)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
				.add(EntityAttributes.GENERIC_ARMOR, 2.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new GunAttackGoal(this, 1.0, 16.0, 20, 3.0f));
		this.goalSelector.add(3, new HoverWanderGoal(this));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f));

		this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, null));
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
	public int getMinAmbientSoundDelay() {
		return 60;
	}
}
