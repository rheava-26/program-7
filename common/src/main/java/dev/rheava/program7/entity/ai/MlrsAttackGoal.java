package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.director.FireMissionManager;
import dev.rheava.program7.director.FireMissionManager.FireMission;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.entity.MlrsLauncherEntity;
import dev.rheava.program7.entity.MlrsRocketEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The MLRS launcher's ripple fire: not one round on a timer like the
 * howitzer/mortar, but a stuttering burst of {@link #ROUNDS_PER_SALVO}
 * rockets fired a few ticks apart, then a long reload — the doc's "a
 * stutter of launches, a few seconds of silence, then a wall of impacts
 * arriving together." Each rocket in the salvo gets its own independent
 * scatter inside the mission's (wide) error radius, so the ripple lands as
 * a spread pattern across the target area rather than one tight group.
 *
 * <p>Same {@link FireMissionManager} client shape as {@link
 * HowitzerAttackGoal}/{@link MortarAttackGoal}: self-observed when it has
 * its own line-of-sight target, otherwise firing on whatever the manager has
 * assigned.
 */
public class MlrsAttackGoal extends Goal {
	/** Rockets per ripple. */
	private static final int ROUNDS_PER_SALVO = 6;
	/** Ticks between rockets within one ripple — a stutter, not a single crack. */
	private static final int SALVO_STAGGER_TICKS = 4;
	/** Long reload between salvos: ~13s at 20 ticks/sec. */
	private static final int RELOAD_INTERVAL = 260;
	private static final double LAUNCH_VELOCITY_Y = 2.2;
	/** Mirrors {@link MlrsRocketEntity}'s own gravity so the ballistic solve lands rockets on the aim point. */
	private static final double SHELL_GRAVITY = 0.05;
	/** Each rocket in the ripple scatters across a wider area than the mission's raw CEP so the salvo reads as a spread pattern, not a tight group. */
	private static final double SALVO_SPREAD_MULTIPLIER = 1.6;
	/** Minimum gap between "the launcher's dry" clicks so a starved launcher doesn't spam it every tick. */
	private static final int DRY_FIRE_CLICK_INTERVAL_TICKS = 40;

	private final MlrsLauncherEntity shooter;
	private int cooldown;
	private int dryFireCooldown;
	/** Rockets left to fire in the current ripple; 0 means idle/reloading. */
	private int salvoRoundsLeft;
	/** The aim point + spread the current ripple is firing at, locked in when the salvo starts so every rocket in it lands around the same spot. */
	private Vec3d salvoAim;
	private double salvoSpread;
	private FireMission salvoMission;

	public MlrsAttackGoal(MlrsLauncherEntity shooter) {
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
		// Once a ripple has started, see it through even if the target
		// slips away mid-salvo — the rockets are already committed.
		return this.salvoRoundsLeft > 0 || this.canStart();
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

		if (this.salvoRoundsLeft > 0) {
			// Mid-ripple: keep firing on the locked-in salvo aim regardless of
			// whether the mission has since moved on — the stutter of launches
			// is already committed.
			if (this.cooldown > 0) {
				this.cooldown--;
				return;
			}
			if (!this.shooter.consumeRound()) {
				this.salvoRoundsLeft = 0;
				this.tickDryFireClick();
				this.cooldown = RELOAD_INTERVAL;
				return;
			}
			this.fireOneRocket(world, this.salvoAim, this.salvoSpread);
			manager.onShotFired(this.salvoMission);
			this.salvoRoundsLeft--;
			this.cooldown = this.salvoRoundsLeft > 0 ? SALVO_STAGGER_TICKS : RELOAD_INTERVAL;
			return;
		}

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

		// Start the ripple: lock in the aim/spread and fire the first rocket now.
		double spread = manager.currentSpread(mission, launcherPos);
		this.salvoAim = aimBase;
		this.salvoSpread = spread;
		this.salvoMission = mission;
		this.salvoRoundsLeft = ROUNDS_PER_SALVO - 1;
		this.fireOneRocket(world, aimBase, spread);
		manager.onShotFired(mission);
		this.cooldown = this.salvoRoundsLeft > 0 ? SALVO_STAGGER_TICKS : RELOAD_INTERVAL;
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

	/** Fires one rocket of the ripple at {@code aimBase}, scattered within {@code spread} widened by {@link #SALVO_SPREAD_MULTIPLIER}. */
	private void fireOneRocket(ServerWorld world, Vec3d aimBase, double spread) {
		Vec3d launch = this.shooter.getPos().add(0.0, 2.2, 0.0);

		this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 1.4f, 1.1f);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, launch.x, launch.y, launch.z, 6, 0.25, 0.1, 0.25, 0.03);

		double wideSpread = spread * SALVO_SPREAD_MULTIPLIER;
		Vec3d aim = aimBase.add(
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * wideSpread,
				0.0,
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * wideSpread);

		MlrsRocketEntity rocket = new MlrsRocketEntity(world, this.shooter);
		rocket.setPosition(launch.x, launch.y, launch.z);

		Vec3d velocity = BallisticSolver.solve(launch, aim, LAUNCH_VELOCITY_Y, SHELL_GRAVITY);
		rocket.setVelocity(velocity.x, velocity.y, velocity.z);
		world.spawnEntity(rocket);
	}
}
