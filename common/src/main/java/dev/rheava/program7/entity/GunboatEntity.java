package dev.rheava.program7.entity;

import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.entity.ai.GunAttackGoal;
import dev.rheava.program7.entity.ai.HitscanImpact;
import dev.rheava.program7.entity.ai.MagazineFed;
import dev.rheava.program7.entity.ai.NavalMoveControl;
import dev.rheava.program7.entity.ai.RoundClass;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
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
	private static final int MAGAZINE_CAPACITY = 48;
	private static final String NBT_ROUNDS = "RoundsRemaining";

	private int roundsRemaining = MAGAZINE_CAPACITY;

	// Secondary point-defense: two light beam turrets (port + starboard) that
	// fend off hostile mobs on a fast cadence. They draw from the ship's own
	// magazine — no free ammo — so a boat that's been fending off a swarm runs
	// its main gun dry too and has to be resupplied. See tickSecondaryTurrets.
	private static final double SECONDARY_RANGE = 16.0;
	private static final int SECONDARY_INTERVAL = 12;
	private static final float SECONDARY_DAMAGE = 5.0f;
	private int secondaryCooldown = 0;

	public GunboatEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new NavalMoveControl(this);
		this.experiencePoints = 30;
	}

	public static DefaultAttributeContainer.Builder createGunboatAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 120.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
				// Long acquisition so it can lock and bombard from well out — a
				// standoff artillery platform, not just a close-in gunboat.
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 120.0)
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

		if (this.getWorld() instanceof ServerWorld serverWorld) {
			this.tickSecondaryTurrets(serverWorld);
		}
	}

	/**
	 * Runs the two beam point-defense turrets: on a fast cadence, finds the
	 * nearest hostile mob in close range and hoses it with a light round from
	 * whichever beam turret (port/starboard) it's on. Hostile mobs are a
	 * different class tree from the Program's own units, so this never targets
	 * friendly drones.
	 */
	private void tickSecondaryTurrets(ServerWorld world) {
		if (this.secondaryCooldown > 0) {
			this.secondaryCooldown--;
			return;
		}
		// Nearest living hostile mob in range (getEntitiesByClass is the proven
		// query pattern in this codebase; hostile mobs are a different class
		// tree from the Program's own units, so friendly drones are never hit).
		HostileEntity mob = null;
		double bestSq = SECONDARY_RANGE * SECONDARY_RANGE;
		for (HostileEntity candidate : world.getEntitiesByClass(HostileEntity.class,
				this.getBoundingBox().expand(SECONDARY_RANGE), LivingEntity::isAlive)) {
			double sq = candidate.squaredDistanceTo(this);
			if (sq < bestSq) {
				bestSq = sq;
				mob = candidate;
			}
		}
		if (mob == null) {
			return;
		}
		// Draw from the ship's magazine — the secondaries aren't free ammo.
		if (!this.consumeRound()) {
			return;
		}
		this.secondaryCooldown = SECONDARY_INTERVAL;

		// Muzzle at the beam turret on the side the mob is on.
		double yawRad = Math.toRadians(this.getYaw());
		Vec3d right = new Vec3d(Math.cos(yawRad), 0.0, Math.sin(yawRad));
		Vec3d toMob = mob.getPos().subtract(this.getPos());
		double side = toMob.x * right.x + toMob.z * right.z;
		Vec3d muzzle = this.getPos().add(right.multiply(side >= 0 ? 3.0 : -3.0)).add(0.0, 2.0, 0.0);
		this.fireSecondary(world, muzzle, mob);
	}

	private void fireSecondary(ServerWorld world, Vec3d muzzle, LivingEntity mob) {
		Vec3d aim = mob.getBoundingBox().getCenter();
		world.spawnParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0.05, 0.05, 0.05, 0.01);
		HitscanImpact.drawTracer(world, muzzle, aim);
		ProgramAcoustics.emit(world, muzzle, P7Sounds.GUN_FIRE.get(), SoundCategory.HOSTILE, 0.8f, 1.3f);
		// Light round: bypass the post-hit invulnerability window with low
		// knockback, same treatment the autogun turret uses.
		mob.timeUntilRegen = 0;
		mob.damage(world.getDamageSources().mobAttack(this), SECONDARY_DAMAGE);
		Vec3d shove = this.getPos().subtract(mob.getPos());
		if (shove.lengthSquared() > 1.0e-4) {
			mob.takeKnockback(RoundClass.LIGHT.knockbackStrength(), shove.x, shove.z);
		}
		HitscanImpact.resolve(world, muzzle, aim, SECONDARY_RANGE, this, RoundClass.LIGHT);
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
	 * The deck gun — both modes fire real shells (no hitscan): a flat, fast
	 * direct shell at targets in sight within {@link #DIRECT_RANGE}, and a
	 * gravity-arced bombardment shell (the same {@link HowitzerShellEntity} the
	 * artillery fires) at anything beyond that or ducked behind terrain, out to
	 * {@link #BOMBARD_RANGE} — several chunks of indirect reach.
	 */
	private static final class DeckGunAttackGoal extends GunAttackGoal {
		/** Flat direct-fire reach; beyond this the gun arcs shells instead. */
		private static final double DIRECT_RANGE = 40.0;
		/** Indirect bombardment reach — arcing shells lobbed over terrain (~7 chunks). */
		private static final double BOMBARD_RANGE = 112.0;
		/** Slow, heavy cadence between bombardment rounds. */
		private static final int BOMBARD_INTERVAL = 80;
		/** Fixed flight time for the arcing ballistic solve (same idea as HowitzerAttackGoal). */
		private static final double FLIGHT_TICKS = 90.0;
		/** Flat direct-fire shell speed, blocks/tick. */
		private static final double DIRECT_SHELL_SPEED = 2.8;
		/** HowitzerShellEntity's gravity — used to compensate drop on a flat shot. */
		private static final double SHELL_GRAVITY = 0.045;

		DeckGunAttackGoal(GunboatEntity shooter) {
			// MEDIUM class, cadence 30 ticks between direct rounds. The shell's
			// own explosion does the damage now, not a hitscan number.
			super(shooter, 1.0, DIRECT_RANGE, 30, 14.0f, RoundClass.MEDIUM);
		}

		@Override
		protected int engage(LivingEntity target, double distance, boolean canSee) {
			// In sight and close: a fast, flat direct shell.
			if (canSee && distance <= DIRECT_RANGE) {
				this.fireDirectShell(target);
				return this.fireInterval;
			}
			// Too far, or the target ducked behind terrain: lob an arcing shell
			// at it (or the last spot it was seen) — indirect bombardment.
			Vec3d aim = canSee ? target.getBoundingBox().getCenter() : this.getLastSeenPos();
			if (aim != null && distance <= BOMBARD_RANGE) {
				this.fireArcingShell(aim);
				return BOMBARD_INTERVAL;
			}
			return 0;
		}

		/** A flat, fast direct-fire shell aimed straight at the target, with drop compensation so it lands on the mark. */
		private void fireDirectShell(LivingEntity target) {
			if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
				return;
			}
			if (this.shooter instanceof MagazineFed magazineFed && !magazineFed.consumeRound()) {
				return;
			}
			Vec3d muzzle = this.shooter.getEyePos().add(0.0, 0.5, 0.0);
			Vec3d aim = target.getBoundingBox().getCenter();
			this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 1.6f, 0.95f);
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 8, 0.25, 0.1, 0.25, 0.03);
			HowitzerShellEntity shell = new HowitzerShellEntity(world, this.shooter);
			shell.setPosition(muzzle.x, muzzle.y, muzzle.z);
			Vec3d delta = aim.subtract(muzzle);
			double dist = Math.max(delta.length(), 1.0e-4);
			double flight = dist / DIRECT_SHELL_SPEED;
			Vec3d dir = delta.multiply(1.0 / dist);
			double vyComp = 0.5 * SHELL_GRAVITY * flight;
			shell.setVelocity(dir.x * DIRECT_SHELL_SPEED, dir.y * DIRECT_SHELL_SPEED + vyComp,
					dir.z * DIRECT_SHELL_SPEED);
			world.spawnEntity(shell);
		}

		/** Launches a gravity-arced shell toward {@code aim} with the same simple ballistic solve the howitzer uses. */
		private void fireArcingShell(Vec3d aim) {
			if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
				return;
			}
			if (this.shooter instanceof MagazineFed magazineFed && !magazineFed.consumeRound()) {
				return;
			}
			Vec3d muzzle = this.shooter.getEyePos().add(0.0, 1.0, 0.0);
			this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 2.0f, 0.7f);
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 10, 0.3, 0.15, 0.3, 0.03);
			HowitzerShellEntity shell = new HowitzerShellEntity(world, this.shooter);
			shell.setPosition(muzzle.x, muzzle.y, muzzle.z);
			double dx = aim.x - muzzle.x;
			double dz = aim.z - muzzle.z;
			shell.setVelocity(dx / FLIGHT_TICKS, 2.1, dz / FLIGHT_TICKS);
			world.spawnEntity(shell);
		}

		@Override
		protected void playFireSound() {
			this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 1.0f, 1.4f);
		}
	}
}
