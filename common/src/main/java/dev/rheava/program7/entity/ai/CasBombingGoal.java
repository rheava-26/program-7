package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.director.FireMissionManager;
import dev.rheava.program7.director.FireMissionManager.FireMission;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.entity.BombEntity;
import dev.rheava.program7.entity.GunshipEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Close air support (see {@code ARTILLERY_AND_INDIRECT_FIRE.md} §4 CAS row):
 * "a fixed-wing or the gunship makes a bombing run: a stick of bombs walked
 * along a line across the target on a diving pass." A {@link
 * FireMissionManager} client, but unlike every tube in the roster it flies
 * — it has no fixed range, it flies <em>to</em> the target instead of
 * lobbing something at it, then releases {@link #BOMBS_PER_RUN} bombs a few
 * ticks apart as it passes overhead, each offset along its own heading so
 * they land in a line, not a cluster.
 *
 * <p>Registered at a lower priority than the belly autocannon's {@code
 * BurstGunAttackGoal} (see {@link GunshipEntity#initGoals}), so — same
 * arbitration as {@link NavalBombardmentGoal}/{@link HowitzerAttackGoal} —
 * it only actually starts when the gunship has no line-of-sight target of
 * its own: the cannon is direct fire and always wins that fight; this is
 * what fires when the gunship is instead answering an observer's relay on a
 * target it can't see. Bombs draw from the same onboard magazine as the
 * cannon (no free ammo), same discipline as the gunboat's secondary turrets.
 */
public class CasBombingGoal extends Goal {
	/** How far above the target the run-in altitude sits. */
	private static final double RUN_ALTITUDE = 14.0;
	private static final double APPROACH_SPEED = 1.0;
	/** Close enough (horizontally) to the run point, and low enough above it, to start releasing. */
	private static final double RELEASE_RADIUS = 7.0;
	private static final double RELEASE_MAX_ALTITUDE_ABOVE_TARGET = RUN_ALTITUDE + 6.0;
	private static final int BOMBS_PER_RUN = 4;
	private static final int BOMB_STAGGER_TICKS = 5;
	/** Spacing between bombs in the stick, walked along the aircraft's heading. */
	private static final double STICK_SPACING = 4.0;
	/** Long break-off between runs — this is a set-piece, not a steady patter. */
	private static final int RUN_COOLDOWN = 300;
	private static final int DRY_FIRE_CLICK_INTERVAL_TICKS = 40;

	private final GunshipEntity shooter;
	private int cooldown;
	private int dryFireCooldown;
	private int bombsLeft;
	private Vec3d stickAim;
	private FireMission runMission;

	public CasBombingGoal(GunshipEntity shooter) {
		this.shooter = shooter;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.shooter.getTarget() != null && this.shooter.getTarget().isAlive()) {
			// The belly cannon owns any live target — CAS only answers
			// purely indirect designations (see class doc).
			return false;
		}
		return this.assignedTarget() != null;
	}

	@Override
	public boolean shouldContinue() {
		return this.bombsLeft > 0 || this.canStart();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
			return;
		}
		FireMissionManager manager = ProgramDirectorState.get(world).getFireMissionManager();

		if (this.bombsLeft > 0) {
			if (this.cooldown > 0) {
				this.cooldown--;
				return;
			}
			if (!this.shooter.consumeRound()) {
				this.bombsLeft = 0;
				this.tickDryFireClick();
				this.cooldown = RUN_COOLDOWN;
				return;
			}
			this.releaseOneBomb(world);
			manager.onShotFired(this.runMission);
			this.bombsLeft--;
			this.cooldown = this.bombsLeft > 0 ? BOMB_STAGGER_TICKS : RUN_COOLDOWN;
			return;
		}

		FireMission mission = manager.missionFor(this.shooter);
		Vec3d aimBase = mission == null ? null : mission.targetPos();
		if (aimBase == null) {
			return;
		}
		this.shooter.getLookControl().lookAt(aimBase.x, aimBase.y, aimBase.z);

		Vec3d runPoint = new Vec3d(aimBase.x, aimBase.y + RUN_ALTITUDE, aimBase.z);
		this.shooter.getNavigation().startMovingTo(runPoint.x, runPoint.y, runPoint.z, APPROACH_SPEED);

		if (this.cooldown > 0) {
			this.cooldown--;
			return;
		}

		Vec3d shooterPos = this.shooter.getPos();
		double dx = aimBase.x - shooterPos.x;
		double dz = aimBase.z - shooterPos.z;
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		double altitudeAboveTarget = shooterPos.y - aimBase.y;
		if (horizontalDistance > RELEASE_RADIUS || altitudeAboveTarget < RUN_ALTITUDE - 6.0
				|| altitudeAboveTarget > RELEASE_MAX_ALTITUDE_ABOVE_TARGET) {
			// Still inbound, or already overshot — not lined up for the run yet.
			return;
		}
		if (!this.shooter.consumeRound()) {
			this.tickDryFireClick();
			return;
		}

		double spread = manager.currentSpread(mission, shooterPos);
		this.stickAim = aimBase.add(
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * spread,
				0.0,
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * spread);
		this.runMission = mission;
		this.bombsLeft = BOMBS_PER_RUN - 1;
		this.releaseOneBomb(world);
		manager.onShotFired(mission);
		this.cooldown = this.bombsLeft > 0 ? BOMB_STAGGER_TICKS : RUN_COOLDOWN;
	}

	private Vec3d assignedTarget() {
		if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
			return null;
		}
		FireMission mission = ProgramDirectorState.get(world).getFireMissionManager().missionFor(this.shooter);
		return mission == null ? null : mission.targetPos();
	}

	private void tickDryFireClick() {
		if (this.dryFireCooldown > 0) {
			this.dryFireCooldown--;
			return;
		}
		this.dryFireCooldown = DRY_FIRE_CLICK_INTERVAL_TICKS;
		this.shooter.playSound(P7Sounds.WEAPON_DRY_FIRE.get(), 0.6f,
				0.9f + this.shooter.getRandom().nextFloat() * 0.15f);
	}

	/**
	 * Drops one bomb from the current position, walking the stick along the
	 * aircraft's own current heading so consecutive bombs land in a line
	 * rather than a cluster (index tracked via {@link #bombsLeft} counting
	 * down from {@link #BOMBS_PER_RUN} - 1).
	 */
	private void releaseOneBomb(ServerWorld world) {
		Vec3d dropPoint = this.shooter.getPos();
		this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 1.3f, 1.5f);
		world.spawnParticles(ParticleTypes.CLOUD, dropPoint.x, dropPoint.y, dropPoint.z, 4, 0.3, 0.1, 0.3, 0.02);

		double yawRad = Math.toRadians(this.shooter.getYaw());
		Vec3d heading = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
		int bombIndex = BOMBS_PER_RUN - 1 - this.bombsLeft;
		Vec3d aim = this.stickAim.add(heading.multiply(bombIndex * STICK_SPACING));

		BombEntity bomb = new BombEntity(world, this.shooter);
		bomb.setPosition(dropPoint.x, dropPoint.y - 1.0, dropPoint.z);
		Vec3d shooterVelocity = this.shooter.getVelocity();
		// Released, not launched: it inherits the aircraft's own velocity, biased toward the walked-in aim point along the ground track.
		Vec3d towardAim = aim.subtract(dropPoint);
		double horizontal = Math.max(Math.sqrt(towardAim.x * towardAim.x + towardAim.z * towardAim.z), 1.0E-4);
		Vec3d nudge = new Vec3d(towardAim.x / horizontal, 0.0, towardAim.z / horizontal).multiply(0.15);
		bomb.setVelocity(shooterVelocity.x + nudge.x, Math.min(shooterVelocity.y, -0.05), shooterVelocity.z + nudge.z);
		world.spawnEntity(bomb);
	}
}
