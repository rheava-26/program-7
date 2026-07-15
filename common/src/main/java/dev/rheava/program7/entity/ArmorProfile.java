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
		/**
		 * Armor-piercing punches through plate — a trident's stabbing thrust,
		 * or a fired {@code ArmorPiercingArrowEntity} (armor-piercing
		 * crossbow bolt). The dedicated anti-vehicle lane: see {@code
		 * docs/DESIGN.md}'s "Damage & armor is typed" section.
		 */
		PIERCING,
		/** Explosions. */
		EXPLOSIVE,
		/** Enchanted melee weapons (Sharpness/Smite/Bane/Impaling). */
		ENCHANTED,
		/** Plain, unenchanted melee. */
		MELEE,
		/**
		 * Coherent-light weapons — currently just the player's charge laser.
		 * Deliberately the mirror image of {@link #BALLISTIC}: a naked
		 * airframe or light hull has nothing to boil the beam off against and
		 * melts fast, but real plate soaks and disperses it, so hosing an
		 * armored vehicle down with this is a genuinely bad, battery-wasting
		 * trade rather than just "a bit less good."
		 */
		ENERGY,
		/** Anything that doesn't fit a more specific bucket. */
		GENERIC
	}

	public static final ArmorProfile UNARMORED = new ArmorProfile(
			1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.6f, 1.0f);

	public static final ArmorProfile LIGHT = new ArmorProfile(
			0.9f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.3f, 1.0f);

	/**
	 * Bullets bounce off sloped plate, arrows/tridents that slam in transfer
	 * more, crossbow-AP punches through (1.3x — a genuine specialist counter,
	 * second only to the enchanted/psionic bypass), and enchanted/psionic
	 * weapons bypass armor outright. The beam fares worst of all here (0.2x)
	 * — plate disperses coherent light far better than it stops a bullet.
	 */
	public static final ArmorProfile ARMORED_VEHICLE = new ArmorProfile(
			0.45f, 1.25f, 1.3f, 1.1f, 1.4f, 0.8f, 0.2f, 0.8f);

	/** The toughest hull in the game — even more resistant than a light vehicle. */
	public static final ArmorProfile HEAVY_HULL = new ArmorProfile(
			0.35f, 1.15f, 1.2f, 1.0f, 1.3f, 0.7f, 0.15f, 0.7f);

	private final float[] multipliers;

	private ArmorProfile(float ballistic, float highVelocityImpact, float piercing,
			float explosive, float enchanted, float melee, float energy, float generic) {
		this.multipliers = new float[DamageClass.values().length];
		this.multipliers[DamageClass.BALLISTIC.ordinal()] = ballistic;
		this.multipliers[DamageClass.HIGH_VELOCITY_IMPACT.ordinal()] = highVelocityImpact;
		this.multipliers[DamageClass.PIERCING.ordinal()] = piercing;
		this.multipliers[DamageClass.EXPLOSIVE.ordinal()] = explosive;
		this.multipliers[DamageClass.ENCHANTED.ordinal()] = enchanted;
		this.multipliers[DamageClass.MELEE.ordinal()] = melee;
		this.multipliers[DamageClass.ENERGY.ordinal()] = energy;
		this.multipliers[DamageClass.GENERIC.ordinal()] = generic;
	}

	public float multiplierFor(DamageClass damageClass) {
		return this.multipliers[damageClass.ordinal()];
	}
}
