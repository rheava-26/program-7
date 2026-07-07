package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * The Program's small-arms fire: a hitscan burst with a visible tracer line.
 * Shared by anything that mounts a gun — the ground drone paths into range
 * first, the autogun turret just tracks and shoots.
 *
 * <p>Accuracy falls off with distance, so keeping range on a shooter is real
 * counterplay even before cover comes into it.
 */
public class GunAttackGoal extends Goal {
	// shooter/range/damage are protected: subclasses (the sniper's standoff
	// variant) build their own movement and accuracy rules on top of them.
	protected final ProgramDroneEntity shooter;
	/** Movement speed while closing in; 0 for stationary mounts. */
	private final double speed;
	protected final double range;
	private final int fireInterval;
	protected final float damage;
	private int cooldown;

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
		if (this.speed > 0) {
			if (distance > this.range * 0.7 || !canSee) {
				this.shooter.getNavigation().startMovingTo(target, this.speed);
			} else {
				this.shooter.getNavigation().stop();
			}
		}

		if (this.cooldown > 0) {
			this.cooldown--;
		} else if (distance <= this.range && canSee) {
			this.fire(target, distance);
			this.cooldown = this.fireInterval;
		}
	}

	// Overridable, along with the hitChance()/playFireSound() hooks below,
	// so a mount with a different gun or accuracy model doesn't have to
	// duplicate the tracer and particle work.
	protected void fire(LivingEntity target, double distance) {
		if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
			return;
		}
		Vec3d muzzle = this.shooter.getEyePos();
		Vec3d aim = target.getBoundingBox().getCenter();

		// Accuracy degrades with range; a miss still draws a tracer past you.
		boolean hit = this.shooter.getRandom().nextDouble() < this.hitChance(distance);
		if (!hit) {
			aim = aim.add((this.shooter.getRandom().nextDouble() - 0.5) * 2.4,
					(this.shooter.getRandom().nextDouble() - 0.5) * 1.6,
					(this.shooter.getRandom().nextDouble() - 0.5) * 2.4);
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
