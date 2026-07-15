package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.director.FireMissionManager;
import dev.rheava.program7.director.FireMissionManager.FireMission;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.entity.GunboatEntity;
import dev.rheava.program7.entity.HowitzerShellEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The artillery doc's §4 naval bombardment row: "the warship's main gun as
 * shore bombardment when an inland observer feeds it a target." The deck
 * gun's own {@code DeckGunAttackGoal} already arcs a shell at a
 * self-observed target beyond direct range; this is the same reach handed
 * to the {@link FireMissionManager} for the case the hull has <em>no</em>
 * target of its own — an inland observer's relay, counter-battery, or a hot
 * dwell cell. Registered at a lower priority than {@code DeckGunAttackGoal}
 * (see {@link GunboatEntity#initGoals}), so it only actually starts when the
 * deck gun's own self-observed engagement has nothing to fire on — the
 * control-flag conflict on {@code LOOK} does the arbitration, exactly like
 * {@link HowitzerAttackGoal}'s combined self-observed/assigned shape.
 *
 * <p>Fires the same {@link HowitzerShellEntity} the deck gun's own arcing
 * mode already uses — same heavy round, same {@link BallisticSolver}
 * drag-aware solve — so there's no separate naval-shell munition to stand
 * up for this pass.
 */
public class NavalBombardmentGoal extends Goal {
	/** Slower cadence than the deck gun's own direct fire — this is the standoff bombardment role, not the close-in gun. */
	private static final int FIRE_INTERVAL = 100;
	private static final double LAUNCH_VELOCITY_Y = 2.3;
	/** Mirrors {@link HowitzerShellEntity}'s own gravity so the ballistic solve lands the round on the aim point. */
	private static final double SHELL_GRAVITY = 0.045;
	private static final int DRY_FIRE_CLICK_INTERVAL_TICKS = 40;

	private final GunboatEntity shooter;
	private int cooldown;
	private int dryFireCooldown;

	public NavalBombardmentGoal(GunboatEntity shooter) {
		this.shooter = shooter;
		this.setControls(EnumSet.of(Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.shooter.isScrambled()) {
			// Matches GunAttackGoal.canStart()'s own scramble gate — without
			// this, scrambling a gunboat just swapped its DeckGunAttackGoal
			// off for this goal instead of actually silencing it (finding #6).
			return false;
		}
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

		Vec3d hullPos = this.shooter.getPos();
		double dx = aimBase.x - hullPos.x;
		double dz = aimBase.z - hullPos.z;
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance < this.shooter.indirectMinRange() || distance > this.shooter.indirectMaxRange()
				|| !this.hasClearSky()) {
			return;
		}
		if (!this.shooter.consumeRound()) {
			this.tickDryFireClick();
			return;
		}
		double spread = manager.currentSpread(mission, hullPos);
		this.fire(world, aimBase, spread);
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

	/** Same "needs open sky over the gun" rule as every other tube. */
	private boolean hasClearSky() {
		BlockPos base = this.shooter.getBlockPos();
		for (int dy = 1; dy <= 5; dy++) {
			if (!this.shooter.getWorld().getBlockState(base.up(dy)).isAir()) {
				return false;
			}
		}
		return true;
	}

	private void fire(ServerWorld world, Vec3d aimBase, double spread) {
		Vec3d muzzle = this.shooter.getPos().add(0.0, 3.0, 0.0);

		this.shooter.playSound(P7Sounds.MORTAR_FIRE.get(), 2.0f, 0.6f);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 10, 0.3, 0.15, 0.3, 0.03);
		world.spawnParticles(ParticleTypes.CLOUD, muzzle.x, muzzle.y, muzzle.z, 6, 0.25, 0.1, 0.25, 0.02);

		Vec3d aim = aimBase.add(
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * spread,
				0.0,
				(this.shooter.getRandom().nextDouble() * 2.0 - 1.0) * spread);

		HowitzerShellEntity shell = new HowitzerShellEntity(world, this.shooter);
		shell.setPosition(muzzle.x, muzzle.y, muzzle.z);

		Vec3d launch = BallisticSolver.solve(muzzle, aim, LAUNCH_VELOCITY_Y, SHELL_GRAVITY);
		shell.setVelocity(launch.x, launch.y, launch.z);
		world.spawnEntity(shell);
	}
}
