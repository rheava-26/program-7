package dev.rheava.program7.registry;

import dev.rheava.program7.Program7;
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

	public static void register() {
		FabricDefaultAttributeRegistry.register(SURVEYOR_DRONE, SurveyorDroneEntity.createSurveyorDroneAttributes());
	}

	private P7Entities() {
	}
}
