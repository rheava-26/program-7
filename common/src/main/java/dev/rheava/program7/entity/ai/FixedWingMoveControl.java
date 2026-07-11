package dev.rheava.program7.entity.ai;

import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A {@link FlightMoveControl} for airframes that actually fly like fixed-wing
 * planes instead of vanilla's hover-capable flight model — the "shulker with
 * wings" problem, where a flier can spin to face a target in place and stop
 * dead in the air. This control keeps vanilla's own pitch-toward-target and
 * gravity-disabling logic (still driven by {@code maxPitchChange} exactly
 * like a normal {@link FlightMoveControl}) but layers two things on top:
 *
 * <ul>
 *   <li><b>Always airborne, never hovering:</b> if horizontal airspeed sags
 *       below {@link #MIN_AIRSPEED}, thrust is added straight along the
 *       nose so the airframe never decelerates to a stop mid-air.</li>
 *   <li><b>Yaw-only steering, hard-capped:</b> however far vanilla's own
 *       heading pursuit wanted to swing the yaw this tick, that swing is
 *       reclamped to a small slew ({@link #MAX_YAW_STEP}) — a wide banking
 *       turn circle that physically cannot pivot in place, the fixed-wing
 *       equivalent of {@link InertialFlightMoveControl}'s mass-scaled yaw
 *       cap.</li>
 * </ul>
 */
public class FixedWingMoveControl extends FlightMoveControl {
	/** Horizontal speed floor: below this, thrust kicks in along the nose so the airframe never hovers. */
	private static final double MIN_AIRSPEED = 0.5;
	/** Acceleration applied along yaw whenever airspeed sags below the floor — a fast cruise, not a crawl. */
	private static final double CRUISE_THRUST = 0.18;
	/**
	 * Hard cap on yaw slew per tick: a wide, physically-plausible banking turn
	 * circle, not a snap-to-face. Halved again from an earlier 5.5 — even
	 * that read as "whipping around" for a baby fixed-wing UAV, so the bank
	 * circle is now noticeably lazier/wider.
	 */
	private static final float MAX_YAW_STEP = 2.75F;

	public FixedWingMoveControl(MobEntity entity, int maxPitchChange, boolean noGravity) {
		super(entity, maxPitchChange, noGravity);
	}

	@Override
	public void tick() {
		float yawBeforeTick = this.entity.getYaw();

		// Vanilla FlightMoveControl still owns gravity-disabling and the
		// pitch-toward-target pursuit (capped at the maxPitchChange passed
		// to our constructor above) — only the yaw result gets overridden below.
		super.tick();

		// Steers by yaw only: reclamp however far vanilla wanted to swing the
		// heading this tick down to a small slew, so the airframe banks
		// through a wide turn circle instead of snapping to face the target.
		float newYaw = MathHelper.stepUnwrappedAngleTowards(yawBeforeTick, this.entity.getYaw(), MAX_YAW_STEP);
		this.entity.setYaw(newYaw);
		this.entity.bodyYaw = newYaw;

		// Fixed wings never decelerate to a hover: if horizontal airspeed has
		// sagged below the floor, add thrust straight along the (now-clamped) nose.
		Vec3d velocity = this.entity.getVelocity();
		if (velocity.horizontalLength() < MIN_AIRSPEED) {
			double yawRad = Math.toRadians(newYaw);
			double addX = -Math.sin(yawRad) * CRUISE_THRUST;
			double addZ = Math.cos(yawRad) * CRUISE_THRUST;
			this.entity.setVelocity(velocity.add(addX, 0.0, addZ));
		}
	}
}
