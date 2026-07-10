package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.BurstGunAttackGoal;
import dev.rheava.program7.entity.ai.HoverWanderGoal;
import dev.rheava.program7.entity.ai.InertialFlightMoveControl;
import dev.rheava.program7.entity.ai.InvestigateNoiseGoal;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
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
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/**
 * Tier 4 apex: the house-sized gunship. Per {@code UNITS.md}'s Tier 4 entry
 * and {@code AIR_DOCTRINE.md} §5, this is a fabricated (repurposed
 * extraction-gear) heavy-lift airframe, not clean military hardware — a
 * fat Osprey-derived fuselage on twin ducted rotors, a helicopter tail rotor,
 * and a fluid-traversing belly autocannon.
 *
 * <p>This pass is the airframe's <b>presence + direct-fire belly
 * autocannon</b> only, reusing {@link BurstGunAttackGoal} exactly like
 * {@link HeavyAttackDroneEntity} does, just heavier and reaching further.
 * The CAS bombing run described in the doctrine doc is a later pass wired
 * through the fire-mission system — nothing here depends on it existing.
 *
 * <p>It is, deliberately, the heaviest thing that flies: its
 * {@link InertialFlightMoveControl} mass is set well above the Tier 3 heavy
 * attack drone's, so it pivots its whole body into a heading change like a
 * ship coming about rather than snapping onto a bearing. And per doctrine
 * (§6, "the apex doesn't run") it never breaks off a losing fight — it
 * simply doesn't override {@link #fleeHealthFraction()}, so the inherited
 * {@code 0.0f} default holds: no {@code RetreatGoal}, fights to the end.
 */
public class GunshipEntity extends ProgramDroneEntity {
	public GunshipEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		// Mass 8.0 vs. the heavy attack drone's 4.0: half the yaw-rate cap and a
		// noticeably longer spool-up, so every turn telegraphs like the doctrine
		// doc demands ("weighty, never twitchy" — AIR_DOCTRINE.md §6).
		this.moveControl = new InertialFlightMoveControl(this, 12, true, 8.0f);
		this.experiencePoints = 40;
	}

	public static DefaultAttributeContainer.Builder createGunshipAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.4)
				// A standoff apex: seen and heard from far off, per UNITS.md.
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 96.0)
				.add(EntityAttributes.GENERIC_ARMOR, 18.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void initGoals() {
		// No RetreatGoal/priority-0 flee entry here on purpose: the apex fights
		// to the end (see class doc). The belly autocannon out-ranges the heavy
		// attack drone's gun (30 vs. 24) to read as a genuine standoff platform.
		this.goalSelector.add(0, new BurstGunAttackGoal(this, 0.7, 30.0, 6, 45, 5.0f));
		this.goalSelector.add(1, new InvestigateNoiseGoal(this));
		this.goalSelector.add(2, new HoverWanderGoal(this));
		this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f));

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
	protected SoundEvent getAmbientSound() {
		// Reuses the shared rotor-loop sound (dual-rotor downwash reads through
		// it at this unit's volume/cadence); a dedicated gunship signature is a
		// later audio pass, not required for this feel-test.
		return P7Sounds.HELI_ROTOR_LOOP.get();
	}

	@Override
	public int getMinAmbientSoundDelay() {
		// Short delay so the loop restarts often enough to stay audible well
		// before the airframe is in view — per AIR_DOCTRINE.md §3/§7, "hear it
		// first" is the entire point of this unit.
		return 20;
	}

	@Override
	protected float getSoundVolume() {
		// Loud: this has to carry across the "hear it long before you see it"
		// approach distance the doctrine doc calls for.
		return 1.4f;
	}

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.ARMORED_VEHICLE;
	}

	@Override
	public boolean isRangedAttacker() {
		return true;
	}

	// isFlier() is intentionally not overridden: ProgramDroneEntity's default
	// (true) already applies, exactly like HeavyAttackDroneEntity and
	// ReconHelicopterEntity — neither overrides it either — so rotor-hit
	// quick-kills and the Knockback/Punch scramble both still land on this
	// unit like any other airframe. Mass/armor are what make it tough, not an
	// exemption from the flier rules.
}
