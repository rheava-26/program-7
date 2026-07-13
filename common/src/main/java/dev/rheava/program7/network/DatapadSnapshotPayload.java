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
 * alert stage (does it know about you yet?), and a coarse type guess — and an
 * {@link Incoming} artillery warning (the acoustic-intelligence hook from
 * ARTILLERY_AND_INDIRECT_FIRE.md §5/§9: a shell is in the air toward you).
 */
public record DatapadSnapshotPayload(Header header, List<Contact> contacts, Incoming incoming)
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
			Incoming.CODEC, DatapadSnapshotPayload::incoming,
			DatapadSnapshotPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
