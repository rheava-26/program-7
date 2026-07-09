package dev.rheava.program7.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import dev.architectury.platform.Platform;
import dev.rheava.program7.Program7;

/**
 * Program 7 configuration, stored as {@code config/program7.json}.
 * Defaults follow the design bible's config table.
 */
public class P7Config {
	/** HUD interference biases toward the screen edge facing the threat. */
	public boolean interferenceDirectionality = false;
	/** Master switch for the psionic interference HUD overlay. */
	public boolean interferenceOverlay = true;
	/** During psionic interference, vanilla music ducks out so the Program's sounds carry. */
	public boolean interferenceMusicDuck = true;
	/** Drones haul off unattended gear / death drops. */
	public boolean itemStealing = true;
	/** Drones may modify player-built structures outside active conflict. */
	public boolean interfereWithPlayerStructures = false;
	/** Program explosions break terrain (explosive weapons never respect
	 * structure protection either way). */
	public boolean terrainDestruction = true;
	/**
	 * How aggressively Program units dig and build around the player's own
	 * work. {@link DiggingPolicy#PROTECT} keeps them off player-placed blocks
	 * and out of a buffer around player builds; {@link DiggingPolicy#MINIMAL}
	 * also has them prefer caves/exposed ore over fresh shafts near you;
	 * {@link DiggingPolicy#AGGRESSIVE} lets them carve through anything. All
	 * digging still respects the {@code mobGriefing} gamerule regardless. */
	public DiggingPolicy diggingPolicy = DiggingPolicy.PROTECT;

	/** Governs how much of the world (and whose builds) Program excavation may touch. */
	public enum DiggingPolicy {
		/** Never touch player-placed blocks; stay out of a buffer around player builds. */
		PROTECT,
		/** Prefer caves and exposed ore; avoid carving fresh shafts near player territory. */
		MINIMAL,
		/** Dig through anything, player structures included — the nightmare setting. */
		AGGRESSIVE
	}
	/** Weakened Program may offer timed truces (after discovering villagers). */
	public boolean ceasefireProtocol = true;
	/** Re-insertion never stops; anti-orbital victory disabled. */
	public boolean endlessWaves = false;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static P7Config load() {
		Path path = Platform.getConfigFolder().resolve("program7.json");
		try {
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path)) {
					P7Config config = GSON.fromJson(reader, P7Config.class);
					if (config != null) {
						config.save(path); // rewrite so newly added keys appear
						return config;
					}
				}
			}
		} catch (IOException | JsonParseException e) {
			Program7.LOGGER.warn("[Program 7] Could not read config, using defaults", e);
		}
		P7Config config = new P7Config();
		config.save(path);
		return config;
	}

	private void save(Path path) {
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(this));
		} catch (IOException e) {
			Program7.LOGGER.warn("[Program 7] Could not write config", e);
		}
	}
}
