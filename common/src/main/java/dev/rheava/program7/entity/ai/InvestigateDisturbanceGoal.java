package dev.rheava.program7.entity.ai;

import java.util.EnumSet;
import java.util.List;

import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.entity.ProgramDroneEntity.AlertState;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * "It doesn't instantly know what it saw." A Program unit with a clear line
 * of sight on nearby movement — a player's, or just some passing mob's — goes
 * from {@link AlertState#UNAWARE} to {@link AlertState#SUSPICIOUS} and darts
 * over to look, rather than either ignoring it or insta-locking a target.
 *
 * <p>Entirely soundborne/behavioural by design: a rising psionic swell
 * ({@link P7Sounds#DRONE_INTERFERENCE}) followed by a sharp crack ({@link
 * P7Sounds#SCAN_STING}) announces the notice, then the unit flies/drives to
 * the disturbance and looks. If what's there turns out to be the player with
 * a clear sightline, that's a confirmed contact: the unit sets them as its
 * target (which {@link ProgramDroneEntity#tickAcquisitionAlert} then reads to
 * push the ramp to {@link AlertState#TRACKING}/{@link AlertState#ENGAGING}
 * and play the usual acquisition beat), and this goal steps aside for
 * whatever scan/combat goal handles it from there. If it was just a mob, the
 * unit lingers a beat — long enough to read as "was just checking" — then
 * peels off back toward wherever it started and decays back down to {@link
 * AlertState#UNAWARE}.
 *
 * <p>No text popups, ever — everything here is sound and motion.
 */
public class InvestigateDisturbanceGoal extends Goal {
	/** How far out a unit can notice motion at all — gated on clear LOS, so cover still works. */
	private static final double DETECTION_RANGE = 40.0;
	/** entity.getVelocity().lengthSquared() above this reads as "moving," not just standing/jittering. */
	private static final double MOVEMENT_SPEED_THRESHOLD_SQ = 0.0025;
	/** Ticks between the interference swell and the spotted crack. */
	private static final int BUILDUP_TICKS = 25;
	private static final double SEARCH_SPEED = 1.35;
	private static final double ARRIVAL_RADIUS = 3.5;
	private static final int SEARCH_TIMEOUT_TICKS = 200;
	/** How close a player has to be to the disturbance position, on arrival, to count as "found." */
	private static final double IDENTIFY_RANGE = 10.0;
	private static final int LINGER_TICKS = 40;
	private static final double RETURN_SPEED = 1.0;
	private static final double RETURN_ARRIVAL_RADIUS = 3.0;
	private static final int RETURN_TIMEOUT_TICKS = 200;
	/** Cooldown after a false-alarm cycle finishes, so the unit doesn't immediately re-trigger on the same mob. */
	private static final int RETRIGGER_COOLDOWN_TICKS = 100;

	private enum Phase { ALERTING, SEARCHING, LINGERING, RETURNING }

	private final ProgramDroneEntity drone;
	@Nullable
	private LivingEntity watched;
	private Vec3d disturbancePos = Vec3d.ZERO;
	private Vec3d homePos = Vec3d.ZERO;
	private Phase phase = Phase.ALERTING;
	private int phaseTicks;
	private boolean crackPlayed;
	private boolean done;
	private int cooldownTicks;

	public InvestigateDisturbanceGoal(ProgramDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.cooldownTicks > 0) {
			this.cooldownTicks--;
			return false;
		}
		if (this.drone.getTarget() != null || this.drone.isRetreating() || this.drone.isScrambled()) {
			return false;
		}
		AlertState state = this.drone.getAlertState();
		if (state != AlertState.UNAWARE && state != AlertState.SUSPICIOUS) {
			// Already searching/tracking/engaging — this goal doesn't pile on.
			return false;
		}

		Box box = this.drone.getBoundingBox().expand(DETECTION_RANGE);
		List<LivingEntity> moving = this.drone.getWorld().getEntitiesByClass(LivingEntity.class, box, this::isNoticeable);
		if (moving.isEmpty()) {
			return false;
		}

		LivingEntity closest = null;
		double bestDistSq = Double.MAX_VALUE;
		for (LivingEntity candidate : moving) {
			double distSq = this.drone.squaredDistanceTo(candidate);
			if (distSq < bestDistSq) {
				bestDistSq = distSq;
				closest = candidate;
			}
		}
		this.watched = closest;
		return this.watched != null;
	}

	/** Movement noticed in clear LOS — could be a player OR a mob; that's the point, it doesn't know yet. */
	private boolean isNoticeable(LivingEntity candidate) {
		if (candidate == this.drone || !candidate.isAlive() || candidate instanceof ProgramDroneEntity) {
			return false;
		}
		if (candidate instanceof PlayerEntity player && (player.isSpectator() || player.isCreative())) {
			return false;
		}
		if (candidate.getVelocity().lengthSquared() <= MOVEMENT_SPEED_THRESHOLD_SQ) {
			return false;
		}
		return this.drone.canSee(candidate);
	}

	@Override
	public boolean shouldContinue() {
		return !this.done && !this.drone.isRetreating() && !this.drone.isScrambled();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.homePos = this.drone.getPos();
		this.disturbancePos = this.watched != null ? this.watched.getPos() : this.drone.getPos();
		this.phase = Phase.ALERTING;
		this.phaseTicks = 0;
		this.crackPlayed = false;
		this.done = false;
		this.drone.getNavigation().stop();
		this.drone.setAlertState(AlertState.SUSPICIOUS);
		// Loud and positional: the rising psionic interference swell.
		this.drone.playSound(P7Sounds.DRONE_INTERFERENCE.get(), 1.0f, 1.0f);
	}

	@Override
	public void tick() {
		switch (this.phase) {
			case ALERTING -> this.tickAlerting();
			case SEARCHING -> this.tickSearching();
			case LINGERING -> this.tickLingering();
			case RETURNING -> this.tickReturning();
		}
	}

	private void tickAlerting() {
		this.drone.getLookControl().lookAt(this.disturbancePos.x, this.disturbancePos.y, this.disturbancePos.z);
		this.phaseTicks++;
		if (!this.crackPlayed && this.phaseTicks >= BUILDUP_TICKS) {
			this.crackPlayed = true;
			// The crack: sharp "spotted" tell, same stinger the scan beat uses.
			this.drone.playSound(P7Sounds.SCAN_STING.get(), 1.2f, 1.0f);
			if (this.drone.getWorld() instanceof ServerWorld serverWorld) {
				serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
						this.disturbancePos.x, this.disturbancePos.y + 1.0, this.disturbancePos.z,
						12, 0.3, 0.3, 0.3, 0.05);
			}
			this.phase = Phase.SEARCHING;
			this.phaseTicks = 0;
			this.drone.setAlertState(AlertState.SEARCHING);
			this.drone.getNavigation().startMovingTo(
					this.disturbancePos.x, this.disturbancePos.y, this.disturbancePos.z, SEARCH_SPEED);
		}
	}

	private void tickSearching() {
		this.drone.getLookControl().lookAt(this.disturbancePos.x, this.disturbancePos.y, this.disturbancePos.z);
		this.phaseTicks++;
		if (this.drone.getNavigation().isIdle()) {
			this.drone.getNavigation().startMovingTo(
					this.disturbancePos.x, this.disturbancePos.y, this.disturbancePos.z, SEARCH_SPEED);
		}
		boolean arrived = this.drone.getPos().squaredDistanceTo(this.disturbancePos) <= ARRIVAL_RADIUS * ARRIVAL_RADIUS;
		if (arrived || this.phaseTicks >= SEARCH_TIMEOUT_TICKS) {
			this.identify();
		}
	}

	/** Arrived (or gave up looking): find out what was actually there. */
	private void identify() {
		this.drone.getNavigation().stop();
		PlayerEntity player = this.drone.getWorld().getClosestPlayer(
				this.disturbancePos.x, this.disturbancePos.y, this.disturbancePos.z, IDENTIFY_RANGE, true);
		if (player != null && !player.isSpectator() && !player.isCreative() && this.drone.canSee(player)) {
			// Confirmed hostile: hand off to the acquisition beat and whatever
			// scan/combat goal reads getTarget() from here.
			this.drone.setTarget(player);
			this.done = true;
			return;
		}
		// False alarm — it was just a mob (or nothing's there any more).
		this.phase = Phase.LINGERING;
		this.phaseTicks = 0;
	}

	private void tickLingering() {
		this.drone.getLookControl().lookAt(this.disturbancePos.x, this.disturbancePos.y, this.disturbancePos.z);
		this.phaseTicks++;
		if (this.phaseTicks >= LINGER_TICKS) {
			this.phase = Phase.RETURNING;
			this.phaseTicks = 0;
			// Decaying, not gone: one rung back down the ramp.
			this.drone.setAlertState(AlertState.SUSPICIOUS);
			this.drone.getNavigation().startMovingTo(this.homePos.x, this.homePos.y, this.homePos.z, RETURN_SPEED);
		}
	}

	private void tickReturning() {
		this.phaseTicks++;
		if (this.drone.getNavigation().isIdle()) {
			this.drone.getNavigation().startMovingTo(this.homePos.x, this.homePos.y, this.homePos.z, RETURN_SPEED);
		}
		boolean arrived = this.drone.getPos().squaredDistanceTo(this.homePos) <= RETURN_ARRIVAL_RADIUS * RETURN_ARRIVAL_RADIUS;
		if (arrived || this.phaseTicks >= RETURN_TIMEOUT_TICKS) {
			this.drone.setAlertState(AlertState.UNAWARE);
			this.done = true;
		}
	}

	@Override
	public void stop() {
		if (!this.done
				&& (this.drone.getAlertState() == AlertState.SUSPICIOUS || this.drone.getAlertState() == AlertState.SEARCHING)) {
			// Interrupted mid-arc (scrambled, forced into retreat, etc.) —
			// don't leave the ramp stuck partway up.
			this.drone.setAlertState(AlertState.UNAWARE);
		}
		this.drone.getNavigation().stop();
		this.watched = null;
		this.cooldownTicks = RETRIGGER_COOLDOWN_TICKS;
	}
}
