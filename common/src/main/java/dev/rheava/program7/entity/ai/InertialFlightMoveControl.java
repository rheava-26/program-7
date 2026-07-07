package dev.rheava.program7.entity.ai;

import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A {@link FlightMoveControl} that smears vanilla's per-tick velocity and
 * heading changes across several ticks instead of applying them instantly,
 * scaled by an airframe {@code mass}. This is the "no instant stops, turn
 * rates scale with unit mass" design pillar: a mass-1.0 FPV-quad-style
 * airframe stays nearly as snappy as vanilla, while a heavy transport spools
 * up, banks, and coasts to a stop like it actually has inertia.
 */
public class InertialFlightMoveControl extends FlightMoveControl {
	// A mass-1 unit keeps ~35% responsiveness per tick (still feels snappy);
	// a mass-4 transport only keeps ~9%, smearing its acceleration over many ticks.
	private static final double ACCEL_BASE = 0.35;
	// A mass-1 unit may still turn up to ~40 degrees per tick; heavier units
	// get a proportionally tighter turn-rate cap so they visibly bank into turns.
	private static final float MAX_YAW_STEP_BASE = 40.0F;

	private final float mass;

	public InertialFlightMoveControl(MobEntity entity, int maxPitchChange, boolean noGravity, float mass) {
		super(entity, maxPitchChange, noGravity);
		this.mass = Math.max(1.0F, mass);
	}

	@Override
	public void tick() {
		Vec3d velocityBeforeTick = this.entity.getVelocity();
		float yawBeforeTick = this.entity.getYaw();

		super.tick();

		// Blend the vanilla-desired velocity into the pre-tick velocity rather than
		// snapping to it, so heavier airframes take longer to spool up or bleed off speed.
		double accelFraction = Math.min(1.0, ACCEL_BASE / this.mass);
		this.entity.setVelocity(velocityBeforeTick.lerp(this.entity.getVelocity(), accelFraction));

		// Independently cap how far the yaw is allowed to slew this tick, so heavy
		// units bank into turns instead of snapping to face the target instantly.
		float maxYawStep = MAX_YAW_STEP_BASE / this.mass;
		this.entity.setYaw(MathHelper.stepUnwrappedAngleTowards(yawBeforeTick, this.entity.getYaw(), maxYawStep));
	}
}
