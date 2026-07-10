package dev.rheava.program7.registry;

import dev.architectury.core.item.ArchitecturySpawnEggItem;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import dev.rheava.program7.item.DatapadItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

/**
 * Salvage recovered from downed Program hardware. These are the seeds of the
 * reverse-engineering tree: every piece of player tech (drones, firearms,
 * scanners, turrets) will be crafted from parts looted off the Program.
 */
public final class P7Items {
	public static final DeferredRegister<ItemGroup> TABS =
			DeferredRegister.create(Program7.MOD_ID, RegistryKeys.ITEM_GROUP);
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(Program7.MOD_ID, RegistryKeys.ITEM);

	public static final RegistrySupplier<ItemGroup> MAIN_TAB = TABS.register("main",
			() -> CreativeTabRegistry.create(Text.translatable("itemGroup.program7.main"),
					() -> new ItemStack(P7Items.DRONE_CORE.get())));

	public static final RegistrySupplier<Item> DRONE_CORE = ITEMS.register("drone_core",
			() -> new Item(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON).arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> POWER_BANK = ITEMS.register("power_bank",
			() -> new Item(new Item.Settings().maxCount(16).arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> TRANSMITTER = ITEMS.register("transmitter",
			() -> new Item(new Item.Settings().maxCount(16).arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> GUN_BARREL = ITEMS.register("gun_barrel",
			() -> new Item(new Item.Settings().maxCount(16).arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> EXPLOSIVE_WARHEAD = ITEMS.register("explosive_warhead",
			() -> new Item(new Item.Settings().maxCount(16).arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> MAGAZINE = ITEMS.register("magazine",
			() -> new Item(new Item.Settings().maxCount(16).arch$tab(MAIN_TAB)));

	public static final RegistrySupplier<Item> DATAPAD = ITEMS.register("datapad",
			() -> new DatapadItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON).arch$tab(MAIN_TAB)));

	public static final RegistrySupplier<Item> PROBE_CORE_ITEM = ITEMS.register("probe_core",
			() -> new BlockItem(P7Blocks.PROBE_CORE.get(), new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> ASSEMBLER = ITEMS.register("assembler",
			() -> new BlockItem(P7Blocks.ASSEMBLER.get(), new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> LAUNCH_CATAPULT = ITEMS.register("launch_catapult",
			() -> new BlockItem(P7Blocks.LAUNCH_CATAPULT.get(), new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> AUTOGUN_TURRET_BLOCK = ITEMS.register("autogun_turret",
			() -> new BlockItem(P7Blocks.AUTOGUN_TURRET.get(), new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> METAL_SCAFFOLD = ITEMS.register("metal_scaffold",
			() -> new BlockItem(P7Blocks.METAL_SCAFFOLD.get(), new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> STORAGE_DECK_BLOCK = ITEMS.register("storage_deck",
			() -> new BlockItem(P7Blocks.STORAGE_DECK.get(), new Item.Settings().arch$tab(MAIN_TAB)));

	public static final RegistrySupplier<Item> SURVEYOR_DRONE_SPAWN_EGG = ITEMS.register("surveyor_drone_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.SURVEYOR_DRONE, 0x23272b, 0x27e2d3,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> ATTACK_DRONE_SPAWN_EGG = ITEMS.register("attack_drone_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.ATTACK_DRONE, 0x23272b, 0xe83030,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> HARVESTER_DRONE_SPAWN_EGG = ITEMS.register("harvester_drone_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.HARVESTER_DRONE, 0x23272b, 0xdeb12d,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> GROUND_DRONE_SPAWN_EGG = ITEMS.register("ground_drone_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.GROUND_DRONE, 0x23272b, 0xc23b2e,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> AUTOGUN_TURRET_SPAWN_EGG = ITEMS.register("autogun_turret_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.AUTOGUN_TURRET, 0x23272b, 0x8a93a0,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> LOGISTICS_DRONE_SPAWN_EGG = ITEMS.register("logistics_drone_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.LOGISTICS_DRONE, 0x23272b, 0xdeb12d,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> WHEELED_HAULER_SPAWN_EGG = ITEMS.register("wheeled_hauler_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.WHEELED_HAULER, 0x23272b, 0x9a7b24,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> MEDIUM_ATTACK_DRONE_SPAWN_EGG =
			ITEMS.register("medium_attack_drone_spawn_egg",
					() -> new ArchitecturySpawnEggItem(P7Entities.MEDIUM_ATTACK_DRONE, 0x23272b, 0xd06028,
							new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> SNIPER_DRONE_SPAWN_EGG = ITEMS.register("sniper_drone_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.SNIPER_DRONE, 0x23272b, 0x4a90d9,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> MORTAR_EMPLACEMENT_SPAWN_EGG =
			ITEMS.register("mortar_emplacement_spawn_egg",
					() -> new ArchitecturySpawnEggItem(P7Entities.MORTAR_EMPLACEMENT, 0x23272b, 0x6b7684,
							new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> MEDIUM_MINING_DRONE_SPAWN_EGG =
			ITEMS.register("medium_mining_drone_spawn_egg",
					() -> new ArchitecturySpawnEggItem(P7Entities.MEDIUM_MINING_DRONE, 0x23272b, 0xb9932a,
							new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> TRANSPORT_DRONE_SPAWN_EGG =
			ITEMS.register("transport_drone_spawn_egg",
					() -> new ArchitecturySpawnEggItem(P7Entities.TRANSPORT_DRONE, 0x23272b, 0xe8c84a,
							new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> ANTI_AIR_TURRET_SPAWN_EGG =
			ITEMS.register("anti_air_turret_spawn_egg",
					() -> new ArchitecturySpawnEggItem(P7Entities.ANTI_AIR_TURRET, 0x23272b, 0x8fb7d1,
							new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> SCOUT_CAR_SPAWN_EGG = ITEMS.register("scout_car_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.SCOUT_CAR, 0x23272b, 0x27e2d3,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> AIR_UAV_SPAWN_EGG = ITEMS.register("air_uav_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.AIR_UAV, 0x23272b, 0xa8c8e0,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> HEAVY_ATTACK_DRONE_SPAWN_EGG =
			ITEMS.register("heavy_attack_drone_spawn_egg",
					() -> new ArchitecturySpawnEggItem(P7Entities.HEAVY_ATTACK_DRONE, 0x23272b, 0xb32020,
							new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> IFV_SPAWN_EGG = ITEMS.register("ifv_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.IFV, 0x23272b, 0x5a6b3a,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> GUNBOAT_SPAWN_EGG = ITEMS.register("gunboat_spawn_egg",
			() -> new ArchitecturySpawnEggItem(P7Entities.GUNBOAT, 0x23272b, 0x2f4f6f,
					new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> RECON_HELICOPTER_SPAWN_EGG =
			ITEMS.register("recon_helicopter_spawn_egg",
					() -> new ArchitecturySpawnEggItem(P7Entities.RECON_HELICOPTER, 0x23272b, 0x62d0e8,
							new Item.Settings().arch$tab(MAIN_TAB)));
	public static final RegistrySupplier<Item> BATTERY_CENTER_SPAWN_EGG =
			ITEMS.register("battery_center_spawn_egg",
					() -> new ArchitecturySpawnEggItem(P7Entities.BATTERY_CENTER, 0x23272b, 0xf0e13a,
							new Item.Settings().arch$tab(MAIN_TAB)));

	public static void register() {
		TABS.register();
		ITEMS.register();
	}

	private P7Items() {
	}
}
