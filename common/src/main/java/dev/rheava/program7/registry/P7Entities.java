package dev.rheava.program7.registry;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AirUAVEntity;
import dev.rheava.program7.entity.AntiAirTurretEntity;
import dev.rheava.program7.entity.AttackDroneEntity;
import dev.rheava.program7.entity.AutogunTurretEntity;
import dev.rheava.program7.entity.BatteryCenterEntity;
import dev.rheava.program7.entity.DropPodEntity;
import dev.rheava.program7.entity.GroundDroneEntity;
import dev.rheava.program7.entity.GunboatEntity;
import dev.rheava.program7.entity.GunshipEntity;
import dev.rheava.program7.entity.HowitzerEntity;
import dev.rheava.program7.entity.HowitzerShellEntity;
import dev.rheava.program7.entity.HarvesterDroneEntity;
import dev.rheava.program7.entity.HeavyAttackDroneEntity;
import dev.rheava.program7.entity.IFVEntity;
import dev.rheava.program7.entity.LogisticsDroneEntity;
import dev.rheava.program7.entity.MediumAttackDroneEntity;
import dev.rheava.program7.entity.MediumMiningDroneEntity;
import dev.rheava.program7.entity.MlrsLauncherEntity;
import dev.rheava.program7.entity.MlrsRocketEntity;
import dev.rheava.program7.entity.MortarEmplacementEntity;
import dev.rheava.program7.entity.MortarShellEntity;
import dev.rheava.program7.entity.ReconHelicopterEntity;
import dev.rheava.program7.entity.ScoutCarEntity;
import dev.rheava.program7.entity.SniperDroneEntity;
import dev.rheava.program7.entity.SurveyorDroneEntity;
import dev.rheava.program7.entity.TransportDroneEntity;
import dev.rheava.program7.entity.WheeledHaulerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKeys;

public final class P7Entities {
	public static final DeferredRegister<EntityType<?>> ENTITIES =
			DeferredRegister.create(Program7.MOD_ID, RegistryKeys.ENTITY_TYPE);

	/**
	 * The Program's forward reconnaissance unit: approaches, scans, files a
	 * threat profile on you, then withdraws. The intel it gathers decides
	 * what the Program sends after you next.
	 */
	public static final RegistrySupplier<EntityType<SurveyorDroneEntity>> SURVEYOR_DRONE =
			ENTITIES.register("surveyor_drone",
					() -> EntityType.Builder.create(SurveyorDroneEntity::new, SpawnGroup.MISC)
							.dimensions(1.0f, 0.55f)
							.maxTrackingRange(10)
							.build("surveyor_drone"));

	/** Tier 1 response unit: fast, expendable, explodes on contact. */
	public static final RegistrySupplier<EntityType<AttackDroneEntity>> ATTACK_DRONE =
			ENTITIES.register("attack_drone",
					() -> EntityType.Builder.create(AttackDroneEntity::new, SpawnGroup.MISC)
							.dimensions(1.0f, 0.55f)
							.maxTrackingRange(10)
							.build("attack_drone"));

	/** Wheeled ground hauler: mines what the Program needs, refills the ledger. */
	public static final RegistrySupplier<EntityType<HarvesterDroneEntity>> HARVESTER_DRONE =
			ENTITIES.register("harvester_drone",
					() -> EntityType.Builder.create(HarvesterDroneEntity::new, SpawnGroup.MISC)
							.dimensions(1.0f, 0.9f)
							.maxTrackingRange(10)
							.build("harvester_drone"));

	/** The pod mid-descent; long tracking range so the fireball reads from afar. */
	public static final RegistrySupplier<EntityType<DropPodEntity>> DROP_POD =
			ENTITIES.register("drop_pod",
					() -> EntityType.Builder.<DropPodEntity>create(DropPodEntity::new, SpawnGroup.MISC)
							.dimensions(1.0f, 1.6f)
							.maxTrackingRange(32)
							.build("drop_pod"));

	/** Tier 1 perimeter unit: a knee-high armored car with a small turreted gun. */
	public static final RegistrySupplier<EntityType<GroundDroneEntity>> GROUND_DRONE =
			ENTITIES.register("ground_drone",
					() -> EntityType.Builder.create(GroundDroneEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 0.7f)
							.maxTrackingRange(10)
							.build("ground_drone"));

