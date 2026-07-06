package dev.rheava.program7.registry;

import dev.rheava.program7.Program7;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class P7Blocks {
	/**
	 * The heart of a landed pod. Tough enough that cracking it is a project,
	 * but it IS crackable — and it drops the good salvage when it goes.
	 * Later phases turn this into the fabricator block entity that runs the
	 * whole base.
	 */
	public static final Block PROBE_CORE = register("probe_core", new Block(AbstractBlock.Settings.create()
			.mapColor(MapColor.GRAY)
			.strength(25.0f, 600.0f)
			.requiresTool()
			.sounds(BlockSoundGroup.NETHERITE)
			.luminance(state -> 7)));

	public static final Item PROBE_CORE_ITEM = Registry.register(Registries.ITEM, Program7.id("probe_core"),
			new BlockItem(PROBE_CORE, new Item.Settings()));

	private static Block register(String name, Block block) {
		return Registry.register(Registries.BLOCK, Program7.id(name), block);
	}

	public static void register() {
		// Static initializers above run on first reference.
	}

	private P7Blocks() {
	}
}
