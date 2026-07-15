package dev.rheava.program7.registry;

import dev.rheava.program7.Program7;
import dev.rheava.program7.item.ChargeLaserState;
import dev.rheava.program7.item.TrackingChipTarget;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Custom data components for per-stack item state (the repo's first use of
 * this — see {@code ChargeLaserItem}). {@link Registries#DATA_COMPONENT_TYPE}
 * is a static, built-in registry rather than a dynamic/datapack one, so this
 * registers directly with {@link Registry#register} instead of going through
 * an {@code architectury} {@code DeferredRegister} — there's no registration
 * timing to defer, and both loaders resolve this identically since it's
 * plain vanilla registry code.
 */
public final class P7DataComponents {
	/** See {@link ChargeLaserState}. */
	public static final ComponentType<ChargeLaserState> CHARGE_LASER_STATE = Registry.register(
			Registries.DATA_COMPONENT_TYPE,
			Program7.id("charge_laser_state"),
			ComponentType.<ChargeLaserState>builder()
					.codec(ChargeLaserState.CODEC)
					.packetCodec(ChargeLaserState.PACKET_CODEC)
					.build());

	/**
	 * See {@link TrackingChipTarget}. Unset on a fresh chip — {@code null}
	 * means "no signal" rather than an {@code Optional}-wrapped payload.
	 */
	public static final ComponentType<TrackingChipTarget> TRACKING_CHIP_TARGET = Registry.register(
			Registries.DATA_COMPONENT_TYPE,
			Program7.id("tracking_chip_target"),
			ComponentType.<TrackingChipTarget>builder()
					.codec(TrackingChipTarget.CODEC)
					.packetCodec(TrackingChipTarget.PACKET_CODEC)
					.build());

	/** No-op body — referencing this class is enough to run the static registration above; kept for symmetry with the other {@code P7*} registries. */
	public static void register() {
	}

	private P7DataComponents() {
	}
}
