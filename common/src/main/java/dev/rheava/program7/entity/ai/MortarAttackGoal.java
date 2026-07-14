package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.director.FireMissionManager;
import dev.rheava.program7.director.FireMissionManager.FireMission;
import dev.rheava.program7.director.ProgramDirectorState;
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
 *
 * <p>A {@link FireMissionManager} client, same shape as {@link
 * HowitzerAttackGoal} (see that class's doc): self-observed when it has its
 * own line-of-sight target, otherwise firing on whatever the manager has
 * assigned (an observer relay, counter-battery, a hot dwell cell, or a
 * shared battery mission — §4 battery fire lets several emplaced mortars
 * range and fire together). The ballistic solve is {@link BallisticSolver},
 * the same drag-aware helper the howitzer uses — this replaces the old
 * naive {@code dx / FLIGHT_TICKS} constant-speed backfill, which ignored the
 * shell's drag and landed rounds several blocks short at range.
 */
public class MortarAttackGoal extends Goal {
	private static final int FIRE_INTERVAL = 160;
	/** Vertical launch speed: a lower, quicker arc than the howitzer's, fitting the short range. */
	private static final double LAUNCH_VELOCITY_Y = 1.5;
	/** The shell is a {@link MortarShellEntity}: the ballistic solve mirrors its own gravity so the round lands on the aim point instead of short. */
	private static final double SHELL_GRAVITY = 0.06;
	/** Minimum gap between "the tube's dry" clicks so a starved battery doesn't spam it every tick. */
	private static final int DRY_FIRE_CLICK_INTERVAL_TICKS = 40;

	private final MortarEmplacementEntity shooter;
	private int cooldown;
	private int dryFireCooldown;

	public MortarAttackGoal(MortarEmplacementEntity shooter) {
		this.shooter = shooter;
		this.setControls(EnumSet.of(Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity target = this.shooter.getTarget();
		if (target != null && target.isAlive()) {
			return true;
		}
		return this.assignedTarget() != null;
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
		if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
			return;
		}
		FireMissionManager manager = ProgramDirectorState.get(world).getFireMissionManager();
		long now = world.getTime();

		LivingEntity target = this.shooter.getTarget();
		FireMission mission;
		Vec3d aimBase;
		if (target != null && target.isAlive()) {
			mission = manager.updateSelfObserved(this.shooter, target, now);
			aimBase = target.getPos();
			this.shooter.getLookControl().lookAt(target, 30.0f, 30.0f);
		} else {
			mission = manager.missionFor(this.shooter);
			Vec3d assigned = mission == null ? null : mission.targetPos();
			if (assigned == null) {
				return;
			}
			aimBase = assigned;
			this.shooter.getLookControl().lookAt(aimBase.x, aimBase.y, aimBase.z);
		}

		if (this.cooldown > 0) {
			this.cooldown--;
			return;
		}

		Vec3d tubePos = this.shooter.getPos();
		double dx = aimBase.x - tubePos.x;
		double dz = aimBase.z - tubePos.z;
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance < this.shooter.indirectMinRange() || distance > this.shooter.indirectMaxRange()
				|| !this.hasClearSky()) {
			return;
		}
		if (!this.shooter.consumeRound()) {
			// Magazine's dry: click occasionally instead of firing a free
			// shell, and don't reset the fire cooldown so it retries as soon
			// as a resupply tops the tube back up.
			this.tickDryFireClick();
			return;
		}
		double spread = manager.currentSpread(mission, tubePos);
		this.fire(world, aimBase, spread);
		manager.onShotFired(mission);
		this.cooldown = FIRE_INTERVAL;
	}

	/**
	 * The manager's assigned aim point for this tube, or null if it has no
	 * standing mission with a target — used by {@link #canStart} so the goal
	 * runs for a purely indirect (no line-of-sight) fire mission.
	 */
	private Vec3d assignedTarget() {
		if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
			return null;
		}
		FireMission mission = ProgramDirectorState.get(world).getFireMissionManager().missionFor(this.shooter);
		return mission == null ? null : mission.targetPos();
	}

	/** Plays the empty-magazine click on a cooldown so a starved tube doesn't spam it every tick. */
	private void tickDryFireClick() {
		if (this.dryFireCooldown > 0) {
			this.dryFireCooldown--;
			return;
		}
		this.dryFireCooldown = DRY_FIRE_CLICK_INTERVAL_TICKS;
		this.shooter.playSound(P7Sounds.WEAPON_DRY_FIRE.get(), 0.6f,
				0.9f + this.shooter.getRandom().nextFloat() * 0.15f);
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

	/**
	 * Lob one shell at {@code aimBase}, scattered inside the mission's current
	 * error radius. Close, well-spotted, ranged-in fire lands tight; long or
	 * unobserved fire scatters wide (§3).
	 */
	private void fire(ServerWorld world, Vec3d aimBase, double spread) {
		Vec3d tube = this.shooter.getPos().add(0.0, 2.0, 0.0);

		this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 1.5f, 0.8f);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, tube.x, tube.y, tube.z, 6, 0.2, 0.1, 0.2, 0.02);

		// Aim isn't perfect: scatter the impact point inside the CEP radius.
		Vec3d aim = aimBase.add(
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * spread,
				0.0,
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * spread);

		MortarShellEntity shell = new MortarShellEntity(world, this.shooter);
		shell.setPosition(tube.x, tube.y, tube.z);

		Vec3d launch = BallisticSolver.solve(tube, aim, LAUNCH_VELOCITY_Y, SHELL_GRAVITY);
		shell.setVelocity(launch.x, launch.y, launch.z);
		world.spawnEntity(shell);
	}
}
