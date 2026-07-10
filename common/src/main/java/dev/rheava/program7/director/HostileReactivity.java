package dev.rheava.program7.director;

import java.util.List;

import dev.architectury.event.events.common.TickEvent;
import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.ProgramDroneEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

/**
 * The "reactive environment" layer: with {@code changeEnemyAi} on, vanilla
 * hostile mobs stop ignoring Program hardware. Every scan, any hostile mob that
 * has a Program drone it can actually engage within {@link #DRONE_AGGRO_RANGE}
 * blocks is re-targeted onto it, so a cave full of zombies and skeletons
 * becomes a real gauntlet the Program has to send combat drones through — a
 * wither thrown at a drone base tears into the drones in front of it instead of
 * walking past them.
 *
 * <p>No mixins: this works purely by calling {@code setTarget} on already
 * loaded hostile mobs from a server tick, the same lightweight player-anchored
 * scan {@link dev.rheava.program7.network.InterferenceManager} uses.
 *
 * <p>Two deliberate limits: (1) it only sics a mob on a drone it can reach — a
 * ground pounder isn't neutralized by being pointed at a drone hovering out of
 * melee range; only ranged mobs get handed airborne targets. (2) It steers
 * goal-based mobs (zombies, skeletons, creepers, spiders, the wither, …) via
 * {@code setTarget}; brain-driven mobs (piglin/hoglin/warden) run combat off
 * Brain memory and are a known gap for a later pass.
 */
public final class HostileReactivity {
	/** How far a hostile mob will notice and go after a Program drone it can engage. */
	private static final double DRONE_AGGRO_RANGE = 24.0;
	/** Search radius around each player for candidate hostile mobs; Program activity clusters near players anyway. */
	private static final double PLAYER_SEARCH_RANGE = 48.0;
	/** A melee mob only gets pointed at a drone within this vertical reach; ranged mobs ignore this. */
	private static final double MELEE_REACH_HEIGHT = 4.0;
	private static final int SCAN_INTERVAL = 20;

	public static void register() {
		TickEvent.SERVER_LEVEL_POST.register(HostileReactivity::tickWorld);
	}

	private static void tickWorld(ServerWorld world) {
		if (!Program7.CONFIG.changeEnemyAi || world.getTime() % SCAN_INTERVAL != 0) {
			return;
		}

		for (ServerPlayerEntity player : world.getPlayers()) {
			// One drone query per player region (mob search radius + aggro radius),
			// then per-mob nearest is a cheap distance walk over this small list —
			// no per-mob entity-box scan.
			Box region = player.getBoundingBox().expand(PLAYER_SEARCH_RANGE + DRONE_AGGRO_RANGE);
			List<ProgramDroneEntity> drones =
					world.getEntitiesByClass(ProgramDroneEntity.class, region, ProgramDroneEntity::isAlive);
			if (drones.isEmpty()) {
				continue;
			}

			Box mobBox = player.getBoundingBox().expand(PLAYER_SEARCH_RANGE);
			List<HostileEntity> hostiles =
					world.getEntitiesByClass(HostileEntity.class, mobBox, HostileEntity::isAlive);
			for (HostileEntity mob : hostiles) {
				// Program units aren't HostileEntity (they extend PathAwareEntity),
				// so they're never in this list.
				ProgramDroneEntity drone = nearestEngageableDrone(mob, drones);
				if (drone != null && mob.getTarget() != drone) {
					// A reachable drone is the priority threat — override whatever
					// the mob was doing so the environment actually fights the fleet.
					mob.setTarget(drone);
				}
			}
		}
	}

	@Nullable
	private static ProgramDroneEntity nearestEngageableDrone(HostileEntity mob, List<ProgramDroneEntity> drones) {
		boolean ranged = mob instanceof RangedAttackMob;
		ProgramDroneEntity nearest = null;
		double best = DRONE_AGGRO_RANGE * DRONE_AGGRO_RANGE;
		for (ProgramDroneEntity drone : drones) {
			double distSq = mob.squaredDistanceTo(drone);
			if (distSq > best) {
				continue;
			}
			// Don't hand a melee mob a drone it can't actually get at (one hovering
			// well above it) — that would just freeze the mob pathing uselessly and
			// pull it off real threats. Ranged mobs can engage a flier at any height.
			if (!ranged && Math.abs(drone.getY() - mob.getY()) > MELEE_REACH_HEIGHT) {
				continue;
			}
			nearest = drone;
			best = distSq;
		}
		return nearest;
	}

	private HostileReactivity() {
	}
}
