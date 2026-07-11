package dev.rheava.program7.entity;

import net.minecraft.util.math.Vec3d;

/**
 * The gun side of the magazine/rearm contract (see
 * {@code docs/ARTILLERY_AND_INDIRECT_FIRE.md} §6a — this is the
 * <b>rearming</b> half, not the per-shot <b>loading</b> half). Any mount
 * with a finite ready magazine implements this so a logistics unit can find
 * it, check whether it's running dry, and top it back up without knowing
 * anything else about the mount — no ammo type, no firing goal, just "how
 * full are you" and "where do I drop the box."
 *
 * <p>Implementors are responsible for actually gating their own fire control
 * on {@link #needsReload()} (or an equivalent internal check) and for
 * decrementing their own round count as shots go out — this interface only
 * covers the resupply side of the contract.
 */
public interface ReloadableWeapon {
	int getRoundsRemaining();

	int getMagazineCapacity();

	default boolean needsReload() {
		return getRoundsRemaining() <= 0;
	}

	void loadRounds(int rounds); // called when an ammo box is delivered

	Vec3d getWeaponPos(); // where a logistics drone delivers
}
