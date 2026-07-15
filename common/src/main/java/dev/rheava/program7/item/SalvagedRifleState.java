package dev.rheava.program7.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * The salvaged rifle's internal-magazine state, carried on the item stack as
 * a data component (see {@link dev.rheava.program7.registry.P7DataComponents})
 * the same way the charge laser's battery/heat/lens state is — see {@link
 * ChargeLaserState}.
 *
 * @param rounds rounds currently loaded, 0..{@link SalvagedRifleItem#MAGAZINE_CAPACITY}
 */
public record SalvagedRifleState(int rounds) {
	public static final SalvagedRifleState DEFAULT = new SalvagedRifleState(0);

	public static final Codec<SalvagedRifleState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("rounds").forGetter(SalvagedRifleState::rounds)
	).apply(instance, SalvagedRifleState::new));

	public static final PacketCodec<RegistryByteBuf, SalvagedRifleState> PACKET_CODEC = PacketCodecs.registryCodec(CODEC);
}
