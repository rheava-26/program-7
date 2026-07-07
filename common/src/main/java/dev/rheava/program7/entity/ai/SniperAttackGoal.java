package dev.rheava.program7.entity.ai;

import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/**
 * The Program's standoff shot: same tracer-and-hitscan bones as
 * {@link GunAttackGoal}, but this mount wants distance, not a firefight.
 * Let a target close inside knife range and it disengages instead of
 * trading — backing straight away instead of holding position.
 */
public class SniperAttackGoal extends GunAttackGoal {
	/** Inside this range the sniper flees rather than firing at all. */
	private static final double STANDOFF_DISTANCE = 12.0;
	private static final double RETREAT_DISTANCE = 10.0;
	/** Accuracy never falls below this, even at the far edge of its range. */
	private static final double MIN_HIT_CHANCE = 0.75;

	public SniperAttackGoal(ProgramDroneEntity shooter, double speed, double range,
			int fireInterval, float damage) {
		super(shooter, speed, range, fireInterval, damage);
	}

	@Override
	public void tick() {
		LivingEntity target = this.shooter.getTarget();
		if (target == null) {
			return;
		}

		if (this.shooter.distanceTo(target) < STANDOFF_DISTANCE) {
			// Too close: disengage instead of closing in. Look stays on the
			// target so it can resume firing the moment it opens the range
			// back up, but no shot goes out this tick.
			this.shooter.getLookControl().lookAt(target, 30.0f, 30.0f);
			Vec3d away = this.shooter.getPos().subtract(target.getPos());
			if (away.lengthSquared() < 1.0E-4) {
				away = new Vec3d(1.0, 0.0, 0.0);
			}
			away = away.normalize();
			Vec3d dest = this.shooter.getPos().add(away.multiply(RETREAT_DISTANCE));
			this.shooter.getNavigation().startMovingTo(dest.x, dest.y, dest.z, 1.0);
		} else {
			super.tick();
		}
	}

	@Override
	protected double hitChance(double distance) {
		// Floored version of the base falloff: the whole point of this mount
		// is that range doesn't save you from it.
		return Math.max(MIN_HIT_CHANCE, super.hitChance(distance));
	}

	@Override
	protected void playFireSound() {
		this.shooter.playSound(P7Sounds.SNIPER_FIRE.get(), 1.0f,
				1.1f + this.shooter.getRandom().nextFloat() * 0.2f);
	}
}
