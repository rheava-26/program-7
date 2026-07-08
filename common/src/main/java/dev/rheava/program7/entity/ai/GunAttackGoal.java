package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * The Program's small-arms fire: a hitscan burst with a visible tracer line.
 * Shared by anything that mounts a gun — the ground drone paths into range
 * first, the autogun turret just tracks and shoots.
 *
 * <p>Accuracy falls off with distance, so keeping range on a shooter is real
 * counterplay even before cover comes into it.
 */
public class GunAttackGoal extends Goal {
	/**
	 * How long, in ticks, a shooter keeps hosing a target's last-known
	 * position after losing line of sight, before it actually gives up.
	 * Ducking behind cover should feel like getting pinned, not an instant
	 * safe button.
	 */
	protected static final int SUPPRESSION_WINDOW_TICKS = 50;
	/** Accuracy multiplier applied while firing blind during a suppression window. */
	protected static final double SUPPRESSION_HIT_CHANCE_SCALE = 0.15;
	/** Miss offsets shorter than this (out of a max of ~1.9) read as a close graze. */
	private static final double NEAR_MISS_THRESHOLD = 1.0;
	/** Minimum gap between whistle cues so a string of misses doesn't spam it. */
	private static final int WHISTLE_COOLDOWN_TICKS = 30;

	// shooter/range/damage are protected: subclasses (the sniper's standoff
	// variant) build their own movement and accuracy rules on top of them.
	protected final ProgramDroneEntity shooter;
	/** Movement speed while closing in; 0 for stationary mounts. */
	private final double speed;
	protected final double range;
	private final int fireInterval;
	protected final float damage;
	private int cooldown;

	/** Target's last-seen position, kept warm for the suppression-fire window below. */
	@Nullable
	private Vec3d lastSeenPos;
	/** Ticks left before suppression fire gives up once line of sight is lost. */
	private int suppressionTicksLeft;
	private int whistleCooldown;

