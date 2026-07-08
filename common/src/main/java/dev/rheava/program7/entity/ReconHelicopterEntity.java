package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.HoverWanderGoal;
import dev.rheava.program7.entity.ai.InertialFlightMoveControl;
import dev.rheava.program7.entity.ai.OrbitTargetGoal;
import dev.rheava.program7.entity.ai.SearchlightSpotGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

/**
 * The TIER 3 Recon Helicopter: a fast rotary spotter that, unlike the
 * catapult-launched {@link AirUAVEntity} lazily circling its base, actively
 * hunts. It flies out, finds you, orbits you above head height with a
 * searchlight pinned on your position, and paints you for every armed drone
 * in range.
 *
 * <p>It carries no weapon of its own — the light is the threat. Once it
 * holds the beam on you long enough, every idle gun in earshot swings your
 * way. Shoot it down before the beam settles, or bring something that
 * reaches: it orbits eight blocks up, out of easy melee, and a
 * Knockback/Punch hit scrambles it into a crash same as any other flier.
 */
public class ReconHelicopterEntity extends ProgramDroneEntity {

	public ReconHelicopterEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new InertialFlightMoveControl(this, 20, true, 1.5f);
		this.experiencePoints = 15;
	}

	public static DefaultAttributeContainer.Builder createReconHelicopterAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.7)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new SearchlightSpotGoal(this));
		this.goalSelector.add(2, new OrbitTargetGoal(this, 16.0, 1.0));
		this.goalSelector.add(3, new HoverWanderGoal(this));
		this.goalSelector.add(4, new LookAroundGoal(this));

		// Unarmed: the only target-selector entry is revenge, so shooting the
		// helicopter locks its searchlight (and every gun it calls in) onto
		// the shooter. The rest of the time SearchlightSpotGoal fills the
		// target slot itself by hunting down the nearest player it can see.
		this.targetSelector.add(1, new RevengeGoal(this));
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
	protected float getSoundVolume() {
		return 0.6f;
	}

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.LIGHT;
	}
}
