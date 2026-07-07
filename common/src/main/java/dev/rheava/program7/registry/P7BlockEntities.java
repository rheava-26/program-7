package dev.rheava.program7.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import dev.rheava.program7.block.DroneWreckBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.RegistryKeys;

public final class P7BlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(Program7.MOD_ID, RegistryKeys.BLOCK_ENTITY_TYPE);

	public static final RegistrySupplier<BlockEntityType<DroneWreckBlockEntity>> DRONE_WRECK =
			BLOCK_ENTITIES.register("drone_wreck",
					() -> BlockEntityType.Builder.create(DroneWreckBlockEntity::new,
							P7Blocks.DRONE_WRECK.get()).build(null));

	public static void register() {
		BLOCK_ENTITIES.register();
	}

	private P7BlockEntities() {
	}
}
