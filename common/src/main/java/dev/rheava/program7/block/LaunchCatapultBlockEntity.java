package dev.rheava.program7.block;

import java.util.Map;

import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.director.Resources;
import dev.rheava.program7.entity.AirUAVEntity;
import dev.rheava.program7.registry.P7BlockEntities;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The catapult's brain. Every few seconds it checks its own footprint for a
 * live spotter upstairs; if the base has gone blind AND the Director's
 * ledger can pay, it slings a fresh {@link AirUAVEntity} off its facing.
 *
 * <p>No ledger, no launch — the cooldown just doubles and it tries again a
 * bit later. Nothing here is worth persisting across a reload: a missed
 * launch window costs nothing but time.
 */
public class LaunchCatapultBlockEntity extends BlockEntity {
	private static final int EVALUATE_INTERVAL = 200;
	private static final int FAILED_LAUNCH_COOLDOWN = 600;
	/** How far out this catapult looks for an existing live spotter. */
	private static final double SPOTTER_SEARCH_RADIUS = 64.0;
	private static final Map<String, Integer> UAV_COST = Map.of(
			Resources.IRON, 5, Resources.COPPER, 3, Resources.REDSTONE, 2);

	/** In-memory only: a failed launch just means try again a bit later. */
	private int cooldown = EVALUATE_INTERVAL;

	public LaunchCatapultBlockEntity(BlockPos pos, BlockState state) {
		super(P7BlockEntities.LAUNCH_CATAPULT.get(), pos, state);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, LaunchCatapultBlockEntity catapult) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		if (--catapult.cooldown > 0) {
			return;
		}
		catapult.cooldown = EVALUATE_INTERVAL;
		catapult.evaluate(serverWorld, pos, state);
	}

	private void evaluate(ServerWorld world, BlockPos pos, BlockState state) {
		Box footprint = new Box(pos).expand(SPOTTER_SEARCH_RADIUS);
		int spotters = world.getEntitiesByClass(AirUAVEntity.class, footprint, e -> true).size();
		if (spotters >= 1) {
			return;
		}
		if (ProgramDirectorState.get(world).tryConsume(UAV_COST)) {
			this.launch(world, pos, state);
		} else {
			// Ledger can't pay yet: wait longer than the usual patrol check before retrying.
			this.cooldown = FAILED_LAUNCH_COOLDOWN;
		}
	}

	private void launch(ServerWorld world, BlockPos pos, BlockState state) {
		AirUAVEntity uav = P7Entities.AIR_UAV.get().create(world);
		if (uav == null) {
			return;
		}
		Direction facing = state.get(LaunchCatapultBlock.FACING);
		Vec3d direction = Vec3d.of(facing.getVector());

		uav.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
				facing.asRotation(), 0.0f);
		uav.setVelocity(direction.multiply(1.2).add(0.0, 0.5, 0.0));
		uav.setHomePos(pos);
		world.spawnEntity(uav);

		world.playSound(null, pos, P7Sounds.CATAPULT_LAUNCH.get(), SoundCategory.BLOCKS, 1.5f, 1.0f);
		for (int i = 1; i <= 5; i++) {
			double dist = i * 0.6;
			world.spawnParticles(ParticleTypes.CLOUD,
					pos.getX() + 0.5 + direction.x * dist, pos.getY() + 1.2, pos.getZ() + 0.5 + direction.z * dist,
					1, 0.05, 0.05, 0.05, 0.01);
		}
	}
}
