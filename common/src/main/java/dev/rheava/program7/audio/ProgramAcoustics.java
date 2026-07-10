package dev.rheava.program7.audio;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * The Program's acoustics physics layer — every discrete weapon/explosion
 * sound routes through {@link #emit} instead of calling
 * {@code World.playSound}/{@code Entity.playSound} directly. This applies
 * three things on top of the (currently placeholder) sound events
 * themselves:
 *
 * <ul>
 *   <li>Travel-time delay: the closest player doesn't hear the shot the
 *       instant it's fired, they hear it once the sound would actually have
 *       reached them, at a fixed speed of sound.</li>
 *   <li>Distance-based tone shaping: close/medium/far bands scale volume and
 *       pitch so a far-off gunshot reads as far-off (louder to carry, lower
 *       to read as boomy/distant) even though it's still the same recorded
 *       clip.</li>
 *   <li>Occlusion: a raycast from the player's eyes to the source checks for
 *       intervening rock and muffles the sound (quieter, lower) if it finds
 *       any.</li>
 * </ul>
 *
 * <p>This is v1: it slots real close/medium/far recorded variants in later
 * without callers changing at all — for now every band just reshapes the
 * one sound event a caller hands over.
 *
 * <p>Not for ambient/looping sounds ({@code getAmbientSound} and the like) —
 * those are MC-driven and out of scope here. Only discrete, one-shot weapon
 * and explosion sounds should route through this.
 */
public final class ProgramAcoustics {
	/** How far out to even look for a player to shape the sound for. */
	private static final double PLAYER_SEARCH_RADIUS = 220.0;
	/** Speed of sound, in blocks per tick, used to derive travel delay. */
	private static final double SPEED_OF_SOUND = 17.0;
	/** Longest travel delay we'll ever schedule, so a very distant blast doesn't sit in queue forever. */
	private static final long MAX_DELAY_TICKS = 60L;

	/** Below this distance a sound is "close": no tone shaping at all. */
	private static final double CLOSE_DISTANCE = 20.0;
	/** At or above this distance a sound is "far": maximum shaping. Between {@link #CLOSE_DISTANCE} and here is "medium". */
	private static final double FAR_DISTANCE = 80.0;

	private static final float MEDIUM_VOLUME_SCALE = 1.1f;
	private static final float MEDIUM_PITCH_SCALE = 0.9f;
	private static final float FAR_VOLUME_SCALE = 1.35f;
	private static final float FAR_PITCH_SCALE = 0.78f;

	/** Volume/pitch scale applied when rock sits between the player and the source. */
	private static final float OCCLUSION_VOLUME_SCALE = 0.5f;
	private static final float OCCLUSION_PITCH_SCALE = 0.85f;
	/**
	 * How much shorter than the true distance the occlusion raycast's hit
	 * point has to land before we call it "blocked" rather than just a graze
	 * off geometry right at the source. In blocks.
	 */
	private static final double OCCLUSION_SLOP = 0.5;

	/** Per-world queue of sounds waiting on their travel-time delay. Weak on the world so an unloaded world doesn't leak. */
	private static final Map<ServerWorld, List<Pending>> QUEUES = new WeakHashMap<>();

	/**
	 * Per-world registry of loud player-made noises (mining, etc.) that a
	 * nearby Program unit might "hear" and go investigate — see
	 * {@link dev.rheava.program7.entity.ai.InvestigateNoiseGoal}. Separate
	 * from {@link #QUEUES}: this isn't about shaping/delaying a sound the
	 * player hears, it's about the Program's own perception of the player.
	 */
	private static final Map<ServerWorld, List<Noise>> NOISES = new WeakHashMap<>();
	/** How long a reported noise stays "fresh" enough for a drone to still react to it. 5 seconds. */
	private static final long NOISE_LIFETIME_TICKS = 100L;

	private ProgramAcoustics() {
	}

	/** A single sound waiting to be played once {@code playAtTick} arrives. */
	private record Pending(double x, double y, double z, SoundEvent sound, SoundCategory category,
			float volume, float pitch, long playAtTick) {
	}

	/** A single loud player-made noise, still audible to drones until {@code expiryTick}. */
	private record Noise(double x, double y, double z, float loudness, long expiryTick) {
	}

	/**
	 * Plays (and removes) every pending sound in {@code world}'s queue whose
	 * travel-time delay has elapsed. Call once per server tick, for every
	 * dimension — weapons fire outside the overworld too.
	 */
	public static void tick(ServerWorld world) {
		List<Pending> queue = QUEUES.get(world);
		if (queue != null && !queue.isEmpty()) {
			long now = world.getTime();
			Iterator<Pending> it = queue.iterator();
			while (it.hasNext()) {
				Pending p = it.next();
				if (p.playAtTick() <= now) {
					world.playSound(null, p.x(), p.y(), p.z(), p.sound(), p.category(), p.volume(), p.pitch());
					it.remove();
				}
			}
		}

		List<Noise> noises = NOISES.get(world);
		if (noises != null && !noises.isEmpty()) {
			long t = world.getTime();
			noises.removeIf(n -> n.expiryTick() <= t);
		}
	}

	/** Record a loud player-made noise at a world position. Louder events are heard from farther off. */
	public static void reportNoise(ServerWorld world, double x, double y, double z, float loudness) {
		NOISES.computeIfAbsent(world, w -> new ArrayList<>())
				.add(new Noise(x, y, z, loudness, world.getTime() + NOISE_LIFETIME_TICKS));
	}

	/**
	 * The position of the nearest still-fresh noise within {@code hearingRange} of
	 * {@code (x,y,z)}, or null if none. A noise's effective audible radius scales
	 * with its loudness, so quiet taps don't carry.
	 */
	public static Vec3d nearestAudibleNoise(ServerWorld world, double x, double y, double z, double hearingRange) {
		List<Noise> list = NOISES.get(world);
		if (list == null || list.isEmpty()) {
			return null;
		}
		Noise best = null;
		double bestSq = Double.MAX_VALUE;
		long now = world.getTime();
		for (Noise n : list) {
			if (n.expiryTick() <= now) {
				continue;
			}
			double dx = n.x() - x, dy = n.y() - y, dz = n.z() - z;
			double distSq = dx * dx + dy * dy + dz * dz;
			double audible = Math.min(hearingRange, hearingRange * n.loudness());
			if (distSq <= audible * audible && distSq < bestSq) {
				bestSq = distSq;
				best = n;
			}
		}
		return best == null ? null : new Vec3d(best.x(), best.y(), best.z());
	}

	/** Convenience overload taking a {@link Vec3d} source position. */
	public static void emit(ServerWorld world, Vec3d pos, SoundEvent sound, SoundCategory category,
			float baseVolume, float basePitch) {
		emit(world, pos.x, pos.y, pos.z, sound, category, baseVolume, basePitch);
	}

	/**
	 * Schedules {@code sound} to play at {@code (x, y, z)}, shaped for the
	 * closest player: delayed for travel time, its volume/pitch bent by
	 * distance band, and muffled further if rock sits between the player and
	 * the source. If no player is close enough to shape it for, the sound
	 * just plays immediately at its base volume/pitch — there's nobody to
	 * shape it for.
	 */
	public static void emit(ServerWorld world, double x, double y, double z, SoundEvent sound,
			SoundCategory category, float baseVolume, float basePitch) {
		PlayerEntity player = world.getClosestPlayer(x, y, z, PLAYER_SEARCH_RADIUS, false);
		if (player == null) {
			schedule(world, x, y, z, sound, category, baseVolume, basePitch, 0L);
			return;
		}

		double distance = Math.sqrt(player.squaredDistanceTo(x, y, z));
		long delay = Math.min(MAX_DELAY_TICKS, Math.round(distance / SPEED_OF_SOUND));

		float volume = baseVolume;
		float pitch = basePitch;
		if (distance > FAR_DISTANCE) {
			volume = baseVolume * FAR_VOLUME_SCALE;
			pitch = basePitch * FAR_PITCH_SCALE;
		} else if (distance >= CLOSE_DISTANCE) {
			volume = baseVolume * MEDIUM_VOLUME_SCALE;
			pitch = basePitch * MEDIUM_PITCH_SCALE;
		}

		Vec3d source = new Vec3d(x, y, z);
		Vec3d eye = player.getEyePos();
		BlockHitResult hit = world.raycast(new RaycastContext(eye, source,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, ShapeContext.absent()));
		if (hit.getType() == HitResult.Type.BLOCK) {
			double hitDistance = hit.getPos().distanceTo(eye);
			double sourceDistance = source.distanceTo(eye);
			if (hitDistance < sourceDistance - OCCLUSION_SLOP) {
				// Rock caught the raycast well short of the source itself —
				// there's real geometry in the way, not just a graze at the
				// source's own position.
				volume *= OCCLUSION_VOLUME_SCALE;
				pitch *= OCCLUSION_PITCH_SCALE;
			}
		}

		schedule(world, x, y, z, sound, category, volume, pitch, delay);
	}

	private static void schedule(ServerWorld world, double x, double y, double z, SoundEvent sound,
			SoundCategory category, float volume, float pitch, long delay) {
		QUEUES.computeIfAbsent(world, w -> new ArrayList<>())
				.add(new Pending(x, y, z, sound, category, volume, pitch, world.getTime() + delay));
	}
}
