package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.MagazineFed;
import dev.rheava.program7.entity.ai.MissileAttackGoal;
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
 * Tier 4-5: the doc's "deliberate inversion of unguided rockets — rockets
 * are cheap-and-loose, missiles are dear-and-exact." A mobile launcher
 * carrying only a handful of {@link GuidedMissileEntity}s (far more
 * expensive per shot than any other tube) and a long reload between them.
 * Reserved, per the doc, for high-value targets where the precision is
 * worth the price — this pass fields the launcher and the loop; the
 * anti-mod/anti-boss role it's framed for later is not implemented here.
 */
public class MissileLauncherEntity extends ProgramDroneEntity
		implements ReloadableWeapon, MagazineFed, IndirectFireUnit {
	/** A handful of rounds, not a magazine — each shot is dear. */
	private static final int MAGAZINE_CAPACITY = 4;
	private static final String NBT_ROUNDS = "RoundsRemaining";
	private static final double MIN_RANGE = 32.0;
	/** The one munition family that stays precise at long range (doc §3) — reaches further than the howitzer. */
	private static final double MAX_RANGE = 140.0;

	private int roundsRemaining = MAGAZINE_CAPACITY;

	public MissileLauncherEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 45;
	}

	public static DefaultAttributeContainer.Builder createMissileLauncherAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 120.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.18)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 150.0)
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.5)
				.add(EntityAttributes.GENERIC_ARMOR, 12.0)
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
		this.goalSelector.add(1, new MissileAttackGoal(this));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.5));
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
		// Tight even before homing corrects it — the launch aim barely
		// matters since the missile steers the rest of the way in.
		return 0.3;
	}

	@Override
	public String battery() {
		// Rare and expensive: fires alone, never pools into a mission with anything else.
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
