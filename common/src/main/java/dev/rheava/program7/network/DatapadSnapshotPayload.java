package dev.rheava.program7.network;

import java.util.List;

import dev.rheava.program7.Program7;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server → client: a frozen snapshot of what the Program's own instrumentation
 * can tell the player right now, sent when they read the Datapad. Drives the
 * v2 radar screen ({@code DatapadScreen}) instead of the old chat readout.
 *
 * <p>Split into a compact {@link Header} of scalar readouts (posture, heat,
 * fleet estimate, escalation tier, nearest-base bearing), a list of
 * {@link Contact} blips — each a Program unit within datapad range, given as
 * an offset from the player so the client can plot it on the chunk grid, its
 * alert stage (does it know about you yet?), and a coarse type guess — a list
 * of {@link Tracked} blips gathered from the player's own carried tracking
 * chips (see {@code DatapadItem#trackedFor}, {@code TrackingChipItem}) — the
 * player's own intel, exact and not fog-limited by datapad range like an
 * ordinary contact — and an {@link Incoming} artillery warning (the
 * acoustic-intelligence hook from ARTILLERY_AND_INDIRECT_FIRE.md §5/§9: a
 * shell is in the air toward you).
 */
public record DatapadSnapshotPayload(Header header, List<Contact> contacts, List<Tracked> tracked, Incoming incoming)
		implements CustomPayload {
	public static final CustomPayload.Id<DatapadSnapshotPayload> ID =
			new CustomPayload.Id<>(Program7.id("datapad_snapshot"));

	/**
	 * The scalar readouts down the datapad's status rail.
	 *
	 * @param posture       the Program's posture label (e.g. HUNTING, DORMANT)
	 * @param heat          current global threat 0..MAX_THREAT
	 * @param fleetEstimate off-screen virtualized fleet size — the "they're everywhere" number
	 * @param tier          coarse escalation tier (1..3)
	 * @param baseYaw       world yaw (deg) toward the nearest known base, or NaN if none
	 * @param baseDistance  blocks to that base, or -1 if none
	 */
	public record Header(String posture, int heat, int fleetEstimate, int tier, float baseYaw, int baseDistance) {
		public static final PacketCodec<RegistryByteBuf, Header> CODEC = PacketCodec.tuple(
				PacketCodecs.STRING, Header::posture,
				PacketCodecs.VAR_INT, Header::heat,
				PacketCodecs.VAR_INT, Header::fleetEstimate,
				PacketCodecs.VAR_INT, Header::tier,
				PacketCodecs.FLOAT, Header::baseYaw,
				PacketCodecs.VAR_INT, Header::baseDistance,
				Header::new);
	}

	/**
	 * A single radar blip.
	 *
	 * @param relX  world X offset from the player (blocks)
	 * @param relZ  world Z offset from the player (blocks)
	 * @param alert alert-ramp ordinal (0 UNAWARE .. 4 ENGAGING) — the "does it know about you" colour axis
	 * @param guess coarse type guess (0 unknown, 1 gun, 2 recon, 3 logistics)
	 */
	public record Contact(float relX, float relZ, int alert, int guess) {
		public static final PacketCodec<RegistryByteBuf, Contact> CODEC = PacketCodec.tuple(
				PacketCodecs.FLOAT, Contact::relX,
				PacketCodecs.FLOAT, Contact::relZ,
				PacketCodecs.VAR_INT, Contact::alert,
				PacketCodecs.VAR_INT, Contact::guess,
				Contact::new);
	}

	/**
	 * A tracking-chip blip: the exact position of one target a player's own
	 * carried {@code TrackingChipItem} stack has marked, gathered straight off
	 * the item stack's stored {@code TrackingChipTarget} component rather than
	 * fog-limited/range-filtered like an ordinary {@link Contact}.
	 *
	 * @param relX world X offset from the player (blocks) — the target's live
	 *             resolved position if it's currently loaded, alive, and in
	 *             the same dimension, otherwise its last-known stored position
	 * @param relZ see {@link #relX}
	 * @param live whether {@code relX}/{@code relZ} is a live resolved
	 *             position or a stale last-known fix — the chip's own
	 *             "signal lost" condition, so the client can grey the marker
	 */
	public record Tracked(float relX, float relZ, boolean live) {
		public static final PacketCodec<RegistryByteBuf, Tracked> CODEC = PacketCodec.tuple(
				PacketCodecs.FLOAT, Tracked::relX,
				PacketCodecs.FLOAT, Tracked::relZ,
				PacketCodecs.BOOL, Tracked::live,
				Tracked::new);
	}

	/**
	 * The incoming-artillery warning down the datapad's status rail. A shell in
	 * the air whose flight path passes near the player surfaces here so the
	 * player gets the §5 fairness telegraph on the readout as well as by ear.
	 *
	 * @param bearing    world yaw (deg) the round is inbound <em>from</em>, or NaN if nothing is inbound
	 * @param etaSeconds rough seconds until it lands nearby, or -1 if nothing is inbound
	 */
	public record Incoming(float bearing, int etaSeconds) {
		/** The "all clear" sentinel: no shell in the air toward the player. */
		public static final Incoming NONE = new Incoming(Float.NaN, -1);

		public static final PacketCodec<RegistryByteBuf, Incoming> CODEC = PacketCodec.tuple(
				PacketCodecs.FLOAT, Incoming::bearing,
				PacketCodecs.VAR_INT, Incoming::etaSeconds,
				Incoming::new);

		public boolean present() {
			return this.etaSeconds >= 0 && !Float.isNaN(this.bearing);
		}
	}

	public static final PacketCodec<RegistryByteBuf, DatapadSnapshotPayload> CODEC = PacketCodec.tuple(
			Header.CODEC, DatapadSnapshotPayload::header,
			Contact.CODEC.collect(PacketCodecs.toList()), DatapadSnapshotPayload::contacts,
			Tracked.CODEC.collect(PacketCodecs.toList()), DatapadSnapshotPayload::tracked,
			Incoming.CODEC, DatapadSnapshotPayload::incoming,
			DatapadSnapshotPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
