package dev.rheava.program7.entity;

/**
 * Typed armor: instead of one flat damage reduction, a unit resists (or is
 * extra-vulnerable to) damage by {@link DamageClass}. This is the mod's
 * mod-compatibility thesis made concrete — a realistic-gun-mod player hosing
 * an armored vehicle with ballistic weapons and a vanilla bow player plinking
 * away with arrows are fighting genuinely different fights against the same
 * target, not the same fight with a different skin.
 *
 * <p>A multiplier below {@code 1.0} means the class is resisted; above
 * {@code 1.0} means the unit is extra-vulnerable to it.
 */
public final class ArmorProfile {
	/** Broad buckets of "how did this damage arrive," used to key resistance. */
	public enum DamageClass {
		/** Small arms and other conventional gunfire — most gun-mod bullets. */
		BALLISTIC,
		/** Fast-moving kinetic slams, e.g. arrows/spectral arrows. */
		HIGH_VELOCITY_IMPACT,
		/** Armor-piercing punches through plate, e.g. tridents. */
		PIERCING,
		/** Explosions. */
		EXPLOSIVE,
		/** Enchanted melee weapons (Sharpness/Smite/Bane/Impaling). */
		ENCHANTED,
		/** Plain, unenchanted melee. */
		MELEE,
		/** Anything that doesn't fit a more specific bucket. */
		GENERIC
	}

	public static final ArmorProfile UNARMORED = new ArmorProfile(
			1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);

	public static final ArmorProfile LIGHT = new ArmorProfile(
			0.9f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);

	/**
	 * Bullets bounce off sloped plate, arrows/tridents that slam in transfer
	 * more, crossbow-AP punches through, and enchanted/psionic weapons bypass
	 * armor outright.
	 */
	public static final ArmorProfile ARMORED_VEHICLE = new ArmorProfile(
			0.45f, 1.25f, 0.85f, 1.1f, 1.4f, 0.8f, 0.8f);

	/** The toughest hull in the game — even more resistant than a light vehicle. */
	public static final ArmorProfile HEAVY_HULL = new ArmorProfile(
			0.35f, 1.15f, 0.8f, 1.0f, 1.3f, 0.7f, 0.7f);

	private final float[] multipliers;

	private ArmorProfile(float ballistic, float highVelocityImpact, float piercing,
			float explosive, float enchanted, float melee, float generic) {
		this.multipliers = new float[DamageClass.values().length];
		this.multipliers[DamageClass.BALLISTIC.ordinal()] = ballistic;
		this.multipliers[DamageClass.HIGH_VELOCITY_IMPACT.ordinal()] = highVelocityImpact;
		this.multipliers[DamageClass.PIERCING.ordinal()] = piercing;
		this.multipliers[DamageClass.EXPLOSIVE.ordinal()] = explosive;
		this.multipliers[DamageClass.ENCHANTED.ordinal()] = enchanted;
		this.multipliers[DamageClass.MELEE.ordinal()] = melee;
		this.multipliers[DamageClass.GENERIC.ordinal()] = generic;
	}

	public float multiplierFor(DamageClass damageClass) {
		return this.multipliers[damageClass.ordinal()];
	}
}
