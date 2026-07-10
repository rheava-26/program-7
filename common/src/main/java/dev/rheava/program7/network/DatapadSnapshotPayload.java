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
 * fleet estimate, escalation tier, nearest-base bearing) plus a list of
 * {@link Contact} blips — each a Program unit within datapad range, given as
 * an offset from the player so the client can plot it on the chunk grid, its
 * alert stage (does it know about you yet?), and a coarse type guess.
 */
public record DatapadSnapshotPayload(Header header, List<Contact> contacts) implements CustomPayload {
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

	public static final PacketCodec<RegistryByteBuf, DatapadSnapshotPayload> CODEC = PacketCodec.tuple(
			Header.CODEC, DatapadSnapshotPayload::header,
			Contact.CODEC.collect(PacketCodecs.toList()), DatapadSnapshotPayload::contacts,
			DatapadSnapshotPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
