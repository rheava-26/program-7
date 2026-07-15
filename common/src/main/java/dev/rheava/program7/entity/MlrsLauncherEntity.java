package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.MagazineFed;
import dev.rheava.program7.entity.ai.MlrsAttackGoal;
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
 * Tier 3-4 mobile rocket artillery: the doc's "cheap, inaccurate end of the
 * munition axis" — a wheeled/tracked launch vehicle that ripples several
 * unguided rockets at once rather than lobbing one precise shell. Where the
 * howitzer is a rifle, this is a shotgun: wide {@link #indirectBaseSpread}
 * for saturation over precision, long reload between salvos, and (per the
 * platform-split doc) a mobile piece that "usually fires alone" — {@link
 * #battery()} opts out of the howitzer/mortar's battery-sharing so it
 * doesn't accidentally pool with a nearby tube of a different family.
 *
 * <p>A {@link dev.rheava.program7.director.FireMissionManager} client from
 * day one (see {@link MlrsAttackGoal}), same self-observed/assigned split as
 * the howitzer and mortar.
 */
public class MlrsLauncherEntity extends ProgramDroneEntity implements ReloadableWeapon, MagazineFed, IndirectFireUnit {
	/** Four salvos' worth before it has to run back to a depot — mobile pieces carry less on hand than an emplaced battery (doc §6a). */
	private static final int MAGAZINE_CAPACITY = 24;
	private static final String NBT_ROUNDS = "RoundsRemaining";
	private static final double MIN_RANGE = 16.0;
	private static final double MAX_RANGE = 72.0;

	private int roundsRemaining = MAGAZINE_CAPACITY;

	public MlrsLauncherEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 32;
	}

	public static DefaultAttributeContainer.Builder createMlrsLauncherAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 110.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 88.0)
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.5)
				.add(EntityAttributes.GENERIC_ARMOR, 10.0)
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
		this.goalSelector.add(1, new MlrsAttackGoal(this));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.6));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		this.goalSelector.add(5, new LookAroundGoal(this));

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
		return 1.0f;
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
		// A shotgun, not a rifle — several times the howitzer's baseline
		// spread; the ripple's saturation is the point, not any one round.
		return 3.0;
	}

	@Override
	public String battery() {
		// Mobile piece: fires alone rather than as a massed battery (doc §4
		// platform split), so it never pools with another launcher parked nearby.
		return null;
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
