package dev.rheava.program7.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * The ender pearl launcher's battery state, carried on the item stack as a
 * data component (see {@link dev.rheava.program7.registry.P7DataComponents}),
 * the same pattern {@link ChargeLaserState} established. Just one juggled
 * resource here — the launcher has no heat or wear, only "how much charge is
 * left in the loaded power bank."
 *
 * @param batteryUnits charge left in the loaded power bank, 0..{@link EnderPearlLauncherItem#BATTERY_CAPACITY}
 */
public record EnderPearlLauncherState(int batteryUnits) {
	public static final EnderPearlLauncherState DEFAULT = new EnderPearlLauncherState(0);

	public static final Codec<EnderPearlLauncherState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("battery_units").forGetter(EnderPearlLauncherState::batteryUnits)
	).apply(instance, EnderPearlLauncherState::new));

	public static final PacketCodec<RegistryByteBuf, EnderPearlLauncherState> PACKET_CODEC =
			PacketCodecs.registryCodec(CODEC);

	public EnderPearlLauncherState withBattery(int newBatteryUnits) {
		return new EnderPearlLauncherState(newBatteryUnits);
	}
}
