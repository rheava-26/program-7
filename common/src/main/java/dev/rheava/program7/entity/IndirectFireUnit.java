package dev.rheava.program7.entity;

/**
 * Marks a mount as a client of the Director-side
 * {@code dev.rheava.program7.director.FireMissionManager} (see
 * {@code ARTILLERY_AND_INDIRECT_FIRE.md} §8) — anything that lobs, arcs, or
 * drops munitions onto a target through the shared fire-mission loop rather
 * than shooting what it directly sees.
 *
 * <p>Implementors are {@code MobEntity}s (the manager only ever iterates
 * loaded entities and checks {@code instanceof IndirectFireUnit}); this
 * interface itself stays entity-agnostic so the manager doesn't have to know
 * every concrete artillery type — a howitzer, a mortar, an MLRS launcher, a
 * gunboat's deck gun, and a missile battery all implement it the same way.
 * {@code battery()} is what lets {@link
 * dev.rheava.program7.director.FireMissionManager} group several tubes of
 * the <em>same family</em> standing near each other onto one shared mission
 * (the artillery doc's §4 "several tubes ranging together") without
 * accidentally pooling, say, a howitzer and an MLRS launcher that happen to
 * be parked side by side.
 */
public interface IndirectFireUnit {
	/** Standoff floor: the manager won't hand this tube a designation closer than this. */
	double indirectMinRange();

	/** Maximum reach: the manager won't hand this tube a designation farther than this. */
	double indirectMaxRange();

	/**
	 * The type's signature CEP (§3 "baseSpread") before range/spotting/ranging
	 * scale it — a rocket battery's shotgun spread vs. a howitzer's rifle
	 * precision vs. a guided missile's near-zero base.
	 */
	double indirectBaseSpread();

	/**
	 * Roughly how big a bang this type's shell makes — handed to {@code
	 * World#createExplosion} by {@code FireMissionManager}'s off-screen
	 * statistical resolution (doc §7), which has no real shell entity to ask.
	 * Matches the munition's own {@code AbstractShellEntity} subclass's
	 * explosion power closely enough that a barrage the player isn't
	 * standing near still reads as roughly the right size if its target
	 * chunk happens to be loaded. {@code 2.0} by default.
	 */
	default float indirectImpactPower() {
		return 2.0f;
	}

	/**
	 * The battery-grouping key: tubes with equal, non-null keys standing near
	 * each other (see {@code FireMissionManager}'s battery radius) share a
	 * single {@code FireMission} so their ranging walks in together and their
	 * fire-for-effect lands as one denser salvo instead of racing separately.
	 * {@code null} opts a mount out of battery-sharing entirely (mobile
	 * pieces that "usually fire alone" per the platform-split doc).
	 */
	default String battery() {
		return this.getClass().getSimpleName();
	}
}
