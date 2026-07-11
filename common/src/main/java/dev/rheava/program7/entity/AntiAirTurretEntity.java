package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.AntiAirAttackGoal;
import dev.rheava.program7.entity.ai.MagazineFed;
import dev.rheava.program7.entity.ai.RoundClass;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Tier 2 fixed anti-air: a flak mount bolted down next to the autogun,
 * tracking anything airborne instead of anything hostile. Like the autogun
 * turret it never moves — the assembler plants it and it holds that ground,
 * bolted, panning idly until something flies into its arc.
 *
 * <p>It cannot depress onto ground targets; that's the counterplay. It only
 * goes after phantoms and players caught elytra-gliding — there's no player
 * drone in the air yet for it to intercept, so ground-bound players are
 * simply invisible to it. Revisit the player target predicate once player
 * drones exist and "airborne" needs a real altitude check instead of just
 * elytra.
 */
public class AntiAirTurretEntity extends ProgramDroneEntity implements ReloadableWeapon, MagazineFed {
	/** Matches the flak mount's own engagement range — see {@link #initGoals}. */
	private static final double ALARM_DETECTION_RANGE = 40.0;
	private static final int MAGAZINE_CAPACITY = 20;
	private static final String NBT_ROUNDS = "RoundsRemaining";

	private int roundsRemaining = MAGAZINE_CAPACITY;

	public AntiAirTurretEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 12;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		// A player walking into range for the first time gets a contact
		// klaxon — bolted-down defenses don't get the sneak-up-quiet
		// treatment the mobile units do.
		this.tickApproachAlarm(ALARM_DETECTION_RANGE, 10);
	}

	public static DefaultAttributeContainer.Builder createAntiAirTurretAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
				.add(EntityAttributes.GENERIC_ARMOR, 6.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected boolean isFlier() {
		return false;
	}

	@Override
	protected void initGoals() {
		// Damage 2.0->6.0 (gun-feel pass), LIGHT caliber like the autogun
		// turret it's bolted beside — turrets stay weaker than a mobile gun
		// of the same barrel size.
		this.goalSelector.add(1, new AntiAirAttackGoal(this, 40.0, 8, 6.0f, RoundClass.LIGHT));
		this.goalSelector.add(7, new LookAroundGoal(this));

		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PhantomEntity.class, true));
		// Only elytra users read as airborne for now — see the class javadoc.
		this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false,
				target -> target instanceof PlayerEntity player && player.isFallFlying()));
	}

	@Override
	public int getRoundsRemaining() {
		return this.roundsRemaining;
	}

	@Override
	public int getMagazineCapacity() {
		return MAGAZINE_CAPACITY;
	}

	@Override
	public void loadRounds(int rounds) {
		this.roundsRemaining = Math.min(MAGAZINE_CAPACITY, this.roundsRemaining + rounds);
	}

	@Override
	public Vec3d getWeaponPos() {
		return this.getPos();
	}

	@Override
	public boolean consumeRound() {
		if (this.roundsRemaining <= 0) {
			return false;
		}
		this.roundsRemaining--;
		return true;
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
	protected ArmorProfile armorProfile() {
		return ArmorProfile.ARMORED_VEHICLE;
	}

	@Override
	public boolean isRangedAttacker() {
		return true;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt(NBT_ROUNDS, this.roundsRemaining);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains(NBT_ROUNDS)) {
			this.roundsRemaining = nbt.getInt(NBT_ROUNDS);
		}
	}
}
