package dev.rheava.program7.entity.ai;

import java.util.EnumSet;
import java.util.List;

import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.entity.ScoutCarEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * The scout car's whole job: find you, shadow you, and if it keeps eyes on
 * you long enough, paint you for every other unit in the area. It never
 * fights — this is the goal that hands the fight off to something that will.
 *
 * <p>It holds a standoff band rather than closing in: too far and it can't
 * keep watching, too close and you can shoot it down before it reports
 * anything. Break line of sight and its paint timer resets; hold sight on it
 * long enough uninterrupted and every combat-capable drone in earshot gets
 * your position.
 */
public class SpotTargetGoal extends Goal {
	private static final double DETECTION_RANGE = 32.0;
	/** Path toward the target beyond this distance. */
	private static final double APPROACH_DISTANCE = 24.0;
	/** Back off if the target closes inside this distance. */
	private static final double STANDOFF_DISTANCE = 12.0;
	private static final double LEASH_RANGE = 40.0;
	private static final int PAINT_TICKS = 100;
	private static final int PAINT_COOLDOWN = 600;
	private static final double ALERT_RANGE = 48.0;

	private final ScoutCarEntity scout;
	@Nullable
	private PlayerEntity target;
	private int losTicks;
	private int paintCooldown;

	public SpotTargetGoal(ScoutCarEntity scout) {
		this.scout = scout;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.scout.isRetreating() || this.scout.isScrambled()) {
			return false;
		}
		PlayerEntity player = this.scout.getWorld().getClosestPlayer(this.scout, DETECTION_RANGE);
		if (player == null || player.isSpectator() || player.isCreative() || !this.scout.canSee(player)) {
			return false;
		}
		this.target = player;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return this.target != null
				&& this.target.isAlive()
				&& !this.target.isSpectator()
				&& !this.scout.isRetreating()
				&& this.scout.squaredDistanceTo(this.target) < LEASH_RANGE * LEASH_RANGE;
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.losTicks = 0;
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}
		this.scout.getLookControl().lookAt(this.target, 30.0f, 30.0f);

		double distanceSq = this.scout.squaredDistanceTo(this.target);
		if (distanceSq > APPROACH_DISTANCE * APPROACH_DISTANCE) {
			this.scout.getNavigation().startMovingTo(this.target, 1.0);
		} else if (distanceSq < STANDOFF_DISTANCE * STANDOFF_DISTANCE) {
			this.backAway();
		} else {
			this.scout.getNavigation().stop();
		}

		if (this.paintCooldown > 0) {
			this.paintCooldown--;
		}

		if (this.scout.canSee(this.target)) {
			this.losTicks++;
			if (this.losTicks >= PAINT_TICKS && this.paintCooldown <= 0) {
				this.paint(this.target);
				this.losTicks = 0;
				this.paintCooldown = PAINT_COOLDOWN;
			}
		} else {
			this.losTicks = 0;
		}
	}

	private void backAway() {
		if (this.scout.getNavigation().isIdle()) {
			Vec3d away = this.scout.getPos().subtract(this.target.getPos());
			if (away.lengthSquared() < 1.0E-4) {
				away = new Vec3d(1.0, 0.0, 0.0);
			}
			Vec3d dest = this.scout.getPos().add(away.normalize().multiply(6.0));
			this.scout.getNavigation().startMovingTo(dest.x, dest.y, dest.z, 1.0);
		}
	}

	/** Paint the target: alert sound, a marker column, every idle drone in range locks on. */
	private void paint(PlayerEntity player) {
		this.scout.playSound(P7Sounds.DRONE_ALERT.get(), 1.0f, 1.0f);
		if (!(this.scout.getWorld() instanceof ServerWorld world)) {
			return;
		}
		for (int i = 0; i < 6; i++) {
			world.spawnParticles(ParticleTypes.END_ROD,
					this.scout.getX(), this.scout.getY() + 1.0 + i * 0.3, this.scout.getZ(),
					1, 0.05, 0.05, 0.05, 0.01);
		}

		Box box = this.scout.getBoundingBox().expand(ALERT_RANGE);
		List<ProgramDroneEntity> drones = world.getEntitiesByClass(ProgramDroneEntity.class, box,
				e -> e != this.scout && e.getTarget() == null);
		for (ProgramDroneEntity drone : drones) {
			drone.setTarget(player);
		}
	}

	@Override
	public void stop() {
		this.target = null;
		this.losTicks = 0;
		this.scout.getNavigation().stop();
	}
}