	public GunAttackGoal(ProgramDroneEntity shooter, double speed, double range,
			int fireInterval, float damage) {
		this.shooter = shooter;
		this.speed = speed;
		this.range = range;
		this.fireInterval = fireInterval;
		this.damage = damage;
		this.setControls(this.speed > 0
				? EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK)
				: EnumSet.of(Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity target = this.shooter.getTarget();
		return target != null && target.isAlive() && !this.shooter.isScrambled();
	}

	@Override
	public boolean shouldContinue() {
		return this.canStart();
	}

	@Override
	public void start() {
		// Acquisition delay: the mount swings around before the first shot.
		this.cooldown = 10;
		this.suppressionTicksLeft = 0;
		this.whistleCooldown = 0;
	}

	@Override
	public void stop() {
		if (this.speed > 0) {
			this.shooter.getNavigation().stop();
		}
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = this.shooter.getTarget();
		if (target == null) {
			return;
		}
		this.shooter.getLookControl().lookAt(target, 30.0f, 30.0f);

		double distance = this.shooter.distanceTo(target);
		boolean canSee = this.shooter.getVisibilityCache().canSee(target);
		boolean sighted = this.updateSight(target, canSee);
		if (this.speed > 0) {
			if (distance > this.range * 0.7 || !canSee) {
				this.shooter.getNavigation().startMovingTo(target, this.speed);
			} else {
				this.shooter.getNavigation().stop();
			}
		}

		if (this.cooldown > 0) {
			this.cooldown--;
		} else if (distance <= this.range && sighted) {
			if (canSee) {
				this.fire(target, distance);
			} else {
				// Suppression window: still hosing the last-seen spot, blind.
				this.fire(target, distance, this.lastSeenPos, SUPPRESSION_HIT_CHANCE_SCALE);
			}
			this.cooldown = this.fireInterval;
		}
	}

	/**
	 * Refreshes {@link #lastSeenPos} and the suppression clock. Returns
	 * whether the shooter should still be treated as "on target" this tick —
	 * either it can actually see the target, or it's within the grace window
	 * of losing sight and is still firing blind at the last-known spot.
	 * Shared by every subclass's {@code tick()} so the suppression behavior
	 * doesn't have to be reimplemented alongside each one's own movement
	 * rules.
	 */
	protected boolean updateSight(LivingEntity target, boolean canSee) {
		if (canSee) {
			this.lastSeenPos = target.getBoundingBox().getCenter();
			this.suppressionTicksLeft = SUPPRESSION_WINDOW_TICKS;
		} else if (this.suppressionTicksLeft > 0) {
			this.suppressionTicksLeft--;
		}
		if (this.whistleCooldown > 0) {
			this.whistleCooldown--;
		}
		return canSee || (this.suppressionTicksLeft > 0 && this.lastSeenPos != null);
	}

	/** The last position the target was actually seen at; only meaningful once {@link #updateSight} has run. */
	@Nullable
	protected Vec3d getLastSeenPos() {
		return this.lastSeenPos;
	}

	// Overridable, along with the hitChance()/playFireSound() hooks below,
	// so a mount with a different gun or accuracy model doesn't have to
	// duplicate the tracer and particle work.
	protected void fire(LivingEntity target, double distance) {
		this.fire(target, distance, target.getBoundingBox().getCenter(), 1.0);
	}

	/**
	 * The actual shot, aimed at {@code aimCenter} rather than always the
	 * target's live position — during suppression fire that's the
	 * last-seen spot instead, scaled down by {@code hitChanceScale} so blind
	 * fire mostly just keeps a target's head down instead of landing hits.
	 */
	protected void fire(LivingEntity target, double distance, Vec3d aimCenter, double hitChanceScale) {
		if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
			return;
		}
		Vec3d muzzle = this.shooter.getEyePos();
		Vec3d aim = aimCenter;

		// Accuracy degrades with range; a miss still draws a tracer past you.
		boolean hit = this.shooter.getRandom().nextDouble() < this.hitChance(distance) * hitChanceScale;
		Vec3d missOffset = Vec3d.ZERO;
		if (!hit) {
			missOffset = new Vec3d((this.shooter.getRandom().nextDouble() - 0.5) * 2.4,
					(this.shooter.getRandom().nextDouble() - 0.5) * 1.6,
					(this.shooter.getRandom().nextDouble() - 0.5) * 2.4);
			aim = aim.add(missOffset);
		}

		// Muzzle flash + tracer line, vanilla particles per the VFX direction.
		world.spawnParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 2, 0.05, 0.05, 0.05, 0.01);
		Vec3d step = aim.subtract(muzzle);
		int steps = Math.max(2, (int) (step.length() / 0.8));
		step = step.multiply(1.0 / steps);
		Vec3d point = muzzle;
		for (int i = 0; i < steps; i++) {
			point = point.add(step);
			world.spawnParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}

		this.playFireSound();
		if (hit) {
			target.damage(this.shooter.getDamageSources().mobAttack(this.shooter), this.damage);
		} else if (this.whistleCooldown <= 0 && missOffset.length() < NEAR_MISS_THRESHOLD) {
			// Close graze: a whistle/crack right past the target's ears.
			target.playSound(P7Sounds.GUN_WHISTLE.get(), 0.4f,
					0.9f + this.shooter.getRandom().nextFloat() * 0.2f);
			this.whistleCooldown = WHISTLE_COOLDOWN_TICKS;
		}
	}

	/** Chance of a hit at the given distance. Falls off linearly with range. */
	protected double hitChance(double distance) {
		return 0.9 - (distance / this.range) * 0.35;
	}

	/** The report itself; broken out so mounts with a different gun can swap it. */
	protected void playFireSound() {
		this.shooter.playSound(P7Sounds.GUN_FIRE.get(), 1.0f,
				1.1f + this.shooter.getRandom().nextFloat() * 0.2f);
	}
}
