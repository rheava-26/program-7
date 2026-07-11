package dev.rheava.program7.entity.ai;

import java.util.EnumSet;
import java.util.List;

import dev.rheava.program7.entity.AirUAVEntity;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

/**
 * The UAV's whole job while it circles: watch for you. A LOOK-only sibling
 * of {@link SpotTargetGoal} — it never touches the navigation, so it can run
 * happily alongside {@link CircleLoiterGoal} without the two fighting over
 * control of the airframe. Hold line of sight long enough uninterrupted and
 * it paints the target for every idle armed drone within earshot.
 */
public class UAVSpotGoal extends Goal {
	private static final double DETECTION_RANGE = 40.0;
	private static final double LEASH_RANGE = 48.0;
	private static final int PAINT_TICKS = 80;
	private static final int PAINT_COOLDOWN = 600;
	private static final double ALERT_RANGE = 64.0;

	private final AirUAVEntity uav;
	@Nullable
	private PlayerEntity target;
	private int losTicks;
	private int paintCooldown;

	public UAVSpotGoal(AirUAVEntity uav) {
		this.uav = uav;
		this.setControls(EnumSet.of(Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.uav.isScrambled()) {
			return false;
		}
		PlayerEntity player = this.uav.getWorld().getClosestPlayer(this.uav, DETECTION_RANGE);
		if (player == null || player.isSpectator() || player.isCreative() || !this.uav.canSee(player)) {
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
				&& !this.uav.isScrambled()
				&& this.uav.squaredDistanceTo(this.target) < LEASH_RANGE * LEASH_RANGE;
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.losTicks = 0;
		// Ping immediately on acquisition rather than waiting for the full
		// PAINT_TICKS hold — the UAV should audibly announce "found you" the
		// moment it spots the player, same as a regular drone locking on, not
		// only after it's held an unbroken sightline for four seconds (see #2).
		if (this.target != null && this.uav.getWorld() instanceof ServerWorld world) {
			this.announceSpotted(world, this.target);
		}
	}

	/** Play the shrill cue if nobody else has announced this player recently; otherwise just layer the hum. */
	private void announceSpotted(ServerWorld world, PlayerEntity player) {
		if (SpottedAlertCoordinator.tryAnnounceSpotted(world, player)) {
			this.uav.playSound(P7Sounds.DRONE_ALERT.get(), 1.0f, 1.0f);
		} else {
			this.uav.playSound(P7Sounds.DRONE_INTERFERENCE.get(), 0.4f, 1.2f);
		}
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}
		this.uav.getLookControl().lookAt(this.target, 30.0f, 30.0f);

		if (this.paintCooldown > 0) {
			this.paintCooldown--;
		}

		if (this.uav.canSee(this.target)) {
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

	/** Paint the target: alert sound, a marker column below the UAV, every idle drone in range locks on. */
	private void paint(PlayerEntity player) {
		if (!(this.uav.getWorld() instanceof ServerWorld world)) {
			return;
		}
		this.announceSpotted(world, player);
		for (int i = 0; i < 6; i++) {
			world.spawnParticles(ParticleTypes.END_ROD,
					this.uav.getX(), this.uav.getY() - i * 0.3, this.uav.getZ(),
					1, 0.05, 0.05, 0.05, 0.01);
		}

		Box box = this.uav.getBoundingBox().expand(ALERT_RANGE);
		List<ProgramDroneEntity> drones = world.getEntitiesByClass(ProgramDroneEntity.class, box,
				e -> e != this.uav && e.getTarget() == null);
		for (ProgramDroneEntity drone : drones) {
			drone.setTarget(player);
		}
	}

	@Override
	public void stop() {
		this.target = null;
		this.losTicks = 0;
	}
}