	/** Tier 1 fixed defense: an open-frame autogun on a splayed-leg mount. */
	public static final RegistrySupplier<EntityType<AutogunTurretEntity>> AUTOGUN_TURRET =
			ENTITIES.register("autogun_turret",
					() -> EntityType.Builder.create(AutogunTurretEntity::new, SpawnGroup.MISC)
							.dimensions(1.1f, 1.8f)
							.maxTrackingRange(10)
							.build("autogun_turret"));

	/** Flying courier hauling the Program's build payments — shoot it down, steal the cargo. */
	public static final RegistrySupplier<EntityType<LogisticsDroneEntity>> LOGISTICS_DRONE =
			ENTITIES.register("logistics_drone",
					() -> EntityType.Builder.create(LogisticsDroneEntity::new, SpawnGroup.MISC)
							.dimensions(0.7f, 0.6f)
							.maxTrackingRange(10)
							.build("logistics_drone"));

	/** Wheeled courier: slower and tougher than its flying sibling, same cargo rules. */
	public static final RegistrySupplier<EntityType<WheeledHaulerEntity>> WHEELED_HAULER =
			ENTITIES.register("wheeled_hauler",
					() -> EntityType.Builder.create(WheeledHaulerEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 0.8f)
							.maxTrackingRange(10)
							.build("wheeled_hauler"));

	/** Tier 2 workhorse gun flyer: strafing hitscan fire. */
	public static final RegistrySupplier<EntityType<MediumAttackDroneEntity>> MEDIUM_ATTACK_DRONE =
			ENTITIES.register("medium_attack_drone",
					() -> EntityType.Builder.create(MediumAttackDroneEntity::new, SpawnGroup.MISC)
							.dimensions(1.2f, 0.8f)
							.maxTrackingRange(10)
							.build("medium_attack_drone"));

	/** Tier 2 glass cannon: long-range standoff platform, dies to a stiff breeze. */
	public static final RegistrySupplier<EntityType<SniperDroneEntity>> SNIPER_DRONE =
			ENTITIES.register("sniper_drone",
					() -> EntityType.Builder.create(SniperDroneEntity::new, SpawnGroup.MISC)
							.dimensions(0.6f, 0.6f)
							.maxTrackingRange(16)
							.build("sniper_drone"));

	/** Tier 2 fixed indirect fire: shells whistle before they land. */
	public static final RegistrySupplier<EntityType<MortarEmplacementEntity>> MORTAR_EMPLACEMENT =
			ENTITIES.register("mortar_emplacement",
					() -> EntityType.Builder.create(MortarEmplacementEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 1.0f)
							.maxTrackingRange(10)
							.build("mortar_emplacement"));

	/** The mortar's arcing shell. */
	public static final RegistrySupplier<EntityType<MortarShellEntity>> MORTAR_SHELL =
			ENTITIES.register("mortar_shell",
					() -> EntityType.Builder.<MortarShellEntity>create(MortarShellEntity::new, SpawnGroup.MISC)
							.dimensions(0.25f, 0.25f)
							.maxTrackingRange(32)
							.build("mortar_shell"));

	/** Tier 2 flying miner: the harvester's job, airborne, faster, bigger hopper. */
	public static final RegistrySupplier<EntityType<MediumMiningDroneEntity>> MEDIUM_MINING_DRONE =
			ENTITIES.register("medium_mining_drone",
					() -> EntityType.Builder.create(MediumMiningDroneEntity::new, SpawnGroup.MISC)
							.dimensions(1.0f, 0.8f)
							.maxTrackingRange(10)
							.build("medium_mining_drone"));

	/** Tier 2 heavy courier: two crates, sent when the payment is too big for the light couriers. */
	public static final RegistrySupplier<EntityType<TransportDroneEntity>> TRANSPORT_DRONE =
			ENTITIES.register("transport_drone",
					() -> EntityType.Builder.create(TransportDroneEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 0.7f)
							.maxTrackingRange(10)
							.build("transport_drone"));

	/** Tier 2 fixed anti-air: open-frame flak mount, blind against anything on the ground. */
	public static final RegistrySupplier<EntityType<AntiAirTurretEntity>> ANTI_AIR_TURRET =
			ENTITIES.register("anti_air_turret",
					() -> EntityType.Builder.create(AntiAirTurretEntity::new, SpawnGroup.MISC)
							.dimensions(1.1f, 2.0f)
							.maxTrackingRange(10)
							.build("anti_air_turret"));

