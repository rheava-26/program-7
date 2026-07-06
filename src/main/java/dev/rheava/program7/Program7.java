package dev.rheava.program7;

import dev.rheava.program7.command.Program7Command;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Items;
import dev.rheava.program7.registry.P7Sounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PROGRAM 7 — an automated orbital probe program, field-testing long-duration
 * autonomous resource extraction while its crew sleeps in cryogenesis.
 *
 * <p>The Program lands, mines, builds, adapts to how you fight, and escalates
 * alongside you. Everything it fields can be destroyed, salvaged and turned
 * against it.
 */
public class Program7 implements ModInitializer {
	public static final String MOD_ID = "program7";
	public static final Logger LOGGER = LoggerFactory.getLogger("Program 7");

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		P7Sounds.register();
		P7Entities.register();
		P7Items.register();
		Program7Command.register();

		LOGGER.info("[Program 7] Probe telemetry online. Awaiting insertion window.");
	}
}
