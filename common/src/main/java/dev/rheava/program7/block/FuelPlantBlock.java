package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.director.SupplyNetwork;
import dev.rheava.program7.registry.P7BlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Program's one depot block in this slice: mirrors the {@link
 * AssemblerBlock}/{@link AssemblerBlockEntity} split exactly (a {@code
 * BlockWithEntity} shell; behavior lives in the block entity and, above
 * that, in the {@link SupplyNetwork} manager). Converts ledger coal into
 * FUEL for the Tier 2/3 fliers within its bubble — see {@code
 * SUPPLY_LINES_SPEC.md} §3 for the full contract.
 *
 * <p>Breaking it deregisters its {@link SupplyNetwork.Depot} and spills a
 * little coal salvage back out — killing infrastructure always pays a
 * little, like shooting down a courier, never just denial.
 */
public class FuelPlantBlock extends BlockWithEntity {
	public static final MapCodec<FuelPlantBlock> CODEC = createCodec(FuelPlantBlock::new);
	/** Coal spilled for every this-many stock the depot was still holding when broken. */
	private static final int SPILL_STOCK_PER_COAL = 16;
	/** Never spill more than this much coal, however full the depot was. */
	private static final int SPILL_CAP = 15;

	public FuelPlantBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<FuelPlantBlock> getCodec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new FuelPlantBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
			BlockEntityType<T> type) {
		return world.isClient ? null
				: validateTicker(type, P7BlockEntities.FUEL_PLANT.get(), FuelPlantBlockEntity::serverTick);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
			SupplyNetwork net = ProgramDirectorState.get(serverWorld).getSupplyNetwork();
			SupplyNetwork.Depot depot = net.depotAt(pos);
			int stock = depot != null ? depot.stock : 0;
			net.removeDepot(pos);

			int spill = Math.min(SPILL_CAP, stock / SPILL_STOCK_PER_COAL);
			if (spill > 0) {
				ItemScatterer.spawn(serverWorld, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
						new ItemStack(Items.COAL, spill));
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
