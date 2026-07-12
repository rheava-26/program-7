package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.GunAttackGoal;
import dev.rheava.program7.entity.ai.MagazineFed;
import dev.rheava.program7.entity.ai.NavalMoveControl;
import dev.rheava.program7.entity.ai.RoundClass;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimAroundGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SwimNavigation;
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
 * Tier 3: the biggest, slowest, toughest thing the Program puts on the
 * board. A gunboat doesn't come to you — it holds a stretch of water and
 * makes crossing it expensive, its deck gun putting down a single
 * heavy-caliber round every couple of seconds rather than the small arms'
 * steady hose of fire. Nothing else the Program fields soaks up this much
 * damage before it goes down.
 *
 * <p>That toughness comes with a hard leash: this is a hull, not a walker.
 * It cannot leave the water under its own power, and if it ends up beached
 * — driven aground, or a lake drained out from under it — the engines have
 * nothing to push against. It sits there, guns still tracking, taking hits
 * from anything willing to close the distance. The harbor that actually
 * builds these arrives later with the rest of the Program's
 * support-infrastructure phase; for now the gunboat is deployed the same
 * way everything else is.
 */
public class GunboatEntity extends ProgramDroneEntity implements ReloadableWeapon, MagazineFed {
	// Buoyancy: how hard the hull shoves itself back toward the surface per
	// tick once it's been pushed under.
	private static final double BUOYANCY_RISE = 0.08;
	// How much of its vertical speed the hull sheds per tick while riding the
	// surface — a slow bob settling out, not a cork springing back up.
	private static final double SURFACE_VERTICAL_DAMPING = 0.5;
	private static final int MAGAZINE_CAPACITY = 30;
	private static final String NBT_ROUNDS = "RoundsRemaining";

	private int roundsRemaining = MAGAZINE_CAPACITY;

	public GunboatEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new NavalMoveControl(this);
		this.experiencePoints = 30;
	}

	public static DefaultAttributeContainer.Builder createGunboatAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 120.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
				.add(EntityAttributes.GENERIC_ARMOR, 16.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
				// No traction on dry land anyway, so climbing a step is moot;
				// zeroing this out means it never even pretends to.
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 0.0);
	}

	@Override
	protected boolean isFlier() {
		return false;
	}

	/**
	 * Solid deck: other entities collide with the gunboat's bounding box
	 * instead of passing through it, so a player can climb up and stand on it
	 * like a moving platform. Note the hitbox is a single square AABB, so this
	 * is the central deck footprint, not the full bow-to-stern length — true
	 * end-to-end walkability would need a multipart collision setup.
	 */
	@Override
	public boolean isCollidable() {
		return true;
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		SwimNavigation navigation = new SwimNavigation(this, world);
		// SwimNavigation's own setCanSwim is a no-op — its node maker already
		// refuses to path anywhere that isn't fluid, which is exactly the
		// "never routes onto land" guarantee this unit needs.
		navigation.setCanSwim(true);
		return navigation;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new DeckGunAttackGoal(this));
		this.goalSelector.add(4, new SwimAroundGoal(this, 1.0, 60));
		this.goalSelector.add(5, new LookAroundGoal(this));

		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		this.targetSelector.add(3, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, null));
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (this.getWorld().isClient) {
			return;
		}

		if (this.isTouchingWater() && !this.isScrambled()) {
			Vec3d velocity = this.getVelocity();
			if (this.isSubmergedInWater()) {
				// Shoved under — by a wave, a blast, whatever — the hull is
				// buoyant, not a submarine, so it noses back up on its own.
				this.setVelocity(velocity.x, velocity.y + BUOYANCY_RISE, velocity.z);
			} else {
				// Riding the surface: bleed the vertical component toward
				// zero instead of letting it spring back, so it settles into
				// a slow, heavy bob rather than bouncing like a cork.
				this.setVelocity(velocity.x, velocity.y * SURFACE_VERTICAL_DAMPING, velocity.z);
			}
		}

		if (!this.isTouchingWater() && this.isOnGround()) {
			// Beached. Kill the navigation every tick rather than let it sit
			// idle with a stale path — the goals will keep trying to swim
			// somewhere and keep failing, which is the point: a beached
			// gunboat is a helpless, visibly struggling target until
			// something puts it back in the water.
			this.getNavigation().stop();
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return P7Sounds.BOAT_ENGINE_LOOP.get();
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 45;
	}

	@Override
	protected float getSoundVolume() {
		return 1.2f;
	}

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.HEAVY_HULL;
	}

	@Override
	protected int getNextAirUnderwater(int air) {
		// Machines don't breathe. canBreatheInWater() is final in 1.21.1 and
		// keyed off a data-driven entity type tag this class has no business
		// touching, so the actual override point is here: air never spends
		// down, so the drowning damage in LivingEntity#baseTick never fires.
		return this.getMaxAir();
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

	/**
	 * The deck gun: {@link GunAttackGoal}'s hitscan-with-tracer model works
	 * fine unchanged for a slow-swimming shooter, so this only swaps the
	 * report for something that sounds like it's firing a real gun instead
	 * of small arms.
	 */
	private static final class DeckGunAttackGoal extends GunAttackGoal {
		DeckGunAttackGoal(GunboatEntity shooter) {
			// Damage 6.0->14.0 (gun-feel pass): a single heavy-caliber round,
			// MEDIUM class alongside the IFV's autocannon (differentiated-
			// rounds pass) — clearly harder-hitting than either turret's LIGHT
			// rounds, clearly a rung below the gunship's HEAVY belly gun.
			super(shooter, 1.0, 28.0, 30, 14.0f, RoundClass.MEDIUM);
		}

		@Override
		protected void playFireSound() {
			this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 1.0f, 1.4f);
		}
	}
}
