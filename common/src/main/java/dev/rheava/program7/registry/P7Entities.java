package dev.rheava.program7.registry;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AttackDroneEntity;
import dev.rheava.program7.entity.DropPodEntity;
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

	public static void register() {
		ENTITIES.register();

		EntityAttributeRegistry.register(SURVEYOR_DRONE, SurveyorDroneEntity::createSurveyorDroneAttributes);
		EntityAttributeRegistry.register(ATTACK_DRONE, AttackDroneEntity::createAttackDroneAttributes);
		EntityAttributeRegistry.register(HARVESTER_DRONE, HarvesterDroneEntity::createHarvesterDroneAttributes);
	}

	private P7Entities() {
	}
}
