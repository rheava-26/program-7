package dev.rheava.program7.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import dev.rheava.program7.block.AssemblerBlockEntity;
import dev.rheava.program7.block.AutogunTurretBlockEntity;
import dev.rheava.program7.block.CrateBlockEntity;
import dev.rheava.program7.block.DroneWreckBlockEntity;
import dev.rheava.program7.block.FuelPlantBlockEntity;
import dev.rheava.program7.block.LaunchCatapultBlockEntity;
import dev.rheava.program7.block.ProbeCoreBlockEntity;
import dev.rheava.program7.block.PsionicResearchBlockEntity;
import dev.rheava.program7.block.StorageDeckBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.RegistryKeys;

public final class P7BlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(Program7.MOD_ID, RegistryKeys.BLOCK_ENTITY_TYPE);

	public static final RegistrySupplier<BlockEntityType<DroneWreckBlockEntity>> DRONE_WRECK =
			BLOCK_ENTITIES.register("drone_wreck",
					() -> BlockEntityType.Builder.create(DroneWreckBlockEntity::new,
							P7Blocks.DRONE_WRECK.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<AssemblerBlockEntity>> ASSEMBLER =
			BLOCK_ENTITIES.register("assembler",
					() -> BlockEntityType.Builder.create(AssemblerBlockEntity::new,
							P7Blocks.ASSEMBLER.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<AutogunTurretBlockEntity>> AUTOGUN_TURRET =
			BLOCK_ENTITIES.register("autogun_turret",
					() -> BlockEntityType.Builder.create(AutogunTurretBlockEntity::new,
							P7Blocks.AUTOGUN_TURRET.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<LaunchCatapultBlockEntity>> LAUNCH_CATAPULT =
			BLOCK_ENTITIES.register("launch_catapult",
					() -> BlockEntityType.Builder.create(LaunchCatapultBlockEntity::new,
							P7Blocks.LAUNCH_CATAPULT.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<ProbeCoreBlockEntity>> PROBE_CORE =
			BLOCK_ENTITIES.register("probe_core",
					() -> BlockEntityType.Builder.create(ProbeCoreBlockEntity::new,
							P7Blocks.PROBE_CORE.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<StorageDeckBlockEntity>> STORAGE_DECK =
			BLOCK_ENTITIES.register("storage_deck",
					() -> BlockEntityType.Builder.create(StorageDeckBlockEntity::new,
							P7Blocks.STORAGE_DECK.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<CrateBlockEntity>> CRATE =
			BLOCK_ENTITIES.register("crate",
					() -> BlockEntityType.Builder.create(CrateBlockEntity::new,
							P7Blocks.CRATE.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<FuelPlantBlockEntity>> FUEL_PLANT =
			BLOCK_ENTITIES.register("fuel_plant",
					() -> BlockEntityType.Builder.create(FuelPlantBlockEntity::new,
							P7Blocks.FUEL_PLANT.get()).build(null));

	public static final RegistrySupplier<BlockEntityType<PsionicResearchBlockEntity>> PSIONIC_RESEARCH =
			BLOCK_ENTITIES.register("psionic_research",
					() -> BlockEntityType.Builder.create(PsionicResearchBlockEntity::new,
							P7Blocks.PSIONIC_RESEARCH.get()).build(null));

	public static void register() {
		BLOCK_ENTITIES.register();
	}

	private P7BlockEntities() {
	}
}
