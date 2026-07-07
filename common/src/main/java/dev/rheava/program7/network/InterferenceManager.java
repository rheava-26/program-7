package dev.rheava.program7.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.rheava.program7.entity.AttackDroneEntity;
import dev.rheava.program7.registry.P7Entities;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/**
 * The server half of the psionic proximity warning: every half second, rate
 * each player's surroundings for Program hardware within ~3 chunks and tell
 * their client how hard the interference should press.
 *
 * <p>Weights follow the design rule "intensity scales with how dangerous the
 * nearby drones are": a lone surveyor is a flicker, an armed attack drone on
 * approach washes the screen.
 */
public final class InterferenceManager {
	/** ~3 chunks, per the design bible. */
	private static final double RANGE = 48.0;
	private static final int SCAN_INTERVAL = 10;

	private static final Map<UUID, Float> LAST_SENT = new HashMap<>();

	public static void register() {
		// On a dedicated server the payload type must still be known; the
		// client registers it together with its receiver.
		if (Platform.getEnvironment() == Env.SERVER) {
			NetworkManager.registerS2CPayloadType(InterferencePayload.ID, InterferencePayload.CODEC);
		}
		TickEvent.SERVER_LEVEL_POST.register(InterferenceManager::tickWorld);
		PlayerEvent.PLAYER_QUIT.register(player -> LAST_SENT.remove(player.getUuid()));
	}

	private static void tickWorld(ServerWorld world) {
		if (world.getTime() % SCAN_INTERVAL != 0) {
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers()) {
			Box searchBox = player.getBoundingBox().expand(RANGE);
			List<MobEntity> drones = world.getEntitiesByClass(MobEntity.class, searchBox,
					entity -> entity.getType() == P7Entities.SURVEYOR_DRONE.get()
							|| entity.getType() == P7Entities.ATTACK_DRONE.get());

			float intensity = 0.0f;
			float strongestWeight = 0.0f;
			float threatYaw = Float.NaN;
			for (MobEntity drone : drones) {
				double distance = drone.distanceTo(player);
				if (distance > RANGE) {
					continue;
				}
				// Closer units press harder on the link.
				float falloff = 1.0f - (float) (distance / RANGE) * 0.6f;
				float weight = baseWeight(drone) * falloff;
				intensity += weight;
				if (weight > strongestWeight) {
					strongestWeight = weight;
					double dx = drone.getX() - player.getX();
					double dz = drone.getZ() - player.getZ();
					threatYaw = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
				}
			}
			intensity = MathHelper.clamp(intensity, 0.0f, 1.0f);

			float last = LAST_SENT.getOrDefault(player.getUuid(), 0.0f);
			boolean keepAlive = intensity > 0.0f && world.getTime() % 40 == 0;
			if (Math.abs(intensity - last) > 0.01f || keepAlive) {
				NetworkManager.sendToPlayer(player, new InterferencePayload(intensity, threatYaw));
				LAST_SENT.put(player.getUuid(), intensity);
			}
		}
	}

	private static float baseWeight(MobEntity drone) {
		if (drone instanceof AttackDroneEntity attackDrone) {
			return attackDrone.isArmed() ? 0.9f : 0.45f;
		}
		return 0.15f; // surveyor
	}

	private InterferenceManager() {
	}
}
