package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.GunAttackGoal;
import dev.rheava.program7.entity.ai.MagazineFed;
import dev.rheava.program7.entity.ai.RoundClass;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Tier 1 fixed defense: a twin-barrel autogun bolted onto a pedestal.
 * It never moves — the assembler plants it and it holds that ground,
 * panning idly until a hostile mob (or whoever shot it) enters its arc.
 * Higher rate of fire and tougher plating than the ground drone, paid for
 * with total immobility: break line of sight and it's helpless.
 *
 * <p>Per-shot damage is deliberately lower than a mobile gun drone of the
 * same {@link RoundClass#LIGHT} caliber (see {@link #initGoals}) — a static
 * turret trades punch for uptime, the mobile guns trade uptime for punch.
 * Carries its own finite {@link ReloadableWeapon} magazine (see
 * {@link #getRoundsRemaining()}), separate from the block-entity version of
 * this turret ({@code AutogunTurretBlockEntity}) which is the one the
 * assembler actually places.
 */
public class AutogunTurretEntity extends ProgramDroneEntity implements ReloadableWeapon, MagazineFed {
	/** Matches the gun's own engagement range — see {@link #initGoals}. */
	private static final double ALARM_DETECTION_RANGE = 20.0;
	private static final int MAGAZINE_CAPACITY = 20;
	private static final String NBT_ROUNDS = "RoundsRemaining";

	private int roundsRemaining = MAGAZINE_CAPACITY;

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
		// this brings it to ~3.3 rounds/sec instead of ~1.7 (see #1). Damage
		// 3.0->7.0 (gun-feel pass: ~3 hits kills a 20 HP hostile) but kept
		// below the mobile ground drone's 10.0 for the same LIGHT caliber —
		// turrets are weaker than a mobile gun of the same barrel size.
		this.goalSelector.add(1, new GunAttackGoal(this, 0.0, 20.0, 6, 7.0f, RoundClass.LIGHT));
		this.goalSelector.add(7, new LookAroundGoal(this));

		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, null));
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
