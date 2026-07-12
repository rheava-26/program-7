package dev.rheava.program7.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import dev.rheava.program7.block.AmmoBoxBlock;
import dev.rheava.program7.block.ArtilleryShellBlock;
import dev.rheava.program7.block.AssemblerBlock;
import dev.rheava.program7.block.AutocannonMagazineBlock;
import dev.rheava.program7.block.AutogunTurretBlock;
import dev.rheava.program7.block.AutogunTurretBlockEntity;
import dev.rheava.program7.block.BarrierPostBlock;
import dev.rheava.program7.block.CrateBlock;
import dev.rheava.program7.block.DroneWreckBlock;
import dev.rheava.program7.block.PowerCellBlock;
import dev.rheava.program7.block.FuelPlantBlock;
import dev.rheava.program7.block.LaunchCatapultBlock;
import dev.rheava.program7.block.PlatingBlock;
import dev.rheava.program7.block.ProbeCoreBlock;
import dev.rheava.program7.block.StorageDeckBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;

public final class P7Blocks {
	public static final DeferredRegister<Block> BLOCKS =
			DeferredRegister.create(Program7.MOD_ID, RegistryKeys.BLOCK);

	/**
	 * The heart of a landed pod. Tough enough that cracking it is a project,
	 * but it IS crackable — and it drops the good salvage when it goes. A
	 * block entity ({@link dev.rheava.program7.block.ProbeCoreBlockEntity})
	 * carries the HP/under-attack model and reports to the Director when the
	 * base is actually destroyed.
	 */
	public static final RegistrySupplier<Block> PROBE_CORE = BLOCKS.register("probe_core",
			() -> new ProbeCoreBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(25.0f, 600.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.NETHERITE)
					.luminance(state -> 7)));

	/** Crash-site remains of a destroyed drone; holds its salvage. */
	public static final RegistrySupplier<Block> DRONE_WRECK = BLOCKS.register("drone_wreck",
			() -> new DroneWreckBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(1.0f)
					.sounds(BlockSoundGroup.NETHERITE)
					.nonOpaque()));

	/** The Program's Tier 1 production building: battery + assembler. */
	public static final RegistrySupplier<Block> ASSEMBLER = BLOCKS.register("assembler",
			() -> new AssemblerBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(12.0f, 300.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.NETHERITE)
					.luminance(state -> 5)));

	/**
	 * Tier 1 fixed defense: a twin-barrel autogun bolted to a pedestal. A
	 * static, destructible block instead of a mob — it holds its ground and
	 * can't be shoved off post, but it also can't be knocked around or
	 * pathed away from a fight. See {@link AutogunTurretBlockEntity} for the
	 * targeting/firing/overheat logic.
	 */
	public static final RegistrySupplier<Block> AUTOGUN_TURRET = BLOCKS.register("autogun_turret",
			() -> new AutogunTurretBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(8.0f, 200.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.NETHERITE)
					.luminance(state -> 3)));

	/** Support infrastructure: no catapult, no UAVs. Break it and the base goes blind upstairs. */
	public static final RegistrySupplier<Block> LAUNCH_CATAPULT = BLOCKS.register("launch_catapult",
			() -> new LaunchCatapultBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(8.0f, 200.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.NETHERITE)));

	/**
	 * Cheap Program construction block. This is what an incrementally-built
	 * outpost ({@code ConstructionSite} in {@link
	 * dev.rheava.program7.director.ProgramDirectorState}) is built out of —
	 * plain, no block entity, just structural filler.
	 */
	public static final RegistrySupplier<Block> METAL_SCAFFOLD = BLOCKS.register("metal_scaffold",
			() -> new Block(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(4.0f, 30.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.NETHERITE)));

	/** The Program's physical stockpile: a lootable crate-bank the player can raid a base for. */
	public static final RegistrySupplier<Block> STORAGE_DECK = BLOCKS.register("storage_deck",
			() -> new StorageDeckBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(6.0f, 200.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.NETHERITE)));

	/**
	 * The Program's one depot block in the fuel-upkeep slice: converts
	 * ledger coal into FUEL for Tier 2/3 fliers within its bubble (see
	 * {@link dev.rheava.program7.director.SupplyNetwork}). Between the
	 * autogun turret (8/200) and the assembler (12/300): a real demolition
	 * target for an iron-pick player, not a drive-by freebie.
	 */
	public static final RegistrySupplier<Block> FUEL_PLANT = BLOCKS.register("fuel_plant",
			() -> new FuelPlantBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(10.0f, 250.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.NETHERITE)
					.luminance(state -> 5)));

	/** Metal deck plate: the Program's landing-pad / stockpile-apron flooring. */
	public static final RegistrySupplier<Block> PLATING = BLOCKS.register("plating",
			() -> new PlatingBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(4.0f, 30.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.NETHERITE)));

	/** Mechanical barrier post ringing a fresh landing pad — a re-themed fence. */
	public static final RegistrySupplier<Block> BARRIER_POST = BLOCKS.register("barrier_post",
			() -> new BarrierPostBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(4.0f, 30.0f)
					.sounds(BlockSoundGroup.NETHERITE)));

	/** Stacking ammo crate pile (1-4), sea-pickle style — the physical unit AmmoRunGoal draws down. */
	public static final RegistrySupplier<Block> AMMO_BOX = BLOCKS.register("ammo_box",
			() -> new AmmoBoxBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(1.0f)
					.sounds(BlockSoundGroup.NETHERITE)
					.nonOpaque()));

	/** General-cargo shipping crate the drones haul non-modded loot in. */
	public static final RegistrySupplier<Block> CRATE = BLOCKS.register("crate",
			() -> new CrateBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(1.0f)
					.sounds(BlockSoundGroup.NETHERITE)
					.nonOpaque()));

	/** Tall autocannon magazine cargo (1-4) — medium-weapon rounds. */
	public static final RegistrySupplier<Block> AUTOCANNON_MAGAZINE = BLOCKS.register("autocannon_magazine",
			() -> new AutocannonMagazineBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GRAY)
					.strength(1.0f)
					.sounds(BlockSoundGroup.NETHERITE)
					.luminance(state -> 4)
					.nonOpaque()));

	/** Large single artillery shell cargo (1-4) — howitzer / deck-gun rounds. */
	public static final RegistrySupplier<Block> ARTILLERY_SHELL = BLOCKS.register("artillery_shell",
			() -> new ArtilleryShellBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GOLD)
					.strength(1.0f)
					.sounds(BlockSoundGroup.NETHERITE)
					.luminance(state -> 4)
					.nonOpaque()));

	/** Power cell / battery cargo (1-4) — the charge drones need alongside ammo. */
	public static final RegistrySupplier<Block> POWER_CELL = BLOCKS.register("power_cell",
			() -> new PowerCellBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.BLACK)
					.strength(1.0f)
					.sounds(BlockSoundGroup.NETHERITE)
					.luminance(state -> 6)
					.nonOpaque()));

	public static void register() {
		BLOCKS.register();
	}

	private P7Blocks() {
	}
}
