package dev.rheava.program7.registry;

import dev.rheava.program7.Program7;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

/**
 * Datapack-defined damage types (see {@code data/program7/damage_type/}) —
 * these are dynamic-registry content, not code-registered objects, so this
 * class is just the typed {@link RegistryKey} handles callers use to build a
 * {@link net.minecraft.entity.damage.DamageSource} or match one with {@link
 * net.minecraft.entity.damage.DamageSource#isOf}, mirroring vanilla's own
 * {@code DamageTypes} holder class.
 */
public final class P7DamageTypes {
	/**
	 * The charge laser's beam. Routed through {@link
	 * dev.rheava.program7.entity.ArmorProfile.DamageClass#ENERGY} by {@code
	 * ProgramDroneEntity#classify} so the mod's existing typed-armor math is
	 * what produces the "melts light drones, tickles armor" profile — this
	 * damage type itself carries no special-casing.
	 */
	public static final RegistryKey<DamageType> LASER = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Program7.id("laser"));

	/**
	 * Conventional gunfire from a player-held firearm (the salvaged rifle and
	 * anything that joins it later). Routed through {@link
	 * dev.rheava.program7.entity.ArmorProfile.DamageClass#BALLISTIC} by {@code
	 * ProgramDroneEntity#classify} — the mirror image of {@link #LASER}'s
	 * {@code ENERGY} routing: armored hulls resist this, light/unarmored
	 * fliers eat it in full.
	 */
	public static final RegistryKey<DamageType> BULLET = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Program7.id("bullet"));
	 * An armor-piercing crossbow bolt's hit. Routed through {@link
	 * dev.rheava.program7.entity.ArmorProfile.DamageClass#ARMOR_PIERCING} by
	 * {@code ProgramDroneEntity#classify} — its own class, separate from
	 * {@link dev.rheava.program7.entity.ArmorProfile.DamageClass#PIERCING}
	 * (the trident's bucket), so the mod's existing typed-armor math can give
	 * this bolt an "armor is basically ignored" profile without
	 * stealth-buffing the vanilla trident, which shares {@code PIERCING}.
	 * This damage type itself carries no special-casing.
	 */
	public static final RegistryKey<DamageType> AP_BOLT = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Program7.id("ap_bolt"));

	private P7DamageTypes() {
	}
}
