package dev.rheava.program7.director;

/**
 * A single completed surveyor scan of a player.
 *
 * @param riskTier      1 = low (little gear / many deaths), 2 = medium (iron-grade),
 *                      3 = high (diamond-grade and above)
 * @param weaponProfile dominant weapon class seen on the player at scan time —
 *                      this is what the Program counters against ("ranged" gets
 *                      swarmed by explosive drones, "melee" gets harassers, etc.)
 * @param elytra        whether an elytra was equipped; flags the player for
 *                      instant-reaction interception weapons later
 * @param deaths        the player's lifetime death count in this world
 */
public record ScanRecord(int riskTier, String weaponProfile, boolean elytra, int deaths) {
	public static final String PROFILE_NONE = "none";
	public static final String PROFILE_MELEE = "melee";
	public static final String PROFILE_RANGED = "ranged";
}