	/** Tier 2 unarmed scout: fast, fragile, and paints you for everything nearby. */
	public static final RegistrySupplier<EntityType<ScoutCarEntity>> SCOUT_CAR =
			ENTITIES.register("scout_car",
					() -> EntityType.Builder.create(ScoutCarEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 0.6f)
							.maxTrackingRange(10)
							.build("scout_car"));

	/** Fixed-wing spotter circling its base; needs a launch catapult to exist at all. */
	public static final RegistrySupplier<EntityType<AirUAVEntity>> AIR_UAV =
			ENTITIES.register("air_uav",
					() -> EntityType.Builder.create(AirUAVEntity::new, SpawnGroup.MISC)
							.dimensions(1.4f, 0.5f)
							.maxTrackingRange(16)
							.build("air_uav"));

	/** Tier 3 car-sized armored gun flyer: burst-fire machine guns, engages from far away. */
	public static final RegistrySupplier<EntityType<HeavyAttackDroneEntity>> HEAVY_ATTACK_DRONE =
			ENTITIES.register("heavy_attack_drone",
					() -> EntityType.Builder.create(HeavyAttackDroneEntity::new, SpawnGroup.MISC)
							.dimensions(3.5f, 3.0f)
							.maxTrackingRange(12)
							.build("heavy_attack_drone"));

	/** Tier 3 armored fighting vehicle: cannon plus a light-drone fireteam in the bay. */
	public static final RegistrySupplier<EntityType<IFVEntity>> IFV =
			ENTITIES.register("ifv",
					() -> EntityType.Builder.create(IFVEntity::new, SpawnGroup.MISC)
							.dimensions(3.8f, 4.0f)
							.maxTrackingRange(12)
							.build("ifv"));

	/** Tier 3 armed water presence: the toughest hull afloat, helpless if beached. */
	public static final RegistrySupplier<EntityType<GunboatEntity>> GUNBOAT =
			ENTITIES.register("gunboat",
					() -> EntityType.Builder.create(GunboatEntity::new, SpawnGroup.MISC)
							.dimensions(6.1f, 4.0f)
							.maxTrackingRange(12)
							.build("gunboat"));

	/** Tier 3 fast aerial spotter: orbits you with a searchlight pinned on. */
	public static final RegistrySupplier<EntityType<ReconHelicopterEntity>> RECON_HELICOPTER =
			ENTITIES.register("recon_helicopter",
					() -> EntityType.Builder.create(ReconHelicopterEntity::new, SpawnGroup.MISC)
							.dimensions(4.0f, 3.5f)
							.maxTrackingRange(16)
							.build("recon_helicopter"));

	/** Tier 3 rolling power bank: heals nearby units; browns them out when it dies. */
	public static final RegistrySupplier<EntityType<BatteryCenterEntity>> BATTERY_CENTER =
			ENTITIES.register("battery_center",
					() -> EntityType.Builder.create(BatteryCenterEntity::new, SpawnGroup.MISC)
							.dimensions(2.4f, 2.6f)
							.maxTrackingRange(12)
							.build("battery_center"));

	/** Tier 4 apex: house-sized twin-ducted-rotor gunship with a belly autocannon. Seen and heard from far off. */
	public static final RegistrySupplier<EntityType<GunshipEntity>> GUNSHIP =
			ENTITIES.register("gunship",
					() -> EntityType.Builder.create(GunshipEntity::new, SpawnGroup.MISC)
							.dimensions(4.6f, 3.2f)
							// In chunks; clamped to view distance at runtime. 16 (~256 blocks)
							// is the practical max for "seen from far off".
							.maxTrackingRange(16)
							.build("gunship"));

	/** Tier 3 mobile artillery: tracked self-propelled howitzer, the wall-breaker — heavy arcing shells at long range. */
	public static final RegistrySupplier<EntityType<HowitzerEntity>> HOWITZER =
			ENTITIES.register("howitzer",
					() -> EntityType.Builder.create(HowitzerEntity::new, SpawnGroup.MISC)
							.dimensions(3.9f, 3.5f)
							.maxTrackingRange(16)
							.build("howitzer"));

