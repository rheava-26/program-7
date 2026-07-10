package dev.rheava.program7.block;

import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.director.Resources;
import dev.rheava.program7.director.SupplyNetwork;
import dev.rheava.program7.registry.P7BlockEntities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Thin shell behind the {@link FuelPlantBlock}. Stock ownership lives
 * entirely on the {@link SupplyNetwork.Depot} record (persisted with the
 * Director), so this block entity carries no stock of its own — it just
 * registers the depot once (idempotent, so a reload never resets stock) and
 * plays the "still working" tell while it's actually refilling.
 */
public class FuelPlantBlockEntity extends BlockEntity {
	private static final int TELL_INTERVAL = 40;

	/** Not persisted: re-registering an existing depot is a no-op anyway, see SupplyNetwork#registerDepot. */
	private boolean depotRegistered = false;
	private int tellCooldown = TELL_INTERVAL;

	public FuelPlantBlockEntity(BlockPos pos, BlockState state) {
		super(P7BlockEntities.FUEL_PLANT.get(), pos, state);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, FuelPlantBlockEntity plant) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		plant.tick(serverWorld, pos);
	}

	private void tick(ServerWorld world, BlockPos pos) {
		ProgramDirectorState director = ProgramDirectorState.get(world);
		SupplyNetwork net = director.getSupplyNetwork();
		if (!this.depotRegistered) {
			net.registerDepot(pos, SupplyNetwork.SUPPLY_FUEL, SupplyNetwork.FUEL_PLANT_RADIUS,
					SupplyNetwork.FUEL_PLANT_CAPACITY);
			this.depotRegistered = true;
		}

		if (--this.tellCooldown > 0) {
			return;
		}
		this.tellCooldown = TELL_INTERVAL;

		SupplyNetwork.Depot depot = net.depotAt(pos);
		boolean refilling = depot != null && depot.stock < depot.capacity
				&& director.getResource(Resources.COAL) > 0;
		if (!refilling) {
			return;
		}
		world.playSound(null, pos, P7Sounds.ASSEMBLER_WORKING.get(), SoundCategory.BLOCKS,
				0.4f, 0.9f + world.random.nextFloat() * 0.2f);
		world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
				pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.01);
	}
}
