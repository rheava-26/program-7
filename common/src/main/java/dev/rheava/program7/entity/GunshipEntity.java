package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.BurstGunAttackGoal;
import dev.rheava.program7.entity.ai.CasBombingGoal;
import dev.rheava.program7.entity.ai.HoverWanderGoal;
import dev.rheava.program7.entity.ai.InertialFlightMoveControl;
import dev.rheava.program7.entity.ai.InvestigateNoiseGoal;
import dev.rheava.program7.entity.ai.MagazineFed;
import dev.rheava.program7.entity.ai.RoundClass;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Tier 4 apex: the house-sized gunship. Per {@code UNITS.md}'s Tier 4 entry
 * and {@code AIR_DOCTRINE.md} §5, this is a fabricated (repurposed
 * extraction-gear) heavy-lift airframe, not clean military hardware — a
 * fat Osprey-derived fuselage on twin ducted rotors, a helicopter tail rotor,
 * and a fluid-traversing belly autocannon.
 *
 * <p>The airframe's <b>presence + direct-fire belly autocannon</b> reuses
 * {@link BurstGunAttackGoal} exactly like {@link HeavyAttackDroneEntity}
 * does, just heavier and reaching further. Close air support — the doctrine
 * doc's bombing run — is now wired through the fire-mission system as
 * {@link CasBombingGoal}: a lower-priority {@code FireMissionManager} client
 * that flies to and bombs a purely indirect designation (an observer's
 * relay the gunship itself has no line of sight on) whenever the cannon has
 * no live target of its own to answer with instead.
 *
 * <p>It is, deliberately, the heaviest thing that flies: its
 * {@link InertialFlightMoveControl} mass is set well above the Tier 3 heavy
 * attack drone's, so it pivots its whole body into a heading change like a
 * ship coming about rather than snapping onto a bearing. And per doctrine
 * (§6, "the apex doesn't run") it never breaks off a losing fight — it
 * simply doesn't override {@link #fleeHealthFraction()}, so the inherited
 * {@code 0.0f} default holds: no {@code RetreatGoal}, fights to the end.
 */
public class GunshipEntity extends ProgramDroneEntity implements ReloadableWeapon, MagazineFed, IndirectFireUnit {
	private static final int MAGAZINE_CAPACITY = 40;
	private static final String NBT_ROUNDS = "RoundsRemaining";

	private int roundsRemaining = MAGAZINE_CAPACITY;

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
		// Damage 5.0->17.0/shot (gun-feel + differentiated-rounds pass): HEAVY
		// caliber, clearly the hardest-hitting round in the arsenal.
		this.goalSelector.add(0, new BurstGunAttackGoal(this, 0.7, 30.0, 6, 45, 17.0f, RoundClass.HEAVY));
		// Same priority as the noise investigator, lower than the cannon: the
		// LOOK/MOVE control conflict with BurstGunAttackGoal means this only
		// actually starts once the cannon has no live target of its own (see
		// class doc / CasBombingGoal's own doc).
		this.goalSelector.add(1, new CasBombingGoal(this));
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
		// Matches the heavy attack drone's cadence so the multi-second rotor loop
		// doesn't stack on itself into a phasing drone; loudness (below) is what
		// carries it far, per AIR_DOCTRINE.md §3/§7 ("hear it first").
		return 45;
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
		// No standoff floor: unlike a tube, it flies to the target itself.
		return 0.0;
	}

	@Override
	public double indirectMaxRange() {
		// Generous — it closes the distance under its own power rather than
		// needing to already be in range like every fixed/lobbing tube.
		return 200.0;
	}

	@Override
	public double indirectBaseSpread() {
		// Between the howitzer's rifle precision and the MLRS's shotgun
		// saturation — a diving-pass release isn't a precision strike.
		return 1.5;
	}

	@Override
	public String battery() {
		// A single aircraft: no battery concept for CAS.
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

	// isFlier() is intentionally not overridden: ProgramDroneEntity's default
	// (true) already applies, exactly like HeavyAttackDroneEntity and
	// ReconHelicopterEntity — neither overrides it either — so rotor-hit
	// quick-kills and the Knockback/Punch scramble both still land on this
	// unit like any other airframe. Mass/armor are what make it tough, not an
	// exemption from the flier rules.
}
