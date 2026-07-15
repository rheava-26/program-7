package dev.rheava.program7.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * The charge laser's three-resource state, carried on the item stack as a
 * data component (see {@link dev.rheava.program7.registry.P7DataComponents})
 * so it persists and networks with the stack exactly like vanilla item data
 * does — no separate NBT plumbing.
 *
 * @param batteryTicks ticks of fire left in the loaded power bank, 0..{@link ChargeLaserItem#BATTERY_CAPACITY_TICKS}
 * @param heatTicks    accumulated heat, 0..{@link ChargeLaserItem#HEAT_MAX}; vents while not firing
 * @param overheated   hysteresis latch — once heat maxes out, firing stays locked out until heat vents back below the clear threshold, not just below max
 * @param lensWear     cumulative firing ticks ever fired through this lens, 0..{@link ChargeLaserItem#LENS_MAX_TICKS}; never decreases except via the repair recipe
 */
public record ChargeLaserState(int batteryTicks, int heatTicks, boolean overheated, int lensWear) {
	public static final ChargeLaserState DEFAULT = new ChargeLaserState(0, 0, false, 0);

	public static final Codec<ChargeLaserState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("battery_ticks").forGetter(ChargeLaserState::batteryTicks),
			Codec.INT.fieldOf("heat_ticks").forGetter(ChargeLaserState::heatTicks),
			Codec.BOOL.fieldOf("overheated").forGetter(ChargeLaserState::overheated),
			Codec.INT.fieldOf("lens_wear").forGetter(ChargeLaserState::lensWear)
	).apply(instance, ChargeLaserState::new));

	public static final PacketCodec<RegistryByteBuf, ChargeLaserState> PACKET_CODEC = PacketCodecs.codec(CODEC);

	public ChargeLaserState withBattery(int newBatteryTicks) {
		return new ChargeLaserState(newBatteryTicks, this.heatTicks, this.overheated, this.lensWear);
	}

	public ChargeLaserState withHeat(int newHeatTicks, boolean newOverheated) {
		return new ChargeLaserState(this.batteryTicks, newHeatTicks, newOverheated, this.lensWear);
	}

	public ChargeLaserState withLensWear(int newLensWear) {
		return new ChargeLaserState(this.batteryTicks, this.heatTicks, this.overheated, newLensWear);
	}
}
