package dev.rheava.program7;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.command.Program7Command;
import dev.rheava.program7.config.P7Config;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.network.InterferenceManager;
import dev.rheava.program7.registry.P7BlockEntities;
import dev.rheava.program7.registry.P7Blocks;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Items;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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

		// The Program hears you: breaking a hard block (stone and up) is a loud,
		// carrying noise nearby combat units can drift over to investigate — the
		// "something heard me" beat (see ProgramAcoustics#reportNoise +
		// InvestigateNoiseGoal). Soft blocks (dirt, leaves, crops) stay quiet.
		BlockEvent.BREAK.register((world, pos, state, player, xp) -> {
			if (world instanceof ServerWorld serverWorld) {
				float hardness = state.getHardness(serverWorld, pos);
				if (hardness >= 0.8f) {
					float loudness = MathHelper.clamp(hardness / 3.0f, 0.0f, 1.0f);
					ProgramAcoustics.reportNoise(serverWorld,
							pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, loudness);
				}
			}
			return EventResult.pass();
		});

		// The Director thinks once per overworld tick: insertion schedule,
		// pending dispatches, and (later) base production.
		TickEvent.SERVER_LEVEL_POST.register(world -> {
			if (world.getRegistryKey() == World.OVERWORLD) {
				ProgramDirectorState.get(world).tick(world);
			}
			// Acoustics queue ticks for every dimension — weapons fire in the
			// Nether too, not just the overworld the Director cares about.
			ProgramAcoustics.tick(world);
		});

		LOGGER.info("[Program 7] Probe telemetry online. Awaiting insertion window.");
	}

	private Program7() {
	}
}
