package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.HowitzerEntity;
import dev.rheava.program7.entity.HowitzerShellEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The howitzer's main gun: the same shape as {@link MortarAttackGoal} — no
 * chasing, no line-of-sight requirement beyond open sky over the tube, just a
 * traverse-and-lob on a timer — but heavier, much farther-reaching, and far
 * slower to cycle. Where the mortar answers in a steady patter, the howitzer
 * answers with a handful of heavy rounds spread across a whole fight.
 *
 * <p>Self-contained for this pass: it self-targets and self-observes exactly
 * like the mortar. The {@code FireMissionManager}/observer/ranging layer from
 * the artillery doc is a later pass.
 */
public class HowitzerAttackGoal extends Goal {
	/** ~5.5s between rounds at 20 ticks/sec — slow, heavy cadence. */
	private static final int FIRE_INTERVAL = 110;
	private static final double MIN_RANGE = 16.0;
	private static final double MAX_RANGE = 56.0;
	/** Ticks the shell takes to reach the top of its arc and come back down — longer than the mortar's for the extra range. */
	private static final double FLIGHT_TICKS = 80.0;
	/** Max scatter on the impact point, in either direction on each axis. */
	private static final double MAX_SPREAD = 3.0;

	private final HowitzerEntity shooter;
	private int cooldown;

	public HowitzerAttackGoal(HowitzerEntity shooter) {
		this.shooter = shooter;
		this.setControls(EnumSet.of(Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity target = this.shooter.getTarget();
		return target != null && target.isAlive();
	}

	@Override
	public boolean shouldContinue() {
		return this.canStart();
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

		if (this.cooldown > 0) {
			this.cooldown--;
			return;
		}

		double distance = this.shooter.distanceTo(target);
		if (distance < MIN_RANGE || distance > MAX_RANGE || !this.hasClearSky()) {
			return;
		}
		this.fire(target);
		this.cooldown = FIRE_INTERVAL;
	}

	/** The tube needs open air above it to fire — no roofs, no canopy. */
	private boolean hasClearSky() {
		BlockPos base = this.shooter.getBlockPos();
		for (int dy = 1; dy <= 5; dy++) {
			if (!this.shooter.getWorld().getBlockState(base.up(dy)).isAir()) {
				return false;
			}
		}
		return true;
	}

	private void fire(LivingEntity target) {
		if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
			return;
		}
		Vec3d tube = this.shooter.getPos().add(0.0, 2.6, 0.0);

		// Deeper, louder report than the mortar's — a bigger tube going off.
		this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 2.0f, 0.6f);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, tube.x, tube.y, tube.z, 10, 0.3, 0.15, 0.3, 0.03);
		world.spawnParticles(ParticleTypes.CLOUD, tube.x, tube.y, tube.z, 6, 0.25, 0.1, 0.25, 0.02);

		// Aim isn't perfect: scatter the impact point a few blocks.
		Vec3d aim = target.getPos().add(
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * MAX_SPREAD,
				0.0,
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * MAX_SPREAD);

		HowitzerShellEntity shell = new HowitzerShellEntity(world, this.shooter);
		shell.setPosition(tube.x, tube.y, tube.z);

		// Same simple ballistic solve as the mortar: fixed launch/flight time,
		// backfill the horizontal speed needed to cover the distance in that time.
		double dx = aim.x - tube.x;
		double dz = aim.z - tube.z;
		shell.setVelocity(dx / FLIGHT_TICKS, 2.1, dz / FLIGHT_TICKS);
		world.spawnEntity(shell);
	}
}
