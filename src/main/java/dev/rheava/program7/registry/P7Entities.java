package dev.rheava.program7.registry;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.AttackDroneEntity;
import dev.rheava.program7.entity.DropPodEntity;
import dev.rheava.program7.entity.HarvesterDroneEntity;
import dev.rheava.program7.entity.SurveyorDroneEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class P7Entities {
	/**
	 * The Program's forward reconnaissance unit: approaches, scans, files a
	 * threat profile on you, then withdraws. The intel it gathers decides
	 * what the Program sends after you next.
	 */
	public static final EntityType<SurveyorDroneEntity> SURVEYOR_DRONE = Registry.register(
			Registries.ENTITY_TYPE,
			Program7.id("surveyor_drone"),
			EntityType.Builder.create(SurveyorDroneEntity::new, SpawnGroup.MISC)
					.dimensions(0.9f, 0.7f)
					.maxTrackingRange(10)
					.build());

	/** Tier 1 response unit: fast, expendable, explodes on contact. */
	public static final EntityType<AttackDroneEntity> ATTACK_DRONE = Registry.register(
			Registries.ENTITY_TYPE,
			Program7.id("attack_drone"),
			EntityType.Builder.create(AttackDroneEntity::new, SpawnGroup.MISC)
					.dimensions(0.9f, 0.7f)
					.maxTrackingRange(10)
					.build());

	/** Wheeled ground hauler: mines what the Program needs, refills the ledger. */
	public static final EntityType<HarvesterDroneEntity> HARVESTER_DRONE = Registry.register(
			Registries.ENTITY_TYPE,
			Program7.id("harvester_drone"),
			EntityType.Builder.create(HarvesterDroneEntity::new, SpawnGroup.MISC)
					.dimensions(1.0f, 0.9f)
					.maxTrackingRange(10)
					.build());

	/** The pod mid-descent; long tracking range so the fireball reads from afar. */
	public static final EntityType<DropPodEntity> DROP_POD = Registry.register(
			Registries.ENTITY_TYPE,
			Program7.id("drop_pod"),
			EntityType.Builder.<DropPodEntity>create(DropPodEntity::new, SpawnGroup.MISC)
					.dimensions(1.0f, 1.6f)
					.maxTrackingRange(32)
					.build());

	public static void register() {
		FabricDefaultAttributeRegistry.register(SURVEYOR_DRONE, SurveyorDroneEntity.createSurveyorDroneAttributes());
		FabricDefaultAttributeRegistry.register(ATTACK_DRONE, AttackDroneEntity.createAttackDroneAttributes());
		FabricDefaultAttributeRegistry.register(HARVESTER_DRONE, HarvesterDroneEntity.createHarvesterDroneAttributes());
	}

	private P7Entities() {
	}
}
