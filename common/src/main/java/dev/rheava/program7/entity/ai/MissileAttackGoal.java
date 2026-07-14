package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.director.FireMissionManager;
import dev.rheava.program7.director.FireMissionManager.FireMission;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.entity.GuidedMissileEntity;
import dev.rheava.program7.entity.MissileLauncherEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The missile launcher's fire control: same {@link FireMissionManager}
 * client shape as every other tube (self-observed / assigned split, §2
 * acquisition), but the launch itself barely bothers with the ballistic
 * solve — a missile flies (near) flat and steers itself in (see {@link
 * GuidedMissileEntity}), so this only needs a rough initial heading. The
 * one thing that matters here is <em>whether the mission is currently
 * spotted</em>: only then does the missile get handed a live guidance
 * target ({@link FireMission#targetPlayerId()}), so losing the observer
 * degrades a missile exactly like it degrades every cheaper tube — a
 * missile fired blind flies dumb to the stale point instead of homing.
 */
public class MissileAttackGoal extends Goal {
	/** Very long reload — each shot is dear, per the doc's "far more expensive per shot." */
	private static final int FIRE_INTERVAL = 400;
	private static final double LAUNCH_SPEED = 1.6;
	/** Slight upward launch so the missile arcs over just enough to read its falling-whistle cue before it levels off and steers in. */
	private static final double LAUNCH_VELOCITY_Y = 0.4;
	private static final int DRY_FIRE_CLICK_INTERVAL_TICKS = 40;

	private final MissileLauncherEntity shooter;
	private int cooldown;
	private int dryFireCooldown;

	public MissileAttackGoal(MissileLauncherEntity shooter) {
		this.shooter = shooter;
		this.setControls(EnumSet.of(Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		LivingEntity target = this.shooter.getTarget();
		if (target != null && target.isAlive()) {
			return true;
		}
		return this.assignedTarget() != null;
	}

	@Override
	public boolean shouldContinue() {
		return this.canStart();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
			return;
		}
		FireMissionManager manager = ProgramDirectorState.get(world).getFireMissionManager();
		long now = world.getTime();

		LivingEntity target = this.shooter.getTarget();
		FireMission mission;
		Vec3d aimBase;
		if (target != null && target.isAlive()) {
			mission = manager.updateSelfObserved(this.shooter, target, now);
			aimBase = target.getPos();
			this.shooter.getLookControl().lookAt(target, 30.0f, 30.0f);
		} else {
			mission = manager.missionFor(this.shooter);
			Vec3d assigned = mission == null ? null : mission.targetPos();
			if (assigned == null) {
				return;
			}
			aimBase = assigned;
			this.shooter.getLookControl().lookAt(aimBase.x, aimBase.y, aimBase.z);
		}

		if (this.cooldown > 0) {
			this.cooldown--;
			return;
		}

		Vec3d launcherPos = this.shooter.getPos();
		double dx = aimBase.x - launcherPos.x;
		double dz = aimBase.z - launcherPos.z;
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance < this.shooter.indirectMinRange() || distance > this.shooter.indirectMaxRange()
				|| !this.hasClearSky()) {
			return;
		}
		if (!this.shooter.consumeRound()) {
			this.tickDryFireClick();
			return;
		}
		double spread = manager.currentSpread(mission, launcherPos);
		this.fire(world, mission, aimBase, spread);
		manager.onShotFired(mission);
		this.cooldown = FIRE_INTERVAL;
	}

	private Vec3d assignedTarget() {
		if (!(this.shooter.getWorld() instanceof ServerWorld world)) {
			return null;
		}
		FireMission mission = ProgramDirectorState.get(world).getFireMissionManager().missionFor(this.shooter);
		return mission == null ? null : mission.targetPos();
	}

	private void tickDryFireClick() {
		if (this.dryFireCooldown > 0) {
			this.dryFireCooldown--;
			return;
		}
		this.dryFireCooldown = DRY_FIRE_CLICK_INTERVAL_TICKS;
		this.shooter.playSound(P7Sounds.WEAPON_DRY_FIRE.get(), 0.6f,
				0.9f + this.shooter.getRandom().nextFloat() * 0.15f);
	}

	private boolean hasClearSky() {
		BlockPos base = this.shooter.getBlockPos();
		for (int dy = 1; dy <= 5; dy++) {
			if (!this.shooter.getWorld().getBlockState(base.up(dy)).isAir()) {
				return false;
			}
		}
		return true;
	}

	private void fire(ServerWorld world, FireMission mission, Vec3d aimBase, double spread) {
		Vec3d launch = this.shooter.getPos().add(0.0, 2.4, 0.0);

		this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 1.8f, 1.3f);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, launch.x, launch.y, launch.z, 12, 0.3, 0.15, 0.3, 0.04);

		// The launch aim is a rough heading only — the missile steers the
		// rest of the way in once it's airborne, so a small initial jitter
		// inside the (already tight) CEP doesn't matter much.
		Vec3d aim = aimBase.add(
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * spread,
				0.0,
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * spread);

		GuidedMissileEntity missile = new GuidedMissileEntity(world, this.shooter);
		missile.setPosition(launch.x, launch.y, launch.z);

		Vec3d delta = aim.subtract(launch);
		double horizontal = Math.max(Math.sqrt(delta.x * delta.x + delta.z * delta.z), 1.0E-4);
		Vec3d headingXZ = new Vec3d(delta.x / horizontal, 0.0, delta.z / horizontal);
		missile.setVelocity(headingXZ.x * LAUNCH_SPEED, LAUNCH_VELOCITY_Y, headingXZ.z * LAUNCH_SPEED);

		// Only a currently-spotted mission hands over a live guidance target —
		// an unobserved (stale/dwell/counter-battery) mission fires the
		// missile dumb at the fixed point, same "losing the observer hurts"
		// rule every cheaper tube already follows.
		if (mission.isObserved()) {
			missile.setGuidanceTarget(mission.targetPlayerId());
		}
		world.spawnEntity(missile);
	}
}
