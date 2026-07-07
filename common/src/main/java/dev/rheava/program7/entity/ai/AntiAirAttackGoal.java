package dev.rheava.program7.entity.ai;

import dev.rheava.program7.entity.ProgramDroneEntity;
import net.minecraft.entity.LivingEntity;

/**
 * The anti-air mount's fire control: same tracer-and-hitscan bones as
 * {@link GunAttackGoal}, but bolted flak-style instead of aimed. It cannot
 * depress onto a ground target — the whole doctrine is airborne intercept,
 * and a target that lands breaks its lock outright. Accuracy doesn't fall
 * off with range either; a flak burst either catches the target in its
 * envelope or it doesn't.
 */
public class AntiAirAttackGoal extends GunAttackGoal {
	/** Flat hit chance regardless of distance — flak, not a sniper falloff. */
	private static final double FLAK_HIT_CHANCE = 0.65;

	public AntiAirAttackGoal(ProgramDroneEntity shooter, double range, int fireInterval, float damage) {
		super(shooter, 0.0, range, fireInterval, damage);
	}

	@Override
	public boolean canStart() {
		if (!super.canStart()) {
			return false;
		}
		LivingEntity target = this.shooter.getTarget();
		return target != null && !target.isOnGround();
	}

	@Override
	public boolean shouldContinue() {
		if (!super.shouldContinue()) {
			return false;
		}
		LivingEntity target = this.shooter.getTarget();
		return target != null && !target.isOnGround();
	}

	@Override
	protected double hitChance(double distance) {
		return FLAK_HIT_CHANCE;
	}
}