	/** The howitzer's heavy arcing shell. */
	public static final RegistrySupplier<EntityType<HowitzerShellEntity>> HOWITZER_SHELL =
			ENTITIES.register("howitzer_shell",
					() -> EntityType.Builder.<HowitzerShellEntity>create(HowitzerShellEntity::new, SpawnGroup.MISC)
							.dimensions(0.5f, 0.5f)
							.maxTrackingRange(48)
							.build("howitzer_shell"));

	/** Tier 3-4 mobile rocket artillery: ripples several unguided rockets rather than lobbing one precise shell. */
	public static final RegistrySupplier<EntityType<MlrsLauncherEntity>> MLRS_LAUNCHER =
			ENTITIES.register("mlrs_launcher",
					() -> EntityType.Builder.create(MlrsLauncherEntity::new, SpawnGroup.MISC)
							.dimensions(3.9f, 3.5f)
							.maxTrackingRange(16)
							.build("mlrs_launcher"));

	/** One rocket out of an MLRS ripple. */
	public static final RegistrySupplier<EntityType<MlrsRocketEntity>> MLRS_ROCKET =
			ENTITIES.register("mlrs_rocket",
					() -> EntityType.Builder.<MlrsRocketEntity>create(MlrsRocketEntity::new, SpawnGroup.MISC)
							.dimensions(0.4f, 0.4f)
							.maxTrackingRange(48)
							.build("mlrs_rocket"));

	public static void register() {
		ENTITIES.register();

		EntityAttributeRegistry.register(SURVEYOR_DRONE, SurveyorDroneEntity::createSurveyorDroneAttributes);
		EntityAttributeRegistry.register(ATTACK_DRONE, AttackDroneEntity::createAttackDroneAttributes);
		EntityAttributeRegistry.register(HARVESTER_DRONE, HarvesterDroneEntity::createHarvesterDroneAttributes);
		EntityAttributeRegistry.register(GROUND_DRONE, GroundDroneEntity::createGroundDroneAttributes);
		EntityAttributeRegistry.register(AUTOGUN_TURRET, AutogunTurretEntity::createAutogunTurretAttributes);
		EntityAttributeRegistry.register(LOGISTICS_DRONE, LogisticsDroneEntity::createLogisticsDroneAttributes);
		EntityAttributeRegistry.register(WHEELED_HAULER, WheeledHaulerEntity::createWheeledHaulerAttributes);
		EntityAttributeRegistry.register(MEDIUM_ATTACK_DRONE,
				MediumAttackDroneEntity::createMediumAttackDroneAttributes);
		EntityAttributeRegistry.register(SNIPER_DRONE, SniperDroneEntity::createSniperDroneAttributes);
		EntityAttributeRegistry.register(MORTAR_EMPLACEMENT,
				MortarEmplacementEntity::createMortarEmplacementAttributes);
		EntityAttributeRegistry.register(MEDIUM_MINING_DRONE,
				MediumMiningDroneEntity::createMediumMiningDroneAttributes);
		EntityAttributeRegistry.register(TRANSPORT_DRONE, TransportDroneEntity::createTransportDroneAttributes);
		EntityAttributeRegistry.register(ANTI_AIR_TURRET, AntiAirTurretEntity::createAntiAirTurretAttributes);
		EntityAttributeRegistry.register(SCOUT_CAR, ScoutCarEntity::createScoutCarAttributes);
		EntityAttributeRegistry.register(AIR_UAV, AirUAVEntity::createAirUAVAttributes);
		EntityAttributeRegistry.register(HEAVY_ATTACK_DRONE,
				HeavyAttackDroneEntity::createHeavyAttackDroneAttributes);
		EntityAttributeRegistry.register(IFV, IFVEntity::createIFVAttributes);
		EntityAttributeRegistry.register(GUNBOAT, GunboatEntity::createGunboatAttributes);
		EntityAttributeRegistry.register(RECON_HELICOPTER,
				ReconHelicopterEntity::createReconHelicopterAttributes);
		EntityAttributeRegistry.register(BATTERY_CENTER, BatteryCenterEntity::createBatteryCenterAttributes);
		EntityAttributeRegistry.register(GUNSHIP, GunshipEntity::createGunshipAttributes);
		EntityAttributeRegistry.register(HOWITZER, HowitzerEntity::createHowitzerAttributes);
		EntityAttributeRegistry.register(MLRS_LAUNCHER, MlrsLauncherEntity::createMlrsLauncherAttributes);
	}

	private P7Entities() {
	}
}
