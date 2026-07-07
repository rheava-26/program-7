package dev.rheava.program7.entity.ai;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A ship's helm, not an autopilot. Where {@link InertialFlightMoveControl}
 * smears vanilla's own velocity math across ticks, this one replaces it
 * outright: a hull afloat doesn't strafe, doesn't hop over ledges, and
 * doesn't spin to face a target — it puts the rudder over and comes
 * around, slowly, along whichever way the bow happens to be pointed.
 *
 * <ul>
 *   <li><b>Huge turning circle:</b> yaw steps toward the heading at a hard
 *       cap per tick instead of vanilla's ~90&deg; snap.</li>
 *   <li><b>Gradual spool-up:</b> horizontal velocity is lerped toward the
 *       thrust vector rather than set outright, so getting up to speed (or
 *       stopping) takes a couple of seconds of visible acceleration.</li>
 *   <li><b>Dead in the water on land:</b> if the hull isn't touching water
 *       there is no thrust at all — see {@code GunboatEntity} for how
 *       beaching is enforced on top of this.</li>
 * </ul>
 *
 * <p>The vertical axis is deliberately left alone here; the entity's own
 * buoyancy handling owns Y so this class never fights it.
 */
public class NavalMoveControl extends MoveControl {
	// A supertanker's turning circle: even committed to a heading, the bow
	// only comes around a few degrees a tick.
	private static final float MAX_YAW_STEP = 3.0F;
	// Fraction of the gap between current and desired horizontal velocity
	// closed per tick; low enough that spooling up to speed (or coasting to
	// a stop) reads as the hull's own momentum, not a light vehicle.
	private static final double ACCEL_FRACTION = 0.08;

	public NavalMoveControl(MobEntity entity) {
		super(entity);
	}

	@Override
	public void tick() {
		if (!this.entity.isTouchingWater()) {
			// Beached: the screws have nothing to push against. Buoyancy
			// (handled by the entity itself) still owns the Y axis either way.
			return;
		}
		if (this.state != MoveControl.State.MOVE_TO) {
			return;
		}

		double dx = this.targetX - this.entity.getX();
		double dz = this.targetZ - this.entity.getZ();
		// Navigation re-calls moveTo() every tick it's still following a path,
		// so resetting to WAIT here (matching vanilla MoveControl) is safe.
		this.state = MoveControl.State.WAIT;
		if (dx * dx + dz * dz < (double) MoveControl.REACHED_DESTINATION_DISTANCE_SQUARED) {
			return;
		}

		float desiredYaw = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
		float newYaw = MathHelper.stepUnwrappedAngleTowards(this.entity.getYaw(), desiredYaw, MAX_YAW_STEP);
		this.entity.setYaw(newYaw);
		this.entity.bodyYaw = newYaw;

		double speedPerTick = this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		float yawRad = newYaw * (float) (Math.PI / 180.0);
		// Minecraft's forward vector for a given yaw is (-sin, cos) in (x, z)
		// (see Entity#getRotationVector) — thrust along the bow, not a
		// beeline for the target, is the whole point of the turn-rate cap above.
		Vec3d current = this.entity.getVelocity();
		Vec3d desired = new Vec3d(-MathHelper.sin(yawRad) * speedPerTick, current.y, MathHelper.cos(yawRad) * speedPerTick);
		this.entity.setVelocity(current.lerp(desired, ACCEL_FRACTION));
	}
}
