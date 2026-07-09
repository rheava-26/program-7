package dev.rheava.program7.director;

/**
 * Keys for the Director's resource ledger. Kept as plain strings so the
 * ledger serializes trivially and later phases can add entries (diamonds,
 * emeralds, netherite…) without migrations.
 */
public final class Resources {
	public static final String IRON = "iron";
	public static final String COPPER = "copper";
	public static final String REDSTONE = "redstone";
	public static final String COAL = "coal";
	public static final String GUNPOWDER = "gunpowder";
	public static final String WOOD = "wood";

	// Tier-2+ material tree (see EXPANSION_PLAN C6 / RESEARCH_AND_LOGISTICS).
	// String keys, so the ledger and future research gates extend without NBT
	// migrations. Acquisition of the deeper materials leans on tunnelling (C1)
	// and a Nether/ocean presence, which arrive later.
	public static final String GOLD = "gold";
	public static final String AMETHYST = "amethyst";
	public static final String DIAMOND = "diamond";
	public static final String GLOWSTONE = "glowstone";
	public static final String PRISMARINE = "prismarine";
	public static final String NETHERITE = "netherite";
	/** Smelted up from copper rather than mined — the electronics/conductor feedstock. */
	public static final String CONDUCTOR = "conductor";

	private Resources() {
	}
}
