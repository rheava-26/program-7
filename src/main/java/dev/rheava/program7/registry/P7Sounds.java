package dev.rheava.program7.registry;

import dev.rheava.program7.Program7;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Sound events for the Program's machinery.
 *
 * <p>All entries currently redirect to vanilla sounds as placeholders (see
 * {@code assets/program7/sounds.json}); swapping in the real whirring /
 * beeping / interference recordings later only requires replacing the ogg
 * references in that file — no code changes.
 */
public final class P7Sounds {
	public static final SoundEvent DRONE_AMBIENT = register("entity.surveyor_drone.ambient");
	public static final SoundEvent DRONE_ALERT = register("entity.surveyor_drone.alert");
	public static final SoundEvent DRONE_SCAN_BEEP = register("entity.surveyor_drone.scan_beep");
	public static final SoundEvent DRONE_INTERFERENCE = register("entity.surveyor_drone.interference");
	public static final SoundEvent DRONE_HURT = register("entity.surveyor_drone.hurt");
	public static final SoundEvent DRONE_DEATH = register("entity.surveyor_drone.death");

	private static SoundEvent register(String name) {
		Identifier id = Program7.id(name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void register() {
		// Static initializers above run on first reference.
	}

	private P7Sounds() {
	}
}
