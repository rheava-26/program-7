package dev.rheava.program7.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.rheava.program7.director.Resources;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Shared contract for the courier units running a supply mission: haul a
 * cargo of ledger resources from the probe core out to a waiting assembler.
 * The mission is the whole point of putting the payment in the world —
 * shoot the courier down before it docks and the cargo is yours.
 */
public interface CourierUnit {
	/** Where the cargo is headed, or {@code null} if this unit isn't on a run. */
	@Nullable
	BlockPos getDestination();

	/** Which assembler job this delivery is paying for. */
	String getJob();

	/** Ledger units currently strapped to this courier. */
	Map<String, Integer> getCargo();

	/** Docked, destroyed, or aborted — clears the mission fields. */
	void clearMission();

	/** Turns a ledger cargo into the item stacks it represents on the ground. */
	static List<ItemStack> cargoToItems(Map<String, Integer> cargo) {
		List<ItemStack> stacks = new ArrayList<>();
		cargo.forEach((resource, amount) -> {
			if (amount <= 0) {
				return;
			}
			Item item = switch (resource) {
				case Resources.IRON -> Items.IRON_INGOT;
				case Resources.COPPER -> Items.COPPER_INGOT;
				case Resources.REDSTONE -> Items.REDSTONE;
				case Resources.COAL -> Items.COAL;
				case Resources.GUNPOWDER -> Items.GUNPOWDER;
				default -> null;
			};
			if (item != null) {
				stacks.add(new ItemStack(item, amount));
			}
		});
		return stacks;
	}
}
