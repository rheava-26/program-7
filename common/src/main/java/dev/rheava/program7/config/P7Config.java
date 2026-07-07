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
	/** Drones haul off unattended gear / death drops. */
	public boolean itemStealing = true;
	/** Drones may modify player-built structures outside active conflict. */
	public boolean interfereWithPlayerStructures = false;
	/** Program explosions break terrain (explosive weapons never respect
	 * structure protection either way). */
	public boolean terrainDestruction = true;
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
