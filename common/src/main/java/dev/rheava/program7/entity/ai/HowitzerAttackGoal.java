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
 * <p>This is the first (and this pass, only) client of the Director-side
 * {@link FireMissionManager} (see {@code ARTILLERY_AND_INDIRECT_FIRE.md} §8).
 * Two firing modes share one loop:
 *
 * <ul>
 *   <li><b>Self-observed</b> — it has its own line-of-sight {@code getTarget()}
 *       (a mortar-on-a-ridge). Each tick it publishes that live target to the
 *       manager as the top-priority eyes-on designation and fires on it.</li>
 *   <li><b>Assigned</b> — it has no target of its own, so it fires on the
 *       {@link FireMission} the manager has handed it: a recon unit's live
 *       relay, a counter-battery origin, or a hot dwell cell (§2 acquisition).
 *       This is the "no line of sight of its own" indirect shot.</li>
 * </ul>
 *
 * <p>Accuracy (§3) and the ranging walk-in (§2 step 3) both come from the
 * manager: the flat {@code MAX_SPREAD} of the self-contained pass is gone,
 * replaced by {@link FireMissionManager#currentSpread} (a range floor that
 * grows with chunk distance, tightened by spotting and ranging) and advanced
 * one step per shot via {@link FireMissionManager#onShotFired}.
 */
public class HowitzerAttackGoal extends Goal {
	/** ~5.5s between rounds at 20 ticks/sec — slow, heavy cadence. */
	private static final int FIRE_INTERVAL = 110;
	/** Artillery standoff: it won't waste heavy rounds on something in its lap. */
	private static final double MIN_RANGE = 24.0;
	/** Long reach — well beyond several chunks (~7). Paired with the howitzer's
	 *  raised follow range so it can actually acquire a target this far out. */
	private static final double MAX_RANGE = 112.0;
	/** Vertical launch speed: a higher arc than the mortar for the extra reach. */
	private static final double LAUNCH_VELOCITY_Y = 2.5;
	/** The shell is a {@link HowitzerShellEntity} ({@code ThrownEntity}): it
	 *  loses 1% of its speed to drag every tick and falls under this gravity —
	 *  the exact values the shell itself uses. The ballistic solve below mirrors
	 *  them so the round actually lands on the aim point instead of far short. */
	private static final double SHELL_DRAG = 0.99;
	private static final double SHELL_GRAVITY = 0.045;
	/** Safety cap on the trajectory sim so a target the arc can't reach (far
	 *  above the tube) can't spin the solve forever. */
	private static final int MAX_FLIGHT_TICKS = 600;
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
		// (an observer relay, counter-battery, or dwell cell) for this tube.
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
		if (distance < MIN_RANGE || distance > MAX_RANGE || !this.hasClearSky()) {
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

		// Drag-aware ballistic solve. A naive dx/flightTicks backfill assumes a
		// constant horizontal speed, but the shell bleeds SHELL_DRAG each tick,
		// so that lands the round far short (worse the farther it flies). Instead
		// simulate the vertical arc from the muzzle down to the target's height
		// to get the true airtime, accumulating the horizontal decay sum
		// (1 + d + d^2 + ...) over exactly those ticks; the launch speed that
		// actually covers dx is then dx / thatSum. The sim mirrors ThrownEntity's
		// integration order: move by the current velocity, then apply drag, then
		// gravity.
		double dx = aim.x - tube.x;
		double dz = aim.z - tube.z;
		double targetRelY = aim.y - tube.y;
		double vy = LAUNCH_VELOCITY_Y;
		double y = 0.0;
		double decaySum = 0.0;
		double factor = 1.0;
		for (int k = 0; k < MAX_FLIGHT_TICKS; k++) {
			decaySum += factor;
			y += vy;
			vy = vy * SHELL_DRAG - SHELL_GRAVITY;
			factor *= SHELL_DRAG;
			if (vy < 0.0 && y <= targetRelY) {
				break;
			}
		}
		double vx = decaySum > 1.0E-6 ? dx / decaySum : 0.0;
		double vz = decaySum > 1.0E-6 ? dz / decaySum : 0.0;
		shell.setVelocity(vx, LAUNCH_VELOCITY_Y, vz);
		world.spawnEntity(shell);
	}
}
