package dev.rheava.program7.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import net.minecraft.registry.RegistryKeys;
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
	public static final DeferredRegister<SoundEvent> SOUNDS =
			DeferredRegister.create(Program7.MOD_ID, RegistryKeys.SOUND_EVENT);

	public static final RegistrySupplier<SoundEvent> DRONE_AMBIENT = register("entity.surveyor_drone.ambient");
	public static final RegistrySupplier<SoundEvent> DRONE_ALERT = register("entity.surveyor_drone.alert");
	public static final RegistrySupplier<SoundEvent> DRONE_SCAN_BEEP = register("entity.surveyor_drone.scan_beep");
	public static final RegistrySupplier<SoundEvent> DRONE_INTERFERENCE = register("entity.surveyor_drone.interference");
	public static final RegistrySupplier<SoundEvent> DRONE_HURT = register("entity.surveyor_drone.hurt");
	public static final RegistrySupplier<SoundEvent> DRONE_DEATH = register("entity.surveyor_drone.death");
	public static final RegistrySupplier<SoundEvent> ATTACK_DRONE_FUSE = register("entity.attack_drone.fuse");
	public static final RegistrySupplier<SoundEvent> DROP_POD_DESCENT = register("event.drop_pod.descent");
	public static final RegistrySupplier<SoundEvent> DROP_POD_IMPACT = register("event.drop_pod.impact");
	public static final RegistrySupplier<SoundEvent> ORBITAL_RESUPPLY = register("event.orbital_resupply");
	public static final RegistrySupplier<SoundEvent> GUN_FIRE = register("unit.gun_fire");
	public static final RegistrySupplier<SoundEvent> SNIPER_FIRE = register("unit.sniper_fire");
	public static final RegistrySupplier<SoundEvent> MORTAR_FIRE = register("unit.mortar_fire");
	public static final RegistrySupplier<SoundEvent> MORTAR_WHISTLE = register("unit.mortar_whistle");
	public static final RegistrySupplier<SoundEvent> MORTAR_IMPACT = register("unit.mortar_impact");
	public static final RegistrySupplier<SoundEvent> ASSEMBLER_WORKING = register("block.assembler.working");
	public static final RegistrySupplier<SoundEvent> ASSEMBLER_COMPLETE = register("block.assembler.complete");
	public static final RegistrySupplier<SoundEvent> CATAPULT_LAUNCH = register("block.catapult.launch");

	private static RegistrySupplier<SoundEvent> register(String name) {
		Identifier id = Program7.id(name);
		return SOUNDS.register(name, () -> SoundEvent.of(id));
	}

	public static void register() {
		SOUNDS.register();
	}

	private P7Sounds() {
	}
}
