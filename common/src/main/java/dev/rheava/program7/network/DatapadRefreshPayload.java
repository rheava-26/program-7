package dev.rheava.program7.network;

import dev.rheava.program7.Program7;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Client → server: "I've got the datapad screen open, send me a fresh read."
 * The open {@code DatapadScreen} fires this a couple of times a second; the
 * server answers with a new {@link DatapadSnapshotPayload}, so the radar
 * updates live — contacts drift, alert colours warm, and a cluster can bloom
 * into view between sweeps rather than sitting frozen at the moment you opened
 * it. Carries no data; the sending player is all the server needs.
 */
public record DatapadRefreshPayload() implements CustomPayload {
	public static final DatapadRefreshPayload INSTANCE = new DatapadRefreshPayload();
	public static final CustomPayload.Id<DatapadRefreshPayload> ID =
			new CustomPayload.Id<>(Program7.id("datapad_refresh"));
	public static final PacketCodec<RegistryByteBuf, DatapadRefreshPayload> CODEC = PacketCodec.unit(INSTANCE);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
