package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.director.HarvestTargets;
import dev.rheava.program7.entity.CargoHauler;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Find the nearest block the Program wants and cut it out of the world with
 * a standoff mining laser — visible crack stages, a beam of particles, and
 * mining noise, but no need to physically drive onto the block first (see
 * #5: "harvester drones should use a laser instead of driving all the way
 * over to mine ores").
 *
 * <p>The unit paths only as close as {@link #WORK_RANGE} and, once it has a
 * clear line to the ore from there, holds position and lasers it out over
 * {@link #MINE_TICKS} — still real time, same as the old adjacency-mining
 * did, just without the drive-up. If something's in the way (the ore is
 * around a corner, behind rock the standoff distance doesn't clear), it
 * keeps closing the distance exactly like before until it either gets a
 * clear shot or ends up back at point-blank range. Yield still goes into the
 * hopper as ledger units; nothing drops on the ground.
 */
public class MineResourceGoal extends Goal {
	private static final int SEARCH_RADIUS = 20;
	private static final int VERTICAL_RADIUS = 8;
	/** Laser standoff range — the unit only needs to get this close, not adjacent, see #5. */
	private static final double WORK_RANGE = 12.0;
	private static final int MINE_TICKS = 80;
	private static final int STUCK_LIMIT = 300;
	/** Spacing between particles along the laser beam. */
	private static final double BEAM_SEGMENT_SPACING = 1.0;
	private static final double BEAM_JITTER = 0.05;

	private final ProgramDroneEntity drone;
	private final CargoHauler hauler;
	@Nullable
	private BlockPos target;
	private int progress;
	private int stuckTicks;
	private int searchCooldown;

	public <T extends ProgramDroneEntity & CargoHauler> MineResourceGoal(T drone) {
		this.drone = drone;
		this.hauler = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (this.hauler.isCargoFull() || !this.canModifyWorld()) {
			return false;
		}
		if (this.searchCooldown > 0) {
			this.searchCooldown--;
			return false;
		}
		this.searchCooldown = 40;
		this.target = this.findTarget();
		return this.target != null;
	}

	@Nullable
	private BlockPos findTarget() {
		for (BlockPos pos : BlockPos.iterateOutwards(this.drone.getBlockPos(),
				SEARCH_RADIUS, VERTICAL_RADIUS, SEARCH_RADIUS)) {
			BlockState state = this.drone.getWorld().getBlockState(pos);
			if (HarvestTargets.resourceFor(state) != null) {
				return pos.toImmutable();
			}
		}
		return null;
	}

	@Override
	public boolean shouldContinue() {
		return this.target != null
				&& !this.hauler.isCargoFull()
				&& this.canModifyWorld()
				&& this.stuckTicks < STUCK_LIMIT
				&& HarvestTargets.resourceFor(this.drone.getWorld().getBlockState(this.target)) != null;
	}

	/** Program excavation obeys the {@code mobGriefing} gamerule, same as any block-breaking mob. */
	private boolean canModifyWorld() {
		return this.drone.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.progress = 0;
		this.stuckTicks = 0;
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}
		double centerX = this.target.getX() + 0.5;
		double centerY = this.target.getY() + 0.5;
		double centerZ = this.target.getZ() + 0.5;
		Vec3d center = new Vec3d(centerX, centerY, centerZ);
		Vec3d eye = this.drone.getEyePos();
		this.drone.getLookControl().lookAt(centerX, centerY, centerZ);

		double distanceSq = this.drone.squaredDistanceTo(centerX, centerY, centerZ);
		boolean inRange = distanceSq <= WORK_RANGE * WORK_RANGE;
		if (!inRange || !this.hasClearShot(eye, center)) {
			// Either still outside laser range, or something's in the way from
			// here — close the distance exactly like the old drive-up-and-mine
			// behaviour did, until a clear shot opens up (worst case, that's
			// adjacent to the block, same as before).
			this.stuckTicks++;
			if (this.drone.getNavigation().isIdle()) {
				this.drone.getNavigation().startMovingTo(centerX, this.target.getY(), centerZ, 1.0);
			}
			return;
		}

		this.drone.getNavigation().stop();
		this.stuckTicks = 0;
		this.progress++;

		BlockState state = this.drone.getWorld().getBlockState(this.target);
		if (this.progress % 5 == 0) {
			this.drone.getWorld().playSound(null, this.target,
					state.getSoundGroup().getHitSound(), SoundCategory.BLOCKS, 0.5f, 1.0f);
		}
		if (this.progress % 8 == 0) {
			this.drone.playSound(P7Sounds.MINING_LASER.get(), 0.6f, 1.0f + this.drone.getRandom().nextFloat() * 0.1f);
		}
		if (this.progress % 3 == 0) {
			this.drawMiningLaser(eye, center);
		}
		this.drone.getWorld().setBlockBreakingInfo(this.drone.getId(), this.target,
				this.progress * 10 / MINE_TICKS);

		if (this.progress >= MINE_TICKS) {
			String resource = HarvestTargets.resourceFor(state);
			if (resource != null) {
				this.hauler.addCargo(resource,
						HarvestTargets.yieldFor(resource, this.drone.getRandom()));
			}
			this.drone.getWorld().breakBlock(this.target, false, this.drone);
			this.target = null;
		}
	}

	/**
	 * True if nothing solid sits between {@code from} and {@code to} except
	 * (optionally) the target block itself — same raycast technique {@link
	 * dev.rheava.program7.audio.ProgramAcoustics} uses for occlusion checks.
	 */
	private boolean hasClearShot(Vec3d from, Vec3d to) {
		World world = this.drone.getWorld();
		BlockHitResult hit = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, ShapeContext.absent()));
		return hit.getType() == HitResult.Type.MISS || (this.target != null && this.target.equals(hit.getBlockPos()));
	}

	/** Cyan beam of particles from the drone's eye to the ore, plus a small impact burst at the block. */
	private void drawMiningLaser(Vec3d start, Vec3d end) {
		if (!(this.drone.getWorld() instanceof ServerWorld world)) {
			return;
		}
		double length = start.distanceTo(end);
		int segments = Math.max(1, (int) Math.round(length / BEAM_SEGMENT_SPACING));
		for (int i = 0; i <= segments; i++) {
			Vec3d point = start.lerp(end, (double) i / segments);
			world.spawnParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
					1, BEAM_JITTER, BEAM_JITTER, BEAM_JITTER, 0.0);
		}
		world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 4, 0.15, 0.15, 0.15, 0.02);
	}

	@Override
	public void stop() {
		if (this.target != null) {
			this.drone.getWorld().setBlockBreakingInfo(this.drone.getId(), this.target, -1);
			this.target = null;
		}
		this.progress = 0;
		this.drone.getNavigation().stop();
	}
}
