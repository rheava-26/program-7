package dev.rheava.program7.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import dev.rheava.program7.block.AssemblerBlock;
import dev.rheava.program7.block.AutogunTurretBlock;
import dev.rheava.program7.block.AutogunTurretBlockEntity;
import dev.rheava.program7.block.DroneWreckBlock;
import dev.rheava.program7.block.LaunchCatapultBlock;
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
	 * but it IS crackable — and it drops the good salvage when it goes.
	 * Later phases turn this into the fabricator block entity that runs the
	 * whole base.
	 */
	public static final RegistrySupplier<Block> PROBE_CORE = BLOCKS.register("probe_core",
			() -> new Block(AbstractBlock.Settings.create()
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

	public static void register() {
		BLOCKS.register();
	}

	private P7Blocks() {
	}
}
