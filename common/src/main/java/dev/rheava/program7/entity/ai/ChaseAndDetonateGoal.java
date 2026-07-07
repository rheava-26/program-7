package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.AttackDroneEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Run the target down and light the fuse at contact range. There is no
 * defusing — once armed, the drone is committed, so kiting it into terrain
 * (or your enemies) is legitimate counterplay.
 *
 * <p>The terminal approach reads in three phases so the ram can be seen
 * coming and dodged on reaction:
 * <ul>
 *   <li><b>APPROACH</b> — ordinary chase until the target is within
 *       {@link #AIM_RANGE} blocks and in sight.</li>
 *   <li><b>AIM</b> ({@link #AIM_TICKS} ticks) — the drone holds position,
 *       locks its look on the target, and sparks a warning line down the
 *       path it's about to take. The dash vector is locked at the very end
 *       of this phase against the target's position at that instant, so
 *       strafing during AIM is what actually dodges the hit.</li>
 *   <li><b>DASH</b> (up to {@link #DASH_TICKS} ticks) — committed, unguided
 *       flight along the locked vector. Contact detonates it same as
 *       always; a clean miss drifts it into a short recovery before it
 *       re-approaches.</li>
 * </ul>
 */
public class ChaseAndDetonateGoal extends Goal {
	private static final double ARM_DISTANCE = 2.75;
	private static final double AIM_RANGE = 9.0;
	private static final int AIM_TICKS = 15;
	private static final int DASH_TICKS = 20;
	private static final int RECOVERY_TICKS = 20;
	private static final double DASH_SPEED = 1.1;
	/** How far down the dash line the warning telegraph reaches. */
	private static final double TELEGRAPH_LENGTH = 1.0;

	private final AttackDroneEntity drone;

	private enum Phase {
		APPROACH, AIM, DASH, RECOVERY
	}

	private Phase phase = Phase.APPROACH;
	private int phaseTimer = 0;
	private Vec3d dashVector = Vec3d.ZERO;

	public ChaseAndDetonateGoal(AttackDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity target = this.drone.getTarget();
		return target != null && target.isAlive() && !this.drone.isScrambled();
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
	public void start() {
		this.phase = Phase.APPROACH;
		this.phaseTimer = 0;
	}

	@Override
	public void stop() {
		this.phase = Phase.APPROACH;
		this.phaseTimer = 0;
		this.dashVector = Vec3d.ZERO;
		this.drone.getNavigation().stop();
	}

	@Override
	public void tick() {
		LivingEntity target = this.drone.getTarget();
		if (target == null) {
			return;
		}

		// Contact-range arming is unconditional — whatever phase the run is
		// in, walking (or crashing) into the drone lights the fuse exactly
		// like it always has.
		this.tryArm(target);

		switch (this.phase) {
			case APPROACH -> this.tickApproach(target);
			case AIM -> this.tickAim(target);
			case DASH -> this.tickDash();
			case RECOVERY -> this.tickRecovery();
		}
	}

	private void tickApproach(LivingEntity target) {
		this.drone.getLookControl().lookAt(target, 30.0f, 30.0f);
		this.drone.getNavigation().startMovingTo(
				target.getX(), target.getBodyY(0.5), target.getZ(), 1.4);

		boolean inRange = this.drone.squaredDistanceTo(target) <= AIM_RANGE * AIM_RANGE;
		if (inRange && this.drone.canSee(target)) {
			this.enterAim();
		}
	}

	private void enterAim() {
		this.phase = Phase.AIM;
		this.phaseTimer = 0;
		this.drone.getNavigation().stop();
	}

	private void tickAim(LivingEntity target) {
		// Hold position and stare it down — the AIM window is the readable
		// tell, not the dash itself.
		this.drone.getNavigation().stop();
		this.drone.getLookControl().lookAt(target, 30.0f, 30.0f);
		this.spawnWarningTelegraph(target);

		this.phaseTimer++;
		if (this.phaseTimer >= AIM_TICKS) {
			this.lockDashVector(target);
			this.phase = Phase.DASH;
			this.phaseTimer = 0;
		}
	}

	/** 2 CRIT sparks a tick along the first ~1 block of the line to the target. */
	private void spawnWarningTelegraph(LivingEntity target) {
		if (!(this.drone.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		Vec3d from = this.drone.getEyePos();
		Vec3d toTarget = target.getEyePos().subtract(from);
		double length = toTarget.length();
		if (length < 1.0E-4) {
			return;
		}
		Vec3d direction = toTarget.multiply(1.0 / length);
		double reach = Math.min(TELEGRAPH_LENGTH, length);
		for (int i = 0; i < 2; i++) {
			double along = (i + 1) / 3.0 * reach;
			Vec3d point = from.add(direction.multiply(along));
			serverWorld.spawnParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/** Locks the dash onto the target's CURRENT spot — strafing after this dodges it. */
	private void lockDashVector(LivingEntity target) {
		Vec3d toTarget = target.getPos().subtract(this.drone.getPos());
		if (toTarget.lengthSquared() < 1.0E-4) {
			// Degenerate case (they're standing inside the drone): just keep
			// whatever heading it already had rather than divide by zero.
			toTarget = new Vec3d(0.0, 0.0, 1.0);
		}
		this.dashVector = toTarget.normalize().multiply(DASH_SPEED);
	}

	private void tickDash() {
		this.drone.setVelocity(this.dashVector);

		if (this.drone.horizontalCollision) {
			// ProgramDroneEntity only turns horizontalCollision into damage
			// while the drone is scrambled (see its tickMovement) — an
			// unscrambled dash slamming into a wall would otherwise be a
			// silent no-op. Force the same arm-and-detonate path a contact
			// hit uses so baiting a ram into terrain is a real kill, not
			// just a bounce.
			this.drone.arm();
			this.enterRecovery();
			return;
		}

		this.phaseTimer++;
		if (this.phaseTimer >= DASH_TICKS) {
			// Overshot: no contact, no wall. Let it coast off before it
			// re-acquires and comes back around.
			this.enterRecovery();
		}
	}

	private void enterRecovery() {
		this.phase = Phase.RECOVERY;
		this.phaseTimer = 0;
	}

	private void tickRecovery() {
		// No chase, no steering — just drift out the dash's momentum.
		this.drone.getNavigation().stop();
		this.phaseTimer++;
		if (this.phaseTimer >= RECOVERY_TICKS) {
			this.phase = Phase.APPROACH;
			this.phaseTimer = 0;
		}
	}

	private void tryArm(LivingEntity target) {
		if (this.drone.squaredDistanceTo(target) <= ARM_DISTANCE * ARM_DISTANCE) {
			this.drone.arm();
		}
	}
}
