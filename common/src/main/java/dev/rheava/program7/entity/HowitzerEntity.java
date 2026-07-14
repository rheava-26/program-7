package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.HowitzerAttackGoal;
import dev.rheava.program7.entity.ai.MagazineFed;
import dev.rheava.program7.registry.P7Sounds;
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
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Tier 3 mobile artillery: a tracked self-propelled howitzer modeled on the
 * M109 Paladin — boxy hull, big boxy turret, long barrel projecting up and
 * forward. Where the mortar emplacement ({@link MortarEmplacementEntity}) is
 * bolted down and short-ranged, this is a standoff platform per the artillery
 * doc's "wall-breaker" role: it drops heavy arcing shells from well outside
 * the fight and mostly just holds ground doing it, taking only a slow
 * reposition between salvos rather than chasing anything down.
 *
 * <p>A client of the Director-side {@code FireMissionManager} (see {@link
 * dev.rheava.program7.entity.ai.HowitzerAttackGoal}): self-observed when it
 * has its own line-of-sight target, otherwise firing on whatever mission the
 * manager assigns — an observer relay, counter-battery, a hot dwell cell, or
 * a shared battery mission alongside another howitzer nearby.
 */
public class HowitzerEntity extends ProgramDroneEntity implements ReloadableWeapon, MagazineFed, IndirectFireUnit {
	private static final int MAGAZINE_CAPACITY = 8;
	private static final String NBT_ROUNDS = "RoundsRemaining";
	/** Mirrors {@link HowitzerAttackGoal}'s own standoff window — the single source of truth for {@link dev.rheava.program7.director.FireMissionManager}. */
	private static final double MIN_RANGE = 24.0;
	private static final double MAX_RANGE = 112.0;

	private int roundsRemaining = MAGAZINE_CAPACITY;

	public HowitzerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 35;
	}

	public static DefaultAttributeContainer.Builder createHowitzerAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 140.0)
				// Slow tracked artillery, not a line vehicle — it isn't built to
				// close distance, just to waddle between firing positions.
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.16)
				// Big standoff acquisition range so it can lock a target well
				// outside its own gun range and start walking fire onto it —
				// sits just past the gun's ~112-block reach (see HowitzerAttackGoal).
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 120.0)
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.5)
				.add(EntityAttributes.GENERIC_ARMOR, 16.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected boolean isFlier() {
		return false;
	}

	@Override
	public boolean isRangedAttacker() {
		return true;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new HowitzerAttackGoal(this));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.5));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		this.goalSelector.add(5, new LookAroundGoal(this));

		// Players first, then whatever else is hostile — same doctrine as the
		// IFV: this is a war machine answering a real threat.
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		this.targetSelector.add(3, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, null));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return P7Sounds.TANK_TRACKS_LOOP.get();
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 50;
	}

	@Override
	protected float getSoundVolume() {
		return 1.1f;
	}

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.ARMORED_VEHICLE;
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
	public double indirectMinRange() {
		return MIN_RANGE;
	}

	@Override
	public double indirectMaxRange() {
		return MAX_RANGE;
	}

	@Override
	public double indirectBaseSpread() {
		// The howitzer is the doc's baseline "rifle" precision — every other
		// type's spread is tuned relative to this one.
		return 1.0;
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
