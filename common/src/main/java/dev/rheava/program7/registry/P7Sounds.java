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

	// Sounds carrying the "distant menace" get wide FIXED audible ranges (in
	// blocks) via registerRanged(...) so the player hears the Program working
	// from far off — ambient loops, alarms, gunfire and (loudest) artillery.
	// Close-quarters cues (scan beep, hurt, whir, fuse, assembler) stay on the
	// default variable range via register(...).
	public static final RegistrySupplier<SoundEvent> DRONE_AMBIENT = registerRanged("entity.surveyor_drone.ambient", 48f);
	public static final RegistrySupplier<SoundEvent> DRONE_ALERT = registerRanged("entity.surveyor_drone.alert", 64f);
	public static final RegistrySupplier<SoundEvent> DRONE_SCAN_BEEP = register("entity.surveyor_drone.scan_beep");
	public static final RegistrySupplier<SoundEvent> DRONE_INTERFERENCE = registerRanged("entity.surveyor_drone.interference", 64f);
	public static final RegistrySupplier<SoundEvent> DRONE_HURT = register("entity.surveyor_drone.hurt");
	public static final RegistrySupplier<SoundEvent> DRONE_DEATH = registerRanged("entity.surveyor_drone.death", 48f);
	/** A wreck slamming into the ground after the unit dies mid-air — see #7. */
	public static final RegistrySupplier<SoundEvent> DRONE_IMPACT = registerRanged("entity.surveyor_drone.impact", 64f);
	/** Rising engine note as a drone closes on its target — see #4. */
	public static final RegistrySupplier<SoundEvent> DRONE_WHIR = register("entity.surveyor_drone.whir");
	public static final RegistrySupplier<SoundEvent> ATTACK_DRONE_FUSE = register("entity.attack_drone.fuse");
	public static final RegistrySupplier<SoundEvent> DROP_POD_DESCENT = registerRanged("event.drop_pod.descent", 128f);
	public static final RegistrySupplier<SoundEvent> DROP_POD_IMPACT = registerRanged("event.drop_pod.impact", 160f);
	public static final RegistrySupplier<SoundEvent> ORBITAL_RESUPPLY = registerRanged("event.orbital_resupply", 128f);
	/** Loud psychic-sting stinger the instant a scan completes — see horror-beat pass. */
	public static final RegistrySupplier<SoundEvent> SCAN_STING = registerRanged("event.scan_sting", 96f);
	/** Ominous swell right after a scan sting: drones are now inbound on the mark. */
	public static final RegistrySupplier<SoundEvent> DRONES_INBOUND = registerRanged("event.drones_inbound", 96f);
	public static final RegistrySupplier<SoundEvent> GUN_FIRE = registerRanged("unit.gun_fire", 128f);
	public static final RegistrySupplier<SoundEvent> SNIPER_FIRE = registerRanged("unit.sniper_fire", 160f);
	/** Near-miss air-crack played at the target's position on a graze — see #1. */
	public static final RegistrySupplier<SoundEvent> GUN_WHISTLE = registerRanged("unit.gun_whistle", 64f);
	public static final RegistrySupplier<SoundEvent> MORTAR_FIRE = registerRanged("unit.mortar_fire", 192f);
	public static final RegistrySupplier<SoundEvent> MORTAR_WHISTLE = registerRanged("unit.mortar_whistle", 128f);
	public static final RegistrySupplier<SoundEvent> MORTAR_IMPACT = registerRanged("unit.mortar_impact", 192f);
	/** Fixed emplacements' contact klaxon, latched once per approach — see #8. */
	public static final RegistrySupplier<SoundEvent> UNIT_ALARM = registerRanged("unit.alarm", 64f);
	/** Soft "nothing happened" click when a magazine-fed weapon fires dry — see the reload/magazine pass. */
	public static final RegistrySupplier<SoundEvent> WEAPON_DRY_FIRE = register("unit.weapon_dry_fire");
	public static final RegistrySupplier<SoundEvent> ASSEMBLER_WORKING = register("block.assembler.working");
	public static final RegistrySupplier<SoundEvent> ASSEMBLER_COMPLETE = register("block.assembler.complete");
	public static final RegistrySupplier<SoundEvent> CATAPULT_LAUNCH = registerRanged("block.catapult.launch", 96f);
	/** Rhythmic heavy rotor thump loop for helicopter entities. */
	public static final RegistrySupplier<SoundEvent> HELI_ROTOR_LOOP = registerRanged("entity.recon_helicopter.rotor_loop", 112f);
	/** Droning engine loop for fixed-wing aircraft entities. */
	public static final RegistrySupplier<SoundEvent> PLANE_ENGINE_LOOP = registerRanged("entity.air_uav.engine_loop", 96f);
	/** Grinding heavy track loop for ground vehicle entities. */
	public static final RegistrySupplier<SoundEvent> TANK_TRACKS_LOOP = registerRanged("unit.tank_tracks_loop", 64f);
	/** Deep throbbing engine loop for aquatic vehicle entities. */
	public static final RegistrySupplier<SoundEvent> BOAT_ENGINE_LOOP = registerRanged("entity.gunboat.engine_loop", 64f);
	/** Standoff mining-laser hum — see MineResourceGoal. */
	public static final RegistrySupplier<SoundEvent> MINING_LASER = registerRanged("entity.mining_drone.laser", 48f);
	/** Player throws a glow stick — close-quarters cue, default range. */
	public static final RegistrySupplier<SoundEvent> GLOW_STICK_THROW = register("item.glow_stick.throw");
	/** A thrown glow stick sticking to a surface. */
	public static final RegistrySupplier<SoundEvent> GLOW_STICK_STICK = register("block.glow_stick.stick");
	/** A placed glow stick burning all the way out and going dark. */
	public static final RegistrySupplier<SoundEvent> GLOW_STICK_FADE = register("block.glow_stick.fade");

	/** Charge laser spin-up whine on the trigger pull, before the beam actually starts. */
	public static final RegistrySupplier<SoundEvent> LASER_SPINUP = register("item.charge_laser.spinup");
	/** Periodic crackle while the beam is actively firing. */
	public static final RegistrySupplier<SoundEvent> LASER_BEAM_LOOP = registerRanged("item.charge_laser.beam_loop", 32f);
	/** Overheat hiss/klaxon — the beam just locked out until it vents. */
	public static final RegistrySupplier<SoundEvent> LASER_OVERHEAT = register("item.charge_laser.overheat");
	/** Sneak + right-click battery reload clunk. */
	public static final RegistrySupplier<SoundEvent> LASER_RELOAD = register("item.charge_laser.reload");
	/** Chime played once the amethyst lens crosses into "worn" territory. */
	public static final RegistrySupplier<SoundEvent> LASER_LENS_WARN = register("item.charge_laser.lens_warn");

	/** Ender-pearl blink gun's emitter discharge cue — plays whether the shot lands a blink or just shoves a target. */
	public static final RegistrySupplier<SoundEvent> BLINK_GUN_CHARGE = register("item.ender_pearl_blink_gun.charge");
	/** The teleport crack itself — played at both the origin and destination of a successful blink. */
	public static final RegistrySupplier<SoundEvent> BLINK_GUN_WARP = register("item.ender_pearl_blink_gun.warp");
	/** Salvaged rifle shot report — the loudest player-weapon cue in the arsenal so far; guns attract the network. */
	public static final RegistrySupplier<SoundEvent> RIFLE_SHOT = registerRanged("item.salvaged_rifle.shot", 128f);
	/** Sneak + right-click magazine reload clunk. */
	public static final RegistrySupplier<SoundEvent> RIFLE_RELOAD = register("item.salvaged_rifle.reload");

	private static RegistrySupplier<SoundEvent> register(String name) {
		Identifier id = Program7.id(name);
		return SOUNDS.register(name, () -> SoundEvent.of(id));
	}

	/** Registers a sound with a wide FIXED audible range (in blocks) — the "hear it from far off" cues. */
	private static RegistrySupplier<SoundEvent> registerRanged(String name, float range) {
		Identifier id = Program7.id(name);
		return SOUNDS.register(name, () -> SoundEvent.of(id, range));
	}

	public static void register() {
		SOUNDS.register();
	}

	private P7Sounds() {
	}
}
