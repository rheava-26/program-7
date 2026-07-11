package dev.rheava.program7.entity.ai;

/**
 * How "big" a hitscan round is, independent of the exact damage number a
 * given mount happens to be tuned to. Every hitscan weapon in the arsenal
 * picks one of these three and hands it to {@link HitscanImpact#resolve} on
 * every shot, so a small autogun turret's rounds visibly and mechanically
 * read as lighter than an IFV autocannon's, which in turn reads lighter than
 * the gunship's heavy belly gun — same tracer-and-hitscan bones, different
 * caliber.
 *
 * <p>Per-weapon damage stays tuned individually at each mount's own call
 * site (see e.g. {@code AutogunTurretEntity}, {@code IFVEntity},
 * {@code GunshipEntity}) rather than being derived from the class here — two
 * MEDIUM platforms can still have different fire rates and damage numbers.
 * What this enum standardizes is everything that should scale with caliber
 * on top of that: how hard a hit shoves its target, how big the impact puff
 * reads, and how much a single round chews into terrain.
 *
 * <ul>
 *   <li>{@link #LIGHT} — small autogun/flak turrets, light drone-mounted guns.</li>
 *   <li>{@link #MEDIUM} — IFV/autocannon and gunboat deck-gun caliber.</li>
 *   <li>{@link #HEAVY} — the gunship's heavy belly autocannon (and the
 *       sniper's rifle round) — clearly the hardest-hitting rounds in the
 *       arsenal.</li>
 * </ul>
 */
public enum RoundClass {
	LIGHT(0.15, 0.8f, 0.85f, 0.4f),
	MEDIUM(0.22, 1.0f, 1.0f, 0.55f),
	HEAVY(0.32, 1.5f, 1.2f, 0.75f);

	private final double knockbackStrength;
	private final float particleCountScale;
	private final float particleSpreadScale;
	private final float terrainChipWeight;

	RoundClass(double knockbackStrength, float particleCountScale, float particleSpreadScale,
			float terrainChipWeight) {
		this.knockbackStrength = knockbackStrength;
		this.particleCountScale = particleCountScale;
		this.particleSpreadScale = particleSpreadScale;
		this.terrainChipWeight = terrainChipWeight;
	}

	/**
	 * Knockback impulse for a hit of this class, deliberately below vanilla
	 * melee's ~0.4 baseline even at {@link #HEAVY} — see the gun-feel pass:
	 * rapid fire bypasses the target's invulnerability window so hits
	 * actually stack, and a full vanilla shove on every one of those would
	 * juggle the target instead of just hitting hard.
	 */
	public double knockbackStrength() {
		return this.knockbackStrength;
	}

	/** Multiplier on {@link HitscanImpact}'s impact-particle counts. */
	public float particleCountScale() {
		return this.particleCountScale;
	}

	/** Multiplier on {@link HitscanImpact}'s impact-particle spread. */
	public float particleSpreadScale() {
		return this.particleSpreadScale;
	}

	/** Chip damage this round class adds to {@link HitscanImpact}'s per-block accumulator on every hit. */
	public float terrainChipWeight() {
		return this.terrainChipWeight;
	}
}
