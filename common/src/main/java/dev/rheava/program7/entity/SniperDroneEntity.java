package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.HoverWanderGoal;
import dev.rheava.program7.entity.ai.InertialFlightMoveControl;
import dev.rheava.program7.entity.ai.SniperAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Tier 2 glass cannon: a long-range standoff platform that dies to a stiff
 * breeze. It never wants to be close — it holds range, keeps its accuracy
 * up no matter the distance, and bugs out the instant something gets inside
 * its comfort zone instead of trading hits.
 */
public class SniperDroneEntity extends ProgramDroneEntity {
	public SniperDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new InertialFlightMoveControl(this, 20, true, 1.2f);
		this.experiencePoints = 12;
	}

	public static DefaultAttributeContainer.Builder createSniperDroneAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0)
				// Non-combat goals mirror the surveyor's, so the cruise speed
				// does too — this is a recon-grade airframe, not a fighter.
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
				// Long standoff acquisition range so the sniper can pick a
				// target up well before it's in gun range and start closing
				// on its firing position. Capped near 96 rather than pushed
				// further: entities only tick in loaded chunks (~sim distance,
				// often 8-12 chunks), so going much beyond this risks the
				// sniper sitting in an unticked chunk and freezing in place.
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 96.0)
				.add(EntityAttributes.GENERIC_ARMOR, 0.0);
	}

	@Override
	protected void initGoals() {
		// Engagement range widened to match GENERIC_FOLLOW_RANGE above (96.0)
		// so acquiring a target at long range actually translates into fire,
		// not just tracking — this is the standoff platform's whole point.
		this.goalSelector.add(1, new SniperAttackGoal(this, 1.0, 90.0, 80, 9.0f));
		this.goalSelector.add(3, new HoverWanderGoal(this));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
		this.goalSelector.add(5, new LookAroundGoal(this));

		this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		this.targetSelector.add(2, new RevengeGoal(this));
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
		return 80;
	}

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.LIGHT;
	}

	@Override
	public boolean isRangedAttacker() {
		return true;
	}
}
