package dev.rheava.program7.registry;

import dev.rheava.program7.Program7;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

/**
 * Salvage recovered from downed Program hardware. These are the seeds of the
 * reverse-engineering tree: every piece of player tech (drones, firearms,
 * scanners, turrets) will be crafted from parts looted off the Program.
 */
public final class P7Items {
	public static final Item DRONE_CORE = register("drone_core",
			new Item(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));
	public static final Item POWER_BANK = register("power_bank",
			new Item(new Item.Settings().maxCount(16)));
	public static final Item TRANSMITTER = register("transmitter",
			new Item(new Item.Settings().maxCount(16)));
	public static final Item GUN_BARREL = register("gun_barrel",
			new Item(new Item.Settings().maxCount(16)));
	public static final Item EXPLOSIVE_WARHEAD = register("explosive_warhead",
			new Item(new Item.Settings().maxCount(16)));
	public static final Item MAGAZINE = register("magazine",
			new Item(new Item.Settings().maxCount(16)));

	public static final Item SURVEYOR_DRONE_SPAWN_EGG = register("surveyor_drone_spawn_egg",
			new SpawnEggItem(P7Entities.SURVEYOR_DRONE, 0x23272b, 0x27e2d3, new Item.Settings()));

	public static final RegistryKey<ItemGroup> MAIN_GROUP = RegistryKey.of(RegistryKeys.ITEM_GROUP,
			Program7.id("main"));

	private static Item register(String name, Item item) {
		return Registry.register(Registries.ITEM, Program7.id(name), item);
	}

	public static void register() {
		Registry.register(Registries.ITEM_GROUP, MAIN_GROUP, FabricItemGroup.builder()
				.icon(() -> new ItemStack(DRONE_CORE))
				.displayName(Text.translatable("itemGroup.program7.main"))
				.build());

		ItemGroupEvents.modifyEntriesEvent(MAIN_GROUP).register(entries -> {
			entries.add(DRONE_CORE);
			entries.add(POWER_BANK);
			entries.add(TRANSMITTER);
			entries.add(GUN_BARREL);
			entries.add(EXPLOSIVE_WARHEAD);
			entries.add(MAGAZINE);
			entries.add(SURVEYOR_DRONE_SPAWN_EGG);
		});
	}

	private P7Items() {
	}
}
