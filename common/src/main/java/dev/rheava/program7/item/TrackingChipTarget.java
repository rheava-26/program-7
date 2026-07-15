package dev.rheava.program7.item;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

/**
 * A tracking chip's marked target, carried on the item stack as a data
 * component (see {@link dev.rheava.program7.registry.P7DataComponents}). A
 * fresh chip simply has no component set — {@link TrackingChipItem} treats
 * that as "no signal" rather than modelling absence with an {@code Optional}.
 *
 * @param targetUuid the marked entity's UUID, resolved against the world's
 *                    loaded entities on every read (see
 *                    {@link TrackingChipItem})
 * @param lastX      last-known world-space position, refreshed every time
 *                    the target is loaded and the chip is read or re-tagged
 * @param lastY      see {@link #lastX}
 * @param lastZ      see {@link #lastX}
 */
public record TrackingChipTarget(UUID targetUuid, double lastX, double lastY, double lastZ) {
	public static final Codec<TrackingChipTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Uuids.CODEC.fieldOf("target_uuid").forGetter(TrackingChipTarget::targetUuid),
			Codec.DOUBLE.fieldOf("last_x").forGetter(TrackingChipTarget::lastX),
			Codec.DOUBLE.fieldOf("last_y").forGetter(TrackingChipTarget::lastY),
			Codec.DOUBLE.fieldOf("last_z").forGetter(TrackingChipTarget::lastZ)
	).apply(instance, TrackingChipTarget::new));

	public static final PacketCodec<RegistryByteBuf, TrackingChipTarget> PACKET_CODEC = PacketCodecs.registryCodec(CODEC);

	public TrackingChipTarget withPos(double x, double y, double z) {
		return new TrackingChipTarget(this.targetUuid, x, y, z);
	}
}
