package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.HoverWanderGoal;
import dev.rheava.program7.entity.ai.InertialFlightMoveControl;
import dev.rheava.program7.entity.ai.InvestigateDisturbanceGoal;
import dev.rheava.program7.entity.ai.OrbitTargetGoal;
import dev.rheava.program7.entity.ai.SearchlightSpotGoal;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.sound.SoundEvent;
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
		// Mass 3.0: heavier hull, slower spool-up and turn than the tier-2 fliers.
		this.moveControl = new InertialFlightMoveControl(this, 20, true, 3.0f);
		this.experiencePoints = 15;
	}

	public static DefaultAttributeContainer.Builder createReconHelicopterAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.46)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.92)
				// Raised for symmetric "if you can see it, it can see you"
				// LOS-gated perception (InvestigateDisturbanceGoal). 160 is the
				// practical ceiling for this attribute; true whole-region
				// (~600 block) symmetry needs entity simulation distance / a
				// future virtualization layer — a known limit, not fixed here.
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 160.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new SearchlightSpotGoal(this));
		this.goalSelector.add(2, new InvestigateDisturbanceGoal(this));
		this.goalSelector.add(3, new OrbitTargetGoal(this, 16.0, 1.0));
		this.goalSelector.add(4, new HoverWanderGoal(this));
		this.goalSelector.add(5, new LookAroundGoal(this));

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
	protected SoundEvent getAmbientSound() {
		return P7Sounds.HELI_ROTOR_LOOP.get();
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 45;
	}

	@Override
	protected float getSoundVolume() {
		return 1.1f;
	}

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.LIGHT;
	}
}
