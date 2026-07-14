package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.director.FireMissionManager;
import dev.rheava.program7.director.FireMissionManager.FireMission;
import dev.rheava.program7.director.ProgramDirectorState;
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
 * <p>This is a client of the Director-side {@link FireMissionManager} (see
 * {@code ARTILLERY_AND_INDIRECT_FIRE.md} §8), same as {@link
 * MortarAttackGoal}. Two firing modes share one loop:
 *
 * <ul>
 *   <li><b>Self-observed</b> — it has its own line-of-sight {@code getTarget()}
 *       (a mortar-on-a-ridge). Each tick it publishes that live target to the
 *       manager as the top-priority eyes-on designation and fires on it.</li>
 *   <li><b>Assigned</b> — it has no target of its own, so it fires on the
 *       {@link FireMission} the manager has handed it: a recon unit's live
 *       relay, a counter-battery origin, a hot dwell cell, or a shared battery
 *       mission (§2 acquisition / §4 battery fire).</li>
 * </ul>
 *
 * <p>Accuracy (§3) and the ranging walk-in (§2 step 3) both come from the
 * manager: {@link FireMissionManager#currentSpread} (a range floor that grows
 * with chunk distance, tightened by spotting and ranging) advanced one step
 * per shot via {@link FireMissionManager#onShotFired}. The ballistic solve
 * itself is {@link BallisticSolver}, shared with every other artillery type.
 */
public class HowitzerAttackGoal extends Goal {
	/** ~5.5s between rounds at 20 ticks/sec — slow, heavy cadence. */
	private static final int FIRE_INTERVAL = 110;
	/** Vertical launch speed: a higher arc than the mortar for the extra reach. */
	private static final double LAUNCH_VELOCITY_Y = 2.5;
	/** The shell is a {@link HowitzerShellEntity}: the ballistic solve mirrors its own gravity so the round lands on the aim point instead of far short. */
	private static final double SHELL_GRAVITY = 0.045;
	/** Minimum gap between "the tube's dry" clicks so a starved battery doesn't spam it every tick. */
	private static final int DRY_FIRE_CLICK_INTERVAL_TICKS = 40;

	private final HowitzerEntity shooter;
	private int cooldown;
	private int dryFireCooldown;

	public HowitzerAttackGoal(HowitzerEntity shooter) {
		this.shooter = shooter;
		this.setControls(EnumSet.of(Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity target = this.shooter.getTarget();
		if (target != null && target.isAlive()) {
			return true;
		}
		// No eyes of its own — but the manager may still have a mission assigned
		// (an observer relay, counter-battery, dwell cell, or battery mate) for this tube.
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

		// Resolve where we're firing and drive the mission: a live line-of-sight
		// target self-designates (top priority, eyes-on) every tick; otherwise
		// the manager's standing assignment supplies the aim point.
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
		for (int dy = 1; dy <= 5; dy++) {
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
		Vec3d tube = this.shooter.getPos().add(0.0, 2.6, 0.0);

		// Deeper, louder report than the mortar's — a bigger tube going off.
		this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 2.0f, 0.6f);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, tube.x, tube.y, tube.z, 10, 0.3, 0.15, 0.3, 0.03);
		world.spawnParticles(ParticleTypes.CLOUD, tube.x, tube.y, tube.z, 6, 0.25, 0.1, 0.25, 0.02);

		// Aim isn't perfect: scatter the impact point inside the CEP radius.
		Vec3d aim = aimBase.add(
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * spread,
				0.0,
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * spread);

		HowitzerShellEntity shell = new HowitzerShellEntity(world, this.shooter);
		shell.setPosition(tube.x, tube.y, tube.z);

		Vec3d launch = BallisticSolver.solve(tube, aim, LAUNCH_VELOCITY_Y, SHELL_GRAVITY);
		shell.setVelocity(launch.x, launch.y, launch.z);
		world.spawnEntity(shell);
	}
}
