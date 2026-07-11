package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.ProgramDroneEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * "Fly around every direction as they spawn in, looking for structures,
 * caves, villages." A recon unit's default state isn't loitering near where
 * it woke up — it's fanning out on a long leg in a fresh compass direction,
 * covering ground, and swinging aside to look at anything interesting it
 * passes near.
 *
 * <p>Each leg picks a random heading at least {@link #MIN_HEADING_SEPARATION_DEGREES}
 * away from the last one, so a unit doesn't just oscillate back and forth
 * across the same strip of map — successive legs actually fan out in
 * different directions the way a real search pattern would. Legs are long
 * ({@link #MIN_RANGE}-{@link #MAX_RANGE} blocks), so a freshly-deployed
 * scout visibly commits to going somewhere instead of pacing its spawn
 * point.
 *
 * <p>While traveling, a cheap periodic scan looks for a short list of
 * "obviously built" blocks (beds, chests, lecterns, bells, spawners...) —
 * everything HarvestTargets-style ore scanning doesn't already cover — and
 * if it spots one, breaks off the leg to swing over and hover near it for a
 * few seconds before picking a new heading. It's a proxy for "found a
 * structure/village," not real structure-location — see the risk note in the
 * handoff report for why this stays block-tag-based rather than calling the
 * vanilla structure locator.
 *
 * <p>Deliberately given a high priority (lower goal-selector number) so it
 * beats idle wander/loiter goals, but every implementing entity still slots
 * it below its own combat/spot/retreat goals — this goal itself also bails
 * the instant the unit has a live target, so it never fights the goal that's
 * actually handling that target.
 */
public class ExploreGoal extends Goal {
	private static final double MIN_RANGE = 40.0;
	private static final double MAX_RANGE = 110.0;
	private static final double ARRIVAL_RADIUS_SQ = 6.0 * 6.0;
	/** Give up on a stalled/unreachable leg and roll a new heading rather than sitting stuck. */
	private static final int MAX_LEG_TICKS = 600;
	/** Re-roll a heading this close to the previous one so successive legs actually fan out. */
	private static final double MIN_HEADING_SEPARATION_DEGREES = 70.0;
	private static final int POI_SCAN_RADIUS = 6;
	private static final int POI_SCAN_INTERVAL_TICKS = 20;
	private static final int POI_LINGER_TICKS = 80;

	private enum Phase { TRAVELING, INVESTIGATING }

	private final ProgramDroneEntity drone;
	private Vec3d destination = Vec3d.ZERO;
	private double lastHeadingDegrees = Double.NaN;
	private Phase phase = Phase.TRAVELING;
	private int legTicks;
	private int scanThrottle;
	private int poiTicks;
	@Nullable
	private BlockPos poi;

	public ExploreGoal(ProgramDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (!Program7.CONFIG.longRangeScouting) {
			return false;
		}
		return this.drone.getTarget() == null && !this.drone.isRetreating() && !this.drone.isScrambled()
				&& !this.drone.isCharging();
	}

	@Override
	public boolean shouldContinue() {
		return this.drone.getTarget() == null && !this.drone.isRetreating() && !this.drone.isScrambled()
				&& !this.drone.isCharging();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.phase = Phase.TRAVELING;
		this.poi = null;
		this.pickNewDestination();
	}

	@Override
	public void tick() {
		switch (this.phase) {
			case TRAVELING -> this.tickTraveling();
			case INVESTIGATING -> this.tickInvestigating();
		}
	}

	private void tickTraveling() {
		this.legTicks++;
		this.drone.getLookControl().lookAt(this.destination.x, this.destination.y, this.destination.z);
		if (this.drone.getNavigation().isIdle()) {
			this.drone.getNavigation().startMovingTo(this.destination.x, this.destination.y, this.destination.z, 1.0);
		}

		BlockPos found = this.scanForPointOfInterest();
		if (found != null) {
			this.poi = found;
			this.phase = Phase.INVESTIGATING;
			this.poiTicks = 0;
			this.drone.getNavigation().startMovingTo(found.getX() + 0.5, found.getY() + 1.5, found.getZ() + 0.5, 1.0);
			return;
		}

		boolean arrived = this.drone.getPos().squaredDistanceTo(this.destination) <= ARRIVAL_RADIUS_SQ;
		boolean stalled = this.legTicks > 20 && this.drone.getNavigation().isIdle();
		if (arrived || stalled || this.legTicks >= MAX_LEG_TICKS) {
			this.pickNewDestination();
		}
	}

	private void tickInvestigating() {
		if (this.poi == null) {
			this.phase = Phase.TRAVELING;
			this.pickNewDestination();
			return;
		}
		double x = this.poi.getX() + 0.5;
		double y = this.poi.getY() + 1.5;
		double z = this.poi.getZ() + 0.5;
		this.drone.getLookControl().lookAt(x, y, z);
		if (this.drone.getNavigation().isIdle()) {
			this.drone.getNavigation().startMovingTo(x, y, z, 0.8);
		}
		this.poiTicks++;
		if (this.poiTicks >= POI_LINGER_TICKS) {
			this.phase = Phase.TRAVELING;
			this.poi = null;
			this.pickNewDestination();
		}
	}

	/** Throttled scan for a nearby "obviously built" block — a cheap stand-in for real structure detection. */
	@Nullable
	private BlockPos scanForPointOfInterest() {
		this.scanThrottle++;
		if (this.scanThrottle < POI_SCAN_INTERVAL_TICKS) {
			return null;
		}
		this.scanThrottle = 0;
		BlockPos center = this.drone.getBlockPos();
		for (BlockPos pos : BlockPos.iterateOutwards(center, POI_SCAN_RADIUS, POI_SCAN_RADIUS, POI_SCAN_RADIUS)) {
			if (isPointOfInterest(this.drone.getWorld().getBlockState(pos))) {
				if (this.drone.getWorld() instanceof ServerWorld world) {
					world.spawnParticles(ParticleTypes.END_ROD,
							pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
				}
				return pos.toImmutable();
			}
		}
		return null;
	}

	private static boolean isPointOfInterest(BlockState state) {
		return state.isIn(BlockTags.BEDS)
				|| state.isOf(Blocks.CHEST)
				|| state.isOf(Blocks.TRAPPED_CHEST)
				|| state.isOf(Blocks.ENDER_CHEST)
				|| state.isOf(Blocks.SPAWNER)
				|| state.isOf(Blocks.LECTERN)
				|| state.isOf(Blocks.BELL)
				|| state.isOf(Blocks.CAMPFIRE)
				|| state.isOf(Blocks.SOUL_CAMPFIRE)
				|| state.isOf(Blocks.BREWING_STAND)
				|| state.isOf(Blocks.END_PORTAL_FRAME);
	}

	/** Roll a long-range destination whose heading fans out from the previous leg instead of retracing it. */
	private void pickNewDestination() {
		Random random = this.drone.getRandom();
		Vec3d origin = this.drone.getPos();
		double headingDegrees = random.nextDouble() * 360.0;
		for (int attempt = 0; attempt < 6 && !Double.isNaN(this.lastHeadingDegrees); attempt++) {
			double diff = Math.abs(MathHelper.wrapDegrees(headingDegrees - this.lastHeadingDegrees));
			if (diff >= MIN_HEADING_SEPARATION_DEGREES) {
				break;
			}
			headingDegrees = random.nextDouble() * 360.0;
		}

		double distance = MIN_RANGE + random.nextDouble() * (MAX_RANGE - MIN_RANGE);
		double radians = Math.toRadians(headingDegrees);
		double targetX = origin.x + Math.cos(radians) * distance;
		double targetZ = origin.z + Math.sin(radians) * distance;
		double targetY = origin.y + (random.nextDouble() - 0.35) * 24.0;
		targetY = MathHelper.clamp(targetY, this.drone.getWorld().getBottomY() + 8, this.drone.getWorld().getTopY() - 8);

		this.destination = new Vec3d(targetX, targetY, targetZ);
		this.lastHeadingDegrees = headingDegrees;
		this.legTicks = 0;
	}

	@Override
	public void stop() {
		this.drone.getNavigation().stop();
		this.poi = null;
		this.phase = Phase.TRAVELING;
	}
}
