package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.entity.ProgramDroneEntity.AlertState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * "Something heard that." A combat unit within earshot of a loud player
 * noise — see {@link ProgramAcoustics#reportNoise} — doesn't insta-beeline
 * for the source. It hangs for a deliberate psionic-buildup beat (just
 * looking toward the sound), then drifts over to check, the same
 * dread-building "wait, what was that" idiom {@link InvestigateDisturbanceGoal}
 * uses for line-of-sight disturbances. This goal is purely about investigating
 * a *sound* though: if the drone already has an attack target it doesn't fire
 * at all, and combat/attack/retreat goals always outrank it in the selector.
 *
 * <p>If it finds nothing at the noise's position, it just peels off — this is
 * an investigation, not a lock-on. A real target showing up (line-of-sight
 * acquisition, an attack goal, etc.) always wins out from there.
 */
public class InvestigateNoiseGoal extends Goal {
	/** How far out a unit can hear a (loudness-scaled) noise report at all. */
	private static final double HEARING_RANGE = 48.0;
	/** Ticks of "wait... what was that" — the unit only looks, doesn't move yet. */
	private static final int BUILDUP_TICKS = 25;
	private static final double MOVE_SPEED = 1.0;
	/** How close counts as "arrived" at the noise's position. */
	private static final double ARRIVAL_RADIUS = 3.0;
	/** Overall budget for the whole investigation before it gives up. */
	private static final int MAX_TICKS = 200;

	private final ProgramDroneEntity drone;
	private Vec3d target = Vec3d.ZERO;
	private int buildupTicks;
	private int totalTicks;
	private boolean moving;

	public InvestigateNoiseGoal(ProgramDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.drone.getTarget() != null || this.drone.isRetreating() || this.drone.isScrambled()) {
			return false;
		}
		if (!(this.drone.getWorld() instanceof ServerWorld serverWorld)) {
			return false;
		}
		Vec3d noise = ProgramAcoustics.nearestAudibleNoise(serverWorld,
				this.drone.getX(), this.drone.getY(), this.drone.getZ(), HEARING_RANGE);
		if (noise == null) {
			return false;
		}
		this.target = noise;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		if (this.drone.getTarget() != null || this.drone.isRetreating() || this.drone.isScrambled()) {
			return false;
		}
		if (this.totalTicks >= MAX_TICKS) {
			return false;
		}
		return this.drone.getPos().squaredDistanceTo(this.target) > ARRIVAL_RADIUS * ARRIVAL_RADIUS;
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.buildupTicks = 0;
		this.totalTicks = 0;
		this.moving = false;
		this.drone.getNavigation().stop();
		this.drone.setAlertState(AlertState.SUSPICIOUS);
	}

	@Override
	public void tick() {
		this.totalTicks++;
		this.drone.getLookControl().lookAt(this.target.x, this.target.y, this.target.z);
		if (!this.moving) {
			this.buildupTicks++;
			if (this.buildupTicks >= BUILDUP_TICKS) {
				this.moving = true;
				this.drone.setAlertState(AlertState.SEARCHING);
				this.drone.getNavigation().startMovingTo(this.target.x, this.target.y, this.target.z, MOVE_SPEED);
			}
			return;
		}
		if (this.drone.getNavigation().isIdle()) {
			this.drone.getNavigation().startMovingTo(this.target.x, this.target.y, this.target.z, MOVE_SPEED);
		}
	}

	@Override
	public void stop() {
		this.drone.getNavigation().stop();
		if (this.drone.getAlertState() == AlertState.SUSPICIOUS || this.drone.getAlertState() == AlertState.SEARCHING) {
			// Didn't hand off to a real target — decay back down instead of
			// leaving the ramp stuck partway up.
			this.drone.setAlertState(AlertState.UNAWARE);
		}
		this.target = Vec3d.ZERO;
	}
}
