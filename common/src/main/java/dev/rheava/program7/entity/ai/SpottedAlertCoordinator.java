package dev.rheava.program7.entity.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Per-player "we already announced this" cooldown for the shrill spotted cue
 * that {@link SpotTargetGoal}, {@link UAVSpotGoal}, {@link SearchlightSpotGoal}
 * and {@link ScanPlayerGoal} all independently play the instant they confirm
 * visual contact on a player.
 *
 * <p>Without this, every spotter that acquires the same already-spotted
 * player (a scout car, a UAV circling overhead, a surveyor closing in, all at
 * once) replays the full shrill cue, which stacks into a spammy mess. This
 * class is the shared gate: the first spotter to confirm contact within the
 * cooldown window gets to play the real cue; every other spotter that
 * acquires the same player while the window is still open should skip the
 * shrill sound and just layer the quieter psionic interference hum instead
 * (see the call sites in the goals above).
 *
 * <p>Deliberately self-contained — a static, time-keyed map on player UUID
 * rather than anything hung off {@code ProgramDirectorState} — so it doesn't
 * need a shared-file change to exist. Entries are cheap (one {@code Long} per
 * player who has ever been spotted) and get swept opportunistically so a
 * server that's hosted a lot of players over time doesn't accumulate stale
 * entries forever.
 */
public final class SpottedAlertCoordinator {
	/** How long a "spotted" announcement stays fresh enough to suppress a second shrill cue. ~5 seconds. */
	private static final long COOLDOWN_TICKS = 100L;
	/** Sweep stale entries once the map grows past this size, rather than on a timer. */
	private static final int SWEEP_THRESHOLD = 64;

	private static final Map<UUID, Long> lastAnnouncedTick = new HashMap<>();

	private SpottedAlertCoordinator() {
	}

	/**
	 * Call the instant a spotting goal confirms contact on {@code player}
	 * (about to play its shrill "you've been spotted" cue).
	 *
	 * @return {@code true} the first time this fires for {@code player}
	 *         within the cooldown window — the caller should play its full
	 *         shrill cue. {@code false} on every subsequent call for the same
	 *         player while the window is still open — the caller should skip
	 *         the shrill cue and just layer the softer interference hum.
	 */
	public static boolean tryAnnounceSpotted(ServerWorld world, PlayerEntity player) {
		long now = world.getTime();
		UUID id = player.getUuid();
		Long last = lastAnnouncedTick.get(id);
		if (last != null && now - last < COOLDOWN_TICKS) {
			return false;
		}
		lastAnnouncedTick.put(id, now);
		if (lastAnnouncedTick.size() > SWEEP_THRESHOLD) {
			long cutoff = now - COOLDOWN_TICKS * 20L;
			lastAnnouncedTick.values().removeIf(tick -> tick < cutoff);
		}
		return true;
	}
}
