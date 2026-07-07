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

	private Resources() {
	}
}
