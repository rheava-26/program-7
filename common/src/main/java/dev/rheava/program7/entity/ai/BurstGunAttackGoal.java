package dev.rheava.program7.entity.ai;

import dev.rheava.program7.entity.ProgramDroneEntity;
import net.minecraft.entity.LivingEntity;

/**
 * Machine-gun variant of {@link GunAttackGoal}: instead of one shot per
 * fire interval, each firing window is a BURST of shots spaced a few ticks
 * apart, followed by a long cooldown while the belt cycles back up. It
 * reuses the base goal's {@code canStart()}/{@code shouldContinue()} target
 * checks unchanged, and every individual shot still goes through the base
 * goal's protected {@link #fire(LivingEntity, double)} hook (and, through
 * that, {@link #hitChance(double)} and {@link #playFireSound()}) — only the
 * "when do I pull the trigger" clock is different, so a subclass of this
 * goal can still swap accuracy or the fire sound exactly like a subclass of
 * {@link GunAttackGoal} would.
 */
public class BurstGunAttackGoal extends GunAttackGoal {
	/** Ticks between shots within a single burst. */
	private static final int BURST_SHOT_INTERVAL = 3;
	/** Acquisition delay before the first burst, mirroring the base goal's. */
	private static final int ACQUISITION_DELAY = 10;

	// speed is re-stored here because GunAttackGoal keeps its copy private —
	// a burst weapon needs its own firing clock instead of the base
	// one-shot-per-interval timer, but the movement rule (close in until
	// within 70% of range, hold once there) is identical, so it's
	// duplicated rather than reworked.
	private final double speed;
	private final int burstSize;
	private final int burstCooldown;

	private int burstShotsLeft;
	private int intraBurstTimer;
	private int cooldown;

	public BurstGunAttackGoal(ProgramDroneEntity shooter, double speed, double range,
			int burstSize, int burstCooldown, float damagePerShot) {
		// burstCooldown doubles as the base constructor's fireInterval; it's
		// only ever read by the base's tick(), which this goal overrides
		// outright, so the value itself is inert there.
		super(shooter, speed, range, burstCooldown, damagePerShot);
		this.speed = speed;
		this.burstSize = burstSize;
		this.burstCooldown = burstCooldown;
	}

	@Override
	public void start() {
		super.start();
		this.cooldown = ACQUISITION_DELAY;
		this.burstShotsLeft = 0;
		this.intraBurstTimer = 0;
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
		// Shared with the base goal: keeps the last-seen spot warm and, for a
		// short window after losing sight, still counts as "on target" so a
		// burst can keep hosing down cover instead of cutting off instantly.
		boolean sighted = this.updateSight(target, canSee);
		if (this.speed > 0) {
			if (distance > this.range * 0.7 || !canSee) {
				this.shooter.getNavigation().startMovingTo(target, this.speed);
			} else {
				this.shooter.getNavigation().stop();
			}
		}

		if (this.burstShotsLeft > 0) {
			// Mid-burst: cycle the intra-burst timer down and fire on zero.
			if (this.intraBurstTimer > 0) {
				this.intraBurstTimer--;
			} else if (distance <= this.range && sighted) {
				this.fireBurstShot(target, distance, canSee);
				this.burstShotsLeft--;
				this.intraBurstTimer = BURST_SHOT_INTERVAL;
				if (this.burstShotsLeft == 0) {
					// Burst spent: the belt cycles back up before the next one.
					this.cooldown = this.burstCooldown;
				}
			}
			return;
		}

		if (this.cooldown > 0) {
			this.cooldown--;
		} else if (distance <= this.range && sighted) {
			// Cooldown's clear and the target's in the envelope: rack a new burst.
			this.burstShotsLeft = this.burstSize;
			this.intraBurstTimer = 0;
		}
	}

	/** Fires one shot of the burst, aimed live if visible or blind (suppression) otherwise. */
	private void fireBurstShot(LivingEntity target, double distance, boolean canSee) {
		if (canSee) {
			this.fire(target, distance);
		} else {
			this.fire(target, distance, this.getLastSeenPos(), SUPPRESSION_HIT_CHANCE_SCALE);
		}
	}
}
