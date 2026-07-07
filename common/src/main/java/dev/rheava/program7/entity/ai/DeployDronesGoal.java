package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.entity.GroundDroneEntity;
import dev.rheava.program7.entity.IFVEntity;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * The IFV's drone bay: once it's actually got a target locked, it rolls out
 * a pair of ground drones to screen it — the light-drone fireteam the unit
 * bible says this thing exists to haul forward. An escort headcount and a
 * long cooldown keep it from flooding the field with backup.
 *
 * <p>Instant-action goal: it does its whole job inside {@link #start()} and
 * never has anything left to do, so {@link #shouldContinue()} is always
 * {@code false}.
 */
public class DeployDronesGoal extends Goal {
	private static final double TARGET_RANGE = 24.0;
	private static final double ESCORT_CHECK_RANGE = 16.0;
	private static final int MAX_ESCORTS = 2;
	private static final int DEPLOY_COOLDOWN = 1200;
	private static final double SPAWN_OFFSET = 1.5;

	private final IFVEntity ifv;
	private int cooldown;

	public DeployDronesGoal(IFVEntity ifv) {
		this.ifv = ifv;
		this.setControls(EnumSet.noneOf(Goal.Control.class));
	}

	@Override
	public boolean canStart() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}
		LivingEntity target = this.ifv.getTarget();
		if (target == null || !target.isAlive() || this.ifv.distanceTo(target) > TARGET_RANGE) {
			return false;
		}
		if (!(this.ifv.getWorld() instanceof ServerWorld world)) {
			return false;
		}
		Box escortBox = this.ifv.getBoundingBox().expand(ESCORT_CHECK_RANGE);
		int escorts = world.getEntitiesByClass(GroundDroneEntity.class, escortBox, e -> true).size();
		return escorts < MAX_ESCORTS;
	}

	@Override
	public boolean shouldContinue() {
		return false;
	}

	@Override
	public void start() {
		this.cooldown = DEPLOY_COOLDOWN;
		if (!(this.ifv.getWorld() instanceof ServerWorld world)) {
			return;
		}
		LivingEntity target = this.ifv.getTarget();

		// Roll out just behind the IFV, opposite the way it's facing, so the
		// bay doesn't spit drones directly into its own line of fire.
		float yawRad = this.ifv.getYaw() * ((float) Math.PI / 180.0f);
		Vec3d facing = new Vec3d(-MathHelper.sin(yawRad), 0.0, MathHelper.cos(yawRad));
		Vec3d behind = this.ifv.getPos().subtract(facing.multiply(SPAWN_OFFSET));

		for (int i = 0; i < MAX_ESCORTS; i++) {
			GroundDroneEntity drone = P7Entities.GROUND_DRONE.get().create(world);
			if (drone == null) {
				continue;
			}
			double sideOffset = i == 0 ? -0.6 : 0.6;
			drone.refreshPositionAndAngles(behind.x + sideOffset, this.ifv.getY(), behind.z,
					this.ifv.getYaw(), 0.0f);
			drone.setHomePos(this.ifv.getBlockPos());
			if (target != null) {
				drone.setTarget(target);
			}
			world.spawnEntity(drone);
		}

		this.ifv.playSound(P7Sounds.ASSEMBLER_COMPLETE.get(), 1.0f, 0.7f);
		world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
				behind.x, this.ifv.getY() + 0.5, behind.z, 10, 0.4, 0.3, 0.4, 0.01);
	}
}
