package dev.rheava.program7.registry;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AttackDroneEntity;
import dev.rheava.program7.entity.AutogunTurretEntity;
import dev.rheava.program7.entity.DropPodEntity;
import dev.rheava.program7.entity.GroundDroneEntity;
import dev.rheava.program7.entity.HarvesterDroneEntity;
import dev.rheava.program7.entity.SurveyorDroneEntity;
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

	/** Tier 1 fixed defense: a twin-barrel autogun bolted onto a pedestal. */
	public static final RegistrySupplier<EntityType<AutogunTurretEntity>> AUTOGUN_TURRET =
			ENTITIES.register("autogun_turret",
					() -> EntityType.Builder.create(AutogunTurretEntity::new, SpawnGroup.MISC)
							.dimensions(0.8f, 0.8f)
							.maxTrackingRange(10)
							.build());

	public static void register() {
		ENTITIES.register();

		EntityAttributeRegistry.register(SURVEYOR_DRONE, SurveyorDroneEntity::createSurveyorDroneAttributes);
		EntityAttributeRegistry.register(ATTACK_DRONE, AttackDroneEntity::createAttackDroneAttributes);
		EntityAttributeRegistry.register(HARVESTER_DRONE, HarvesterDroneEntity::createHarvesterDroneAttributes);
		EntityAttributeRegistry.register(GROUND_DRONE, GroundDroneEntity::createGroundDroneAttributes);
		EntityAttributeRegistry.register(AUTOGUN_TURRET, AutogunTurretEntity::createAutogunTurretAttributes);
	}

	private P7Entities() {
	}
}
