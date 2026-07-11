package dev.rheava.program7.entity.ai;

import java.util.EnumSet;
import java.util.List;

import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.entity.ReconHelicopterEntity;
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
 * The helicopter's whole reason to exist: find you, pin a searchlight beam
 * on you, and hold it there. A LOOK-only goal — it never touches navigation,
 * so it rides alongside {@link OrbitTargetGoal} without the two fighting
 * over the airframe.
 *
 * <p>When the helicopter has no target it hunts, picking the nearest
 * non-creative, non-spectator player it can actually see within range. Once
 * it has one (whether from its own hunting or handed to it by {@code
 * RevengeGoal} after being shot) it draws the beam every tick it holds line
 * of sight. Sixty continuous ticks of LOS and it paints: every idle armed
 * drone within earshot locks on. Unlike the UAV's one-shot paint, the
 * searchlight doesn't let go — it keeps tracking and repaints on a timer for
 * as long as it holds the same target, only dropping the target if LOS stays
 * broken for longer than that same sixty-tick window.
 */
public class SearchlightSpotGoal extends Goal {
	private static final double DETECTION_RANGE = 48.0;
	private static final int PAINT_LOS_TICKS = 60;
	private static final int LOS_DROP_TICKS = 60;
	private static final int REPAINT_INTERVAL_TICKS = 300;
	private static final double ALERT_RANGE = 64.0;

	private static final double BEAM_SEGMENT_SPACING = 1.5;
	private static final double BEAM_JITTER = 0.1;
	private static final double POOL_RADIUS = 1.0;
	private static final int POOL_PARTICLE_COUNT = 8;

	private final ReconHelicopterEntity heli;
	@Nullable
	private LivingEntity trackedTarget;
	private int losTicks;
	private int noLosTicks;
	private boolean painted;
	private int repaintTimer;

	public SearchlightSpotGoal(ReconHelicopterEntity heli) {
		this.heli = heli;
		this.setControls(EnumSet.of(Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		return !this.heli.isScrambled();
	}

	@Override
	public boolean shouldContinue() {
		return !this.heli.isScrambled();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = this.heli.getTarget();
		if (target == null) {
			this.trackedTarget = null;
			this.resetTrackingState();
			target = this.findNearestPlayer();
			if (target == null) {
				return;
			}
			// Feeds the vanilla target slot, which is what OrbitTargetGoal
			// (and RevengeGoal, the other way around) also key off of.
			this.heli.setTarget(target);
		}

		if (target != this.trackedTarget) {
			boolean freshAcquisition = this.trackedTarget == null;
			this.trackedTarget = target;
			this.resetTrackingState();
			// Ping immediately on a genuinely new lock (not a revenge-goal target
			// swap) rather than waiting on the full LOS-hold paint — the heli
			// should announce "found you" the moment it acquires, same as any
			// other spotter (see #2).
			if (freshAcquisition && target instanceof PlayerEntity player
					&& this.heli.getWorld() instanceof ServerWorld world) {
				this.announceSpotted(world, player);
			}
		}

		if (!this.isValidTarget(target)) {
			this.heli.setTarget(null);
			this.trackedTarget = null;
			this.resetTrackingState();
			return;
		}

		if (this.heli.canSee(target)) {
			this.noLosTicks = 0;
			this.losTicks++;
			this.heli.getLookControl().lookAt(target, 30.0f, 30.0f);
			this.drawSearchlightBeam(target);

			if (!this.painted) {
				if (this.losTicks >= PAINT_LOS_TICKS) {
					this.paint(target);
					this.painted = true;
					this.repaintTimer = 0;
				}
			} else {
				this.repaintTimer++;
				if (this.repaintTimer >= REPAINT_INTERVAL_TICKS) {
					this.paint(target);
					this.repaintTimer = 0;
				}
			}
		} else {
			this.losTicks = 0;
			this.noLosTicks++;
			if (this.noLosTicks > LOS_DROP_TICKS) {
				this.heli.setTarget(null);
				this.trackedTarget = null;
				this.resetTrackingState();
			}
		}
	}

	private void resetTrackingState() {
		this.losTicks = 0;
		this.noLosTicks = 0;
		this.painted = false;
		this.repaintTimer = 0;
	}

	@Nullable
	private PlayerEntity findNearestPlayer() {
		PlayerEntity player = this.heli.getWorld().getClosestPlayer(this.heli, DETECTION_RANGE);
		if (player == null || player.isSpectator() || player.isCreative() || !this.heli.canSee(player)) {
			return null;
		}
		return player;
	}

	private boolean isValidTarget(LivingEntity target) {
		if (!target.isAlive()) {
			return false;
		}
		if (target instanceof PlayerEntity player && (player.isSpectator() || player.isCreative())) {
			return false;
		}
		return true;
	}

	/** Draws the beam from the helicopter down to the target, plus a pool of light at their feet. */
	private void drawSearchlightBeam(LivingEntity target) {
		if (!(this.heli.getWorld() instanceof ServerWorld world)) {
			return;
		}
		Vec3d start = this.heli.getPos().add(0.0, this.heli.getHeight() * 0.5, 0.0);
		Vec3d end = target.getPos();
		double length = start.distanceTo(end);
		int segments = Math.max(1, (int) Math.round(length / BEAM_SEGMENT_SPACING));
		for (int i = 0; i <= segments; i++) {
			Vec3d point = start.lerp(end, (double) i / segments);
			world.spawnParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
					1, BEAM_JITTER, BEAM_JITTER, BEAM_JITTER, 0.0);
		}

		// The pool of light where the beam lands.
		for (int i = 0; i < POOL_PARTICLE_COUNT; i++) {
			double angle = (Math.PI * 2.0 / POOL_PARTICLE_COUNT) * i;
			double x = end.x + Math.cos(angle) * POOL_RADIUS;
			double z = end.z + Math.sin(angle) * POOL_RADIUS;
			world.spawnParticles(ParticleTypes.END_ROD, x, end.y + 0.05, z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/** Play the shrill cue if nobody else has announced this player recently; otherwise just layer the hum. */
	private void announceSpotted(ServerWorld world, PlayerEntity player) {
		if (SpottedAlertCoordinator.tryAnnounceSpotted(world, player)) {
			this.heli.playSound(P7Sounds.DRONE_ALERT.get(), 1.5f, 1.0f);
		} else {
			this.heli.playSound(P7Sounds.DRONE_INTERFERENCE.get(), 0.5f, 1.2f);
		}
	}

	/** Paint the target: alert sound, every idle armed drone in range locks on. */
	private void paint(LivingEntity target) {
		if (!(this.heli.getWorld() instanceof ServerWorld world)) {
			return;
		}
		if (target instanceof PlayerEntity player) {
			this.announceSpotted(world, player);
		} else {
			this.heli.playSound(P7Sounds.DRONE_ALERT.get(), 1.5f, 1.0f);
		}
		Box box = this.heli.getBoundingBox().expand(ALERT_RANGE);
		List<ProgramDroneEntity> drones = world.getEntitiesByClass(ProgramDroneEntity.class, box,
				drone -> drone != this.heli && drone.getTarget() == null);
		for (ProgramDroneEntity drone : drones) {
			drone.setTarget(target);
		}
	}
}
