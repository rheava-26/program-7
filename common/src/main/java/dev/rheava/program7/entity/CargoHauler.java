package dev.rheava.program7.entity;

import java.util.Map;

/**
 * Shared contract for units that grind resources out of the ground and bank
 * them at the probe core: the harvester and its airborne Tier 2 counterpart
 * both drive the same mine-and-deposit goals against this interface.
 */
public interface CargoHauler {
	/** Whether the hopper is full and the unit should head home to bank it. */
	boolean isCargoFull();

	/** Ledger units currently sitting in the hopper. */
	int cargoTotal();

	/** Adds mined-out yield to the hopper. */
	void addCargo(String resource, int amount);

	/** Hands over everything and empties the hopper. */
	Map<String, Integer> drainCargo();
}
