package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.MortarEmplacementEntity;
import dev.rheava.program7.entity.MortarShellEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Indirect fire: the tube doesn't chase or path anywhere, it just traverses
 * onto its target and lobs a shell in on a long timer. It needs clear sky
 * over the tube to fire at all — a roof or canopy overhead shuts it down.
 */
public class MortarAttackGoal extends Goal {
	private static final int FIRE_INTERVAL = 160;
	private static final double MIN_RANGE = 8.0;
	private static final double MAX_RANGE = 40.0;
	/** Ticks the shell takes to reach the top of its arc and come back down. */
	private static final double FLIGHT_TICKS = 50.0;
	/** Max scatter on the impact point, in either direction on each axis. */
	private static final double MAX_SPREAD = 2.0;

	private final MortarEmplacementEntity shooter;
	private int cooldown;

	public MortarAttackGoal(MortarEmplacementEntity shooter) {
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
		for (int dy = 1; dy <= 4; dy++) {
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
		Vec3d tube = this.shooter.getPos().add(0.0, 2.0, 0.0);

		this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 1.5f, 0.8f);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, tube.x, tube.y, tube.z, 6, 0.2, 0.1, 0.2, 0.02);

		// Aim isn't perfect: scatter the impact point a couple of blocks.
		Vec3d aim = target.getPos().add(
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * MAX_SPREAD,
				0.0,
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * MAX_SPREAD);

		MortarShellEntity shell = new MortarShellEntity(world, this.shooter);
		shell.setPosition(tube.x, tube.y, tube.z);

		// Simple ballistic solve: fixed launch/flight time, backfill the
		// horizontal speed needed to cover the distance in that time.
		double dx = aim.x - tube.x;
		double dz = aim.z - tube.z;
		shell.setVelocity(dx / FLIGHT_TICKS, 1.5, dz / FLIGHT_TICKS);
		world.spawnEntity(shell);
	}
}
