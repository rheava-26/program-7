package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.entity.ReloadableWeapon;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
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
	/** Miss offsets shorter than this (out of a max of ~1.9, before spread scaling) read as a close graze. */
	private static final double NEAR_MISS_THRESHOLD = 1.0;
	/** Minimum gap between whistle cues so a string of misses doesn't spam it. */
	private static final int WHISTLE_COOLDOWN_TICKS = 30;
	/**
	 * Beyond this fraction of a mount's normal range, even a visible target
	 * stops getting aimed shots and starts getting suppressive fire instead —
	 * the whole point of long range is pressure, not precision.
	 */
	private static final double LONG_RANGE_SUPPRESSION_FRACTION = 0.6;
	/**
	 * How far out, relative to the mount's normal range, the suppression
	 * envelope reaches — both for chasing a lost target's last-seen spot and
	 * for engaging a visible-but-distant one. Suppressive rounds are cheap
	 * and inaccurate, so it's fine for them to reach further than an aimed
	 * shot would.
	 */
	private static final double SUPPRESSION_RANGE_MULTIPLIER = 1.6;
	/** Miss-offset spread multiplier applied to suppressive fire — wide on purpose. */
	private static final double SUPPRESSIVE_SPREAD_SCALE = 1.8;
	/** Downward bias folded into a suppressive miss so rounds tend to walk into the ground/cover near a target's feet rather than sail overhead. */
	private static final double SUPPRESSIVE_GROUND_BIAS = 0.6;
	/** Cadence multiplier while suppressing: rounds go out faster, not more accurately. */
	private static final double SUPPRESSIVE_FIRE_INTERVAL_SCALE = 0.5;
	/** Minimum gap between "magazine's empty" clicks so a shooter stuck dry doesn't spam it every tick. */
	private static final int DRY_FIRE_CLICK_INTERVAL_TICKS = 40;

	// shooter/range/damage/fireInterval are protected: subclasses (the
	// sniper's standoff variant) build their own movement, accuracy, and
	// engagement rules on top of them.
	protected final ProgramDroneEntity shooter;
	/** Movement speed while closing in; 0 for stationary mounts. */
	private final double speed;
	protected final double range;
	protected final int fireInterval;
	protected final float damage;
	/** How big this weapon's rounds are — see {@link RoundClass} — for knockback/particle/terrain-chip scaling. */
	protected final RoundClass roundClass;
	private int cooldown;

	/** Target's last-seen position, kept warm for the suppression-fire window below. */
	@Nullable
	private Vec3d lastSeenPos;
	/** Ticks left before suppression fire gives up once line of sight is lost. */
	private int suppressionTicksLeft;
	private int whistleCooldown;
	/** Ticks left before another dry-fire click is allowed while the magazine's empty. */
	private int dryFireCooldown;

	public GunAttackGoal(ProgramDroneEntity shooter, double speed, double range,
			int fireInterval, float damage, RoundClass roundClass) {
		this.shooter = shooter;
		this.speed = speed;
		this.range = range;
		this.fireInterval = fireInterval;
		this.damage = damage;
		this.roundClass = roundClass;
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
		this.dryFireCooldown = 0;
		this.playOpeningFireCue();
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

		if (this.isOutOfAmmo()) {
			// Dry: hold position (a closing-in mount stops advancing on a
			// target it can't actually shoot at) and click occasionally
			// instead of firing, rather than silently doing nothing.
			if (this.speed > 0) {
				this.shooter.getNavigation().stop();
			}
			this.tickDryFireClick();
			return;
		}

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
		} else if (sighted) {
			int nextCooldown = this.engage(target, distance, canSee);
			if (nextCooldown > 0) {
				this.cooldown = nextCooldown;
			}
		}
	}

	/** Whether this weapon has a finite magazine (see {@link ReloadableWeapon}) that's currently run dry. */
	protected boolean isOutOfAmmo() {
		return this.shooter instanceof ReloadableWeapon reloadable && reloadable.needsReload();
	}

	/** Plays the empty-magazine click on a cooldown so a shooter stuck dry doesn't spam it every tick. */
	protected void tickDryFireClick() {
		if (this.dryFireCooldown > 0) {
			this.dryFireCooldown--;
			return;
		}
		this.dryFireCooldown = DRY_FIRE_CLICK_INTERVAL_TICKS;
		this.shooter.playSound(P7Sounds.WEAPON_DRY_FIRE.get(), 0.5f,
				1.2f + this.shooter.getRandom().nextFloat() * 0.15f);
	}

	/**
	 * Decides how (and whether) to shoot this tick, and returns the cooldown
	 * to apply if a round went out (0 if nothing fired, leaving the caller's
	 * cooldown alone so it retries next tick). Split out from {@link #tick()}
	 * so a mount with different engagement rules — the sniper wants to stay
	 * precise no matter the range — can override just this decision.
	 *
	 * <p>Close/medium range with eyes on target gets an aimed, accurate shot.
	 * Everything else — a visible target out past {@link
	 * #LONG_RANGE_SUPPRESSION_FRACTION} of normal range, or a lost target
	 * still within the extended {@link #SUPPRESSION_RANGE_MULTIPLIER}
	 * envelope — gets loud, fast, wide-spread suppressive fire walked toward
	 * the target's current or last-known position instead: pressure, not
	 * precision.
	 */
	protected int engage(LivingEntity target, double distance, boolean canSee) {
		boolean longRange = distance > this.range * LONG_RANGE_SUPPRESSION_FRACTION;
		if (canSee && !longRange) {
			this.fire(target, distance);
			return this.fireInterval;
		}
		if (distance <= this.range * SUPPRESSION_RANGE_MULTIPLIER) {
			Vec3d aimPos = canSee ? target.getBoundingBox().getCenter() : this.lastSeenPos;
			if (aimPos != null) {
				this.fire(target, distance, aimPos, SUPPRESSION_HIT_CHANCE_SCALE, true);
				return Math.max(1, (int) (this.fireInterval * SUPPRESSIVE_FIRE_INTERVAL_SCALE));
			}
		}
		return 0;
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
	 * Precise, non-suppressive shot; see the {@code suppressive} overload
	 * below for the wide-spread variant.
	 */
	protected void fire(LivingEntity target, double distance, Vec3d aimCenter, double hitChanceScale) {
		this.fire(target, distance, aimCenter, hitChanceScale, false);
	}

	/**
	 * The actual shot. When {@code suppressive} is true this is deliberately
	 * a bad shot: wider spread (scaled by {@link #SUPPRESSIVE_SPREAD_SCALE})
	 * biased downward (see {@link #SUPPRESSIVE_GROUND_BIAS}) so rounds tend
	 * to walk into the ground/cover around a target's feet instead of
	 * sailing past overhead — loud pressure, not a kill shot. Either way, the
	 * round lands somewhere: {@link HitscanImpact} chews up whatever block or
	 * fluid actually catches it.
	 */
	protected void fire(LivingEntity target, double distance, Vec3d aimCenter, double hitChanceScale,
			boolean suppressive) {
		if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
			return;
		}
		if (this.shooter instanceof MagazineFed magazineFed && !magazineFed.consumeRound()) {
			// Belt-and-braces: tick()'s isOutOfAmmo() check should already have
			// kept this from being reached, but if it is, don't fire a free shot.
			return;
		}
		Vec3d muzzle = this.shooter.getEyePos();
		Vec3d aim = aimCenter;

		// Accuracy degrades with range; a miss still draws a tracer past you.
		boolean hit = this.shooter.getRandom().nextDouble() < this.hitChance(distance) * hitChanceScale;
		double spreadScale = suppressive ? SUPPRESSIVE_SPREAD_SCALE : 1.0;
		Vec3d missOffset = Vec3d.ZERO;
		if (!hit) {
			double dx = (this.shooter.getRandom().nextDouble() - 0.5) * 2.4 * spreadScale;
			double dy = (this.shooter.getRandom().nextDouble() - 0.5) * 1.6 * spreadScale;
			double dz = (this.shooter.getRandom().nextDouble() - 0.5) * 2.4 * spreadScale;
			if (suppressive) {
				dy -= SUPPRESSIVE_GROUND_BIAS;
			}
			missOffset = new Vec3d(dx, dy, dz);
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
		HitscanImpact.ejectCasing(world, muzzle, this.shooter.getRandom());

		this.playFireSound();
		if (hit) {
			if (this.bypassesIframes()) {
				// Rapid fire: zero the target's post-hit invulnerability timer
				// so this hit actually lands instead of every other round in a
				// hose doing nothing — see the gun-feel pass. Paired with a
				// knockback well below vanilla melee's ~0.4 baseline (below) so
				// a burst doesn't juggle its target into the air instead of
				// just hitting hard.
				target.hurtTime = 0;
			}
			target.damage(this.shooter.getDamageSources().mobAttack(this.shooter), this.damage);
			Vec3d shove = target.getPos().subtract(this.shooter.getPos());
			if (shove.lengthSquared() > 1.0E-4) {
				target.takeKnockback(this.roundClass.knockbackStrength(), shove.x, shove.z);
			}
		} else if (this.whistleCooldown <= 0 && missOffset.length() < NEAR_MISS_THRESHOLD * spreadScale) {
			// Close graze: a whistle/crack right past the target's ears.
			// Suppressive fire scales the threshold up along with the wider
			// spread (so it doesn't just go quiet) and halves the cooldown —
			// the whole point is to keep this cue landing often.
			target.playSound(P7Sounds.GUN_WHISTLE.get(), 0.4f,
					0.9f + this.shooter.getRandom().nextFloat() * 0.2f);
			this.whistleCooldown = suppressive ? WHISTLE_COOLDOWN_TICKS / 2 : WHISTLE_COOLDOWN_TICKS;
		}

		// Wherever the round actually lands — chews up whatever block or
		// fluid caught it, hit or miss.
		HitscanImpact.resolve(world, muzzle, aim, this.range, this.shooter, this.roundClass);
	}

	/**
	 * Whether this weapon bypasses the target's brief post-hit invulnerability
	 * window so rapid fire actually stacks damage instead of every other round
	 * doing nothing. True by default — most Program guns hose rather than
	 * plink. A single well-spaced heavy shot (the sniper) overrides this to
	 * {@code false} and keeps normal invulnerability handling instead.
	 */
	protected boolean bypassesIframes() {
		return true;
	}

	/**
	 * Loud, low-pitched "the gun's opening up" report played once when this
	 * goal starts engaging a target — distinct from {@link #playFireSound()}'s
	 * quieter per-shot tick, per the gun-feel pass ("loud/weighty," not a tiny
	 * click on every round). Reuses {@link P7Sounds#MORTAR_FIRE} pitched low
	 * as a placeholder heavy report.
	 */
	protected void playOpeningFireCue() {
		if (this.shooter.getWorld() instanceof ServerWorld world) {
			ProgramAcoustics.emit(world, this.shooter.getEyePos(), P7Sounds.MORTAR_FIRE.get(),
					SoundCategory.HOSTILE, 1.3f, 0.7f + this.shooter.getRandom().nextFloat() * 0.1f);
		}
	}

	/** Chance of a hit at the given distance. Falls off linearly with range. */
	protected double hitChance(double distance) {
		return 0.9 - (distance / this.range) * 0.35;
	}

	/** The report itself; broken out so mounts with a different gun can swap it. */
	protected void playFireSound() {
		if (this.shooter.getWorld() instanceof ServerWorld world) {
			ProgramAcoustics.emit(world, this.shooter.getEyePos(), P7Sounds.GUN_FIRE.get(),
					SoundCategory.HOSTILE, 1.0f, 1.1f + this.shooter.getRandom().nextFloat() * 0.2f);
		}
	}
}
