package dev.rheava.program7.network;

import dev.rheava.program7.Program7;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server → client: how hard the psionic interference should press on this
 * player's HUD right now.
 *
 * @param intensity 0..1 danger level from Program units within ~3 chunks
 * @param threatYaw world yaw (degrees) toward the strongest nearby threat,
 *                  or {@link Float#NaN} when there is none — only used when
 *                  the directionality config is enabled client-side
 */
public record InterferencePayload(float intensity, float threatYaw) implements CustomPayload {
	public static final CustomPayload.Id<InterferencePayload> ID = new CustomPayload.Id<>(Program7.id("interference"));

	public static final PacketCodec<RegistryByteBuf, InterferencePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT, InterferencePayload::intensity,
			PacketCodecs.FLOAT, InterferencePayload::threatYaw,
			InterferencePayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
