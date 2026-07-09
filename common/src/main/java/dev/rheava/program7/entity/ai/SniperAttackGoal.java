package dev.rheava.program7.entity.ai;

import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
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
	protected int engage(LivingEntity target, double distance, boolean canSee) {
		// The sniper never sprays: unlike the base class it doesn't switch to
		// wide-spread suppressive fire at long range or against a lost
		// target — every round that goes out is an aimed shot at the
		// floored hitChance() below, same as before this mount's block-
		// chewing/particle wiring was added via the shared fire() path. It
		// still respects the base suppression *window* (staying "sighted"
		// on a target for a few beats after losing sight) via updateSight(),
		// just without the accuracy/spread penalty that implies elsewhere.
		if (distance > this.range) {
			return 0;
		}
		Vec3d aimPos = canSee ? target.getBoundingBox().getCenter() : this.getLastSeenPos();
		if (aimPos == null) {
			return 0;
		}
		this.fire(target, distance, aimPos, canSee ? 1.0 : SUPPRESSION_HIT_CHANCE_SCALE);
		return this.fireInterval;
	}

	@Override
	protected double hitChance(double distance) {
		// Floored version of the base falloff: the whole point of this mount
		// is that range doesn't save you from it.
		return Math.max(MIN_HIT_CHANCE, super.hitChance(distance));
	}

	@Override
	protected void playFireSound() {
		// Positional, base volume 5.0f (~volume x16 blocks of audible range,
		// so roughly an 80-block radius crack) instead of the shooter's own
		// playSound: this is the whole point of a sniper — the report should
		// carry across several chunks so the player hears it coming from far
		// outside gun range, not just a local crack. ProgramAcoustics then
		// layers travel delay, distance shaping, and occlusion on top.
		float pitch = 1.1f + this.shooter.getRandom().nextFloat() * 0.2f;
		if (this.shooter.getWorld() instanceof ServerWorld world) {
			ProgramAcoustics.emit(world, Vec3d.ofCenter(this.shooter.getBlockPos()), P7Sounds.SNIPER_FIRE.get(),
					SoundCategory.HOSTILE, 5.0f, pitch);
		}
	}
}
