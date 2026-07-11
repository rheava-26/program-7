package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.ProgramDroneEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * Battery management: once a unit's charge (see the {@code charge}/{@code
 * isLowCharge}/{@code addCharge} hooks on {@link ProgramDroneEntity}) sags to
 * the low-charge threshold, it breaks off whatever idle behaviour it was
 * doing — wandering, exploring, mining — finds a safe nearby spot to set
 * down, lands, and holds still while it recharges back to full before
 * resuming its normal duties.
 *
 * <p>Never interrupts a fight: {@link #canStart()} and {@link #shouldContinue()}
 * both bail the instant the unit has a live target, so a low-battery unit
 * that's actually engaging keeps fighting instead of trying to peel off and
 * land mid-firefight — the brief explicitly calls this out ("don't charge
 * while engaging"). A target showing up mid-charge aborts the goal outright.
 *
 * <p>How often a unit actually needs to do this is entirely governed by
 * {@code ProgramDroneEntity#chargeDrainPerTick()} — tier-1 hardware drains
 * fast and lands often; heavier tiers override that hook to drain slowly or
 * not at all, so this same goal reads as "occasional inconvenience" on a
 * light unit and "basically never happens" on a heavy one without any
 * branching here.
 */
public class LandAndChargeGoal extends Goal {
	private static final int SEARCH_RADIUS = 12;
	private static final int SEARCH_ATTEMPTS = 10;
	private static final double ARRIVAL_RADIUS_SQ = 1.6 * 1.6;
	/** Add one point of charge every this-many ticks while landed — see ProgramDroneEntity#addCharge. */
	private static final int RECHARGE_INTERVAL_TICKS = 4;

	private final ProgramDroneEntity drone;
	@Nullable
	private BlockPos landingSpot;
	private int rechargeTimer;

	public LandAndChargeGoal(ProgramDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (!Program7.CONFIG.droneLandToCharge) {
			return false;
		}
		return this.drone.isLowCharge() && !this.drone.isCharging()
				&& this.drone.getTarget() == null
				&& !this.drone.isRetreating() && !this.drone.isScrambled();
	}

	@Override
	public boolean shouldContinue() {
		if (this.drone.getTarget() != null) {
			// Something to fight (or flee) beats finishing a recharge cycle.
			return false;
		}
		if (this.drone.isRetreating() || this.drone.isScrambled()) {
			return false;
		}
		return this.drone.isCharging() || !this.drone.isChargeFull();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.rechargeTimer = 0;
		this.landingSpot = this.findLandingSpot();
	}

	@Override
	public void tick() {
		if (this.drone.isCharging()) {
			this.tickCharging();
			return;
		}

		if (this.landingSpot == null) {
			this.landingSpot = this.findLandingSpot();
			if (this.landingSpot == null) {
				// Nowhere safe found yet (dense canopy, over water, etc.) — hold
				// station and keep trying rather than doing nothing at all.
				this.drone.getNavigation().stop();
				return;
			}
		}

		BlockPos target = this.landingSpot;
		double x = target.getX() + 0.5;
		double y = target.getY() + 1.0;
		double z = target.getZ() + 0.5;
		this.drone.getLookControl().lookAt(x, y, z);
		if (this.drone.getNavigation().isIdle()) {
			this.drone.getNavigation().startMovingTo(x, y, z, 1.0);
		}

		if (this.drone.getPos().squaredDistanceTo(x, y, z) <= ARRIVAL_RADIUS_SQ) {
			this.drone.getNavigation().stop();
			this.drone.setCharging(true);
		}
	}

	private void tickCharging() {
		this.drone.getNavigation().stop();
		this.rechargeTimer++;
		if (this.rechargeTimer >= RECHARGE_INTERVAL_TICKS) {
			this.rechargeTimer = 0;
			this.drone.addCharge(1);
			if (this.drone.getWorld() instanceof ServerWorld world && world.getTime() % 20 == 0) {
				world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
						this.drone.getX(), this.drone.getY() + this.drone.getHeight() * 0.5, this.drone.getZ(),
						3, 0.25, 0.25, 0.25, 0.02);
			}
		}
		if (this.drone.isChargeFull()) {
			this.drone.setCharging(false);
		}
	}

	/** Walk outward from the drone and drop straight down to the first solid footing with headroom above it. */
	@Nullable
	private BlockPos findLandingSpot() {
		Random random = this.drone.getRandom();
		BlockPos origin = this.drone.getBlockPos();
		for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
			BlockPos candidate = origin.add(
					random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS,
					0,
					random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS);
			BlockPos ground = this.findGroundBelow(candidate);
			if (ground != null) {
				return ground;
			}
		}
		return null;
	}

	@Nullable
	private BlockPos findGroundBelow(BlockPos candidate) {
		BlockPos.Mutable pos = candidate.mutableCopy();
		int bottom = Math.max(this.drone.getWorld().getBottomY(), candidate.getY() - 24);
		while (pos.getY() > bottom) {
			if (!this.drone.getWorld().getBlockState(pos).isAir()
					&& this.drone.getWorld().getBlockState(pos.up()).isAir()
					&& this.drone.getWorld().getBlockState(pos.up(2)).isAir()) {
				return pos.up().toImmutable();
			}
			pos.move(0, -1, 0);
		}
		return null;
	}

	@Override
	public void stop() {
		this.drone.setCharging(false);
		this.landingSpot = null;
		this.drone.getNavigation().stop();
	}
}
