package dev.rheava.program7.director;

import java.util.List;

import dev.architectury.event.events.common.TickEvent;
import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.ProgramDroneEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

/**
 * The "reactive environment" layer: with {@code changeEnemyAi} on, vanilla
 * hostile mobs stop ignoring Program hardware. Every scan, any hostile mob
 * that has a Program drone within {@link #DRONE_AGGRO_RANGE} blocks is
 * re-targeted onto it, so a cave full of zombies and skeletons becomes a real
 * gauntlet the Program has to send combat drones through — a wither thrown at
 * a drone base should tear into the drones standing in front of it, not walk
 * past them.
 *
 * <p>No mixins: this works purely by calling {@code setTarget} on already
 * loaded hostile mobs from a server tick, the same lightweight approach
 * {@link dev.rheava.program7.network.InterferenceManager} uses for its
 * player-anchored scan.
 */
public final class HostileReactivity {
	/** How far a hostile mob will notice and go after a nearby Program drone. */
	private static final double DRONE_AGGRO_RANGE = 24.0;
	/** Search radius around each player for candidate hostile mobs; Program activity clusters near players anyway. */
	private static final double PLAYER_SEARCH_RANGE = 48.0;
	private static final int SCAN_INTERVAL = 20;

	public static void register() {
		TickEvent.SERVER_LEVEL_POST.register(HostileReactivity::tickWorld);
	}

	private static void tickWorld(ServerWorld world) {
		if (!Program7.CONFIG.changeEnemyAi) {
			return;
		}
		if (world.getTime() % SCAN_INTERVAL != 0) {
			return;
		}

		for (ServerPlayerEntity player : world.getPlayers()) {
			Box searchBox = player.getBoundingBox().expand(PLAYER_SEARCH_RANGE);
			List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class, searchBox, h -> true);
			for (HostileEntity mob : hostiles) {
				// Program units aren't HostileEntity (they extend PathAwareEntity),
				// so they're never in this list — no need (and, being unrelated
				// sibling types, no way) to guard against re-targeting our own kind.
				ProgramDroneEntity nearestDrone = findNearestDrone(world, mob);
				if (nearestDrone != null && mob.getTarget() != nearestDrone) {
					// A drone in range is always the priority threat — override
					// whatever else the mob was doing (including a player) so
					// caves full of monsters actually fight the Program's escorts.
					mob.setTarget(nearestDrone);
				}
			}
		}
	}

	@Nullable
	private static ProgramDroneEntity findNearestDrone(ServerWorld world, HostileEntity mob) {
		Box searchBox = mob.getBoundingBox().expand(DRONE_AGGRO_RANGE);
		List<ProgramDroneEntity> drones = world.getEntitiesByClass(ProgramDroneEntity.class, searchBox, d -> true);

		ProgramDroneEntity nearest = null;
		double nearestDistSq = DRONE_AGGRO_RANGE * DRONE_AGGRO_RANGE;
		for (ProgramDroneEntity drone : drones) {
			double distSq = mob.squaredDistanceTo(drone);
			if (distSq <= nearestDistSq) {
				nearest = drone;
				nearestDistSq = distSq;
			}
		}
		return nearest;
	}

	private HostileReactivity() {
	}
}
