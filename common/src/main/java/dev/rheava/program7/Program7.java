package dev.rheava.program7;

import dev.architectury.event.events.common.TickEvent;
import dev.rheava.program7.command.Program7Command;
import dev.rheava.program7.config.P7Config;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.network.InterferenceManager;
import dev.rheava.program7.registry.P7BlockEntities;
import dev.rheava.program7.registry.P7Blocks;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Items;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PROGRAM 7 — an automated orbital probe program, field-testing long-duration
 * autonomous resource extraction while its crew sleeps in cryogenesis.
 *
 * <p>The Program lands, mines, builds, adapts to how you fight, and escalates
 * alongside you. Everything it fields can be destroyed, salvaged and turned
 * against it.
 *
 * <p>Loader-agnostic core: the Fabric and NeoForge modules call
 * {@link #init()} from their respective entrypoints.
 */
public final class Program7 {
	public static final String MOD_ID = "program7";
	public static final Logger LOGGER = LoggerFactory.getLogger("Program 7");
	public static final P7Config CONFIG = P7Config.load();

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	public static void init() {
		P7Sounds.register();
		P7Blocks.register();
		P7BlockEntities.register();
		P7Entities.register();
		P7Items.register();
		Program7Command.register();
		InterferenceManager.register();

		// The Director thinks once per overworld tick: insertion schedule,
		// pending dispatches, and (later) base production.
		TickEvent.SERVER_LEVEL_POST.register(world -> {
			if (world.getRegistryKey() == World.OVERWORLD) {
				ProgramDirectorState.get(world).tick(world);
			}
		});

		LOGGER.info("[Program 7] Probe telemetry online. Awaiting insertion window.");
	}

	private Program7() {
	}
}
