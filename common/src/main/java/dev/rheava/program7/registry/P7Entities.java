package dev.rheava.program7.registry;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AntiAirTurretEntity;
import dev.rheava.program7.entity.AttackDroneEntity;
import dev.rheava.program7.entity.AutogunTurretEntity;
import dev.rheava.program7.entity.DropPodEntity;
import dev.rheava.program7.entity.GroundDroneEntity;
import dev.rheava.program7.entity.HarvesterDroneEntity;
import dev.rheava.program7.entity.LogisticsDroneEntity;
import dev.rheava.program7.entity.MediumAttackDroneEntity;
import dev.rheava.program7.entity.MediumMiningDroneEntity;
import dev.rheava.program7.entity.MortarEmplacementEntity;
import dev.rheava.program7.entity.MortarShellEntity;
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
							.build());

	/** Tier 1 response unit: fast, expendable, explodes on contact. */
	public static final RegistrySupplier<EntityType<AttackDroneEntity>> ATTACK_DRONE =
			ENTITIES.register("attack_drone",
					() -> EntityType.Builder.create(AttackDroneEntity::new, SpawnGroup.MISC)
							.dimensions(1.0f, 0.55f)
							.maxTrackingRange(10)
							.build());

	/** Wheeled ground hauler: mines what the Program needs, refills the ledger. */
	public static final RegistrySupplier<EntityType<HarvesterDroneEntity>> HARVESTER_DRONE =
			ENTITIES.register("harvester_drone",
					() -> EntityType.Builder.create(HarvesterDroneEntity::new, SpawnGroup.MISC)
							.dimensions(1.0f, 0.9f)
							.maxTrackingRange(10)
							.build());

	/** The pod mid-descent; long tracking range so the fireball reads from afar. */
	public static final RegistrySupplier<EntityType<DropPodEntity>> DROP_POD =
			ENTITIES.register("drop_pod",
					() -> EntityType.Builder.<DropPodEntity>create(DropPodEntity::new, SpawnGroup.MISC)
							.dimensions(1.0f, 1.6f)
							.maxTrackingRange(32)
							.build());

	/** Tier 1 perimeter unit: a knee-high armored car with a small turreted gun. */
	public static final RegistrySupplier<EntityType<GroundDroneEntity>> GROUND_DRONE =
			ENTITIES.register("ground_drone",
					() -> EntityType.Builder.create(GroundDroneEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 0.7f)
							.maxTrackingRange(10)
							.build());

	/** Tier 1 fixed defense: an open-frame autogun on a splayed-leg mount. */
	public static final RegistrySupplier<EntityType<AutogunTurretEntity>> AUTOGUN_TURRET =
			ENTITIES.register("autogun_turret",
					() -> EntityType.Builder.create(AutogunTurretEntity::new, SpawnGroup.MISC)
							.dimensions(0.8f, 1.35f)
							.maxTrackingRange(10)
							.build());

	/** Flying courier hauling the Program's build payments — shoot it down, steal the cargo. */
	public static final RegistrySupplier<EntityType<LogisticsDroneEntity>> LOGISTICS_DRONE =
			ENTITIES.register("logistics_drone",
					() -> EntityType.Builder.create(LogisticsDroneEntity::new, SpawnGroup.MISC)
							.dimensions(0.7f, 0.6f)
							.maxTrackingRange(10)
							.build());

	/** Wheeled courier: slower and tougher than its flying sibling, same cargo rules. */
	public static final RegistrySupplier<EntityType<WheeledHaulerEntity>> WHEELED_HAULER =
			ENTITIES.register("wheeled_hauler",
					() -> EntityType.Builder.create(WheeledHaulerEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 0.8f)
							.maxTrackingRange(10)
							.build());

	/** Tier 2 workhorse gun flyer: strafing hitscan fire. */
	public static final RegistrySupplier<EntityType<MediumAttackDroneEntity>> MEDIUM_ATTACK_DRONE =
			ENTITIES.register("medium_attack_drone",
					() -> EntityType.Builder.create(MediumAttackDroneEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 0.6f)
							.maxTrackingRange(10)
							.build());

	/** Tier 2 glass cannon: long-range standoff platform, dies to a stiff breeze. */
	public static final RegistrySupplier<EntityType<SniperDroneEntity>> SNIPER_DRONE =
			ENTITIES.register("sniper_drone",
					() -> EntityType.Builder.create(SniperDroneEntity::new, SpawnGroup.MISC)
							.dimensions(0.6f, 0.6f)
							.maxTrackingRange(16)
							.build());

	/** Tier 2 fixed indirect fire: shells whistle before they land. */
	public static final RegistrySupplier<EntityType<MortarEmplacementEntity>> MORTAR_EMPLACEMENT =
			ENTITIES.register("mortar_emplacement",
					() -> EntityType.Builder.create(MortarEmplacementEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 1.0f)
							.maxTrackingRange(10)
							.build());

	/** The mortar's arcing shell. */
	public static final RegistrySupplier<EntityType<MortarShellEntity>> MORTAR_SHELL =
			ENTITIES.register("mortar_shell",
					() -> EntityType.Builder.<MortarShellEntity>create(MortarShellEntity::new, SpawnGroup.MISC)
							.dimensions(0.25f, 0.25f)
							.maxTrackingRange(32)
							.build());

	/** Tier 2 flying miner: the harvester's job, airborne, faster, bigger hopper. */
	public static final RegistrySupplier<EntityType<MediumMiningDroneEntity>> MEDIUM_MINING_DRONE =
			ENTITIES.register("medium_mining_drone",
					() -> EntityType.Builder.create(MediumMiningDroneEntity::new, SpawnGroup.MISC)
							.dimensions(1.0f, 0.8f)
							.maxTrackingRange(10)
							.build());

	/** Tier 2 heavy courier: two crates, sent when the payment is too big for the light couriers. */
	public static final RegistrySupplier<EntityType<TransportDroneEntity>> TRANSPORT_DRONE =
			ENTITIES.register("transport_drone",
					() -> EntityType.Builder.create(TransportDroneEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 0.7f)
							.maxTrackingRange(10)
							.build());

	/** Tier 2 fixed anti-air: open-frame flak mount, blind against anything on the ground. */
	public static final RegistrySupplier<EntityType<AntiAirTurretEntity>> ANTI_AIR_TURRET =
			ENTITIES.register("anti_air_turret",
					() -> EntityType.Builder.create(AntiAirTurretEntity::new, SpawnGroup.MISC)
							.dimensions(0.8f, 1.5f)
							.maxTrackingRange(10)
							.build());

	/** Tier 2 unarmed scout: fast, fragile, and paints you for everything nearby. */
	public static final RegistrySupplier<EntityType<ScoutCarEntity>> SCOUT_CAR =
			ENTITIES.register("scout_car",
					() -> EntityType.Builder.create(ScoutCarEntity::new, SpawnGroup.MISC)
							.dimensions(0.9f, 0.6f)
							.maxTrackingRange(10)
							.build());

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
	}

	private P7Entities() {
	}
}
