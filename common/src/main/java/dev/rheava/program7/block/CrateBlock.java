package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

import dev.rheava.program7.registry.P7BlockEntities;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Shipping crate: the drone's general-cargo container for non-modded loot
 * (raw ore, blocks it clears off the map). A short, rectangular steel-framed
 * metal container with a small cyan status light. Stacks 1-4 like the rest of
 * the {@link CargoBlock cargo} family; low blast power since it's crates of
 * rock, not ordnance.
 *
 * <p>Unlike the rest of the cargo family it's backed by a {@link
 * CrateBlockEntity}: a delivered crate holds real cargo, spends a few seconds
 * unpacking, then folds its contents into an adjacent {@link StorageDeckBlock
 * storage deck} (or spills them on the ground) and clears itself. This is a
 * plain {@link BlockEntityProvider} rather than a {@code BlockWithEntity} so
 * it can keep {@link CargoBlock}'s sea-pickle stacking and cook-off behaviour.
 */
public class CrateBlock extends CargoBlock implements BlockEntityProvider {
	public static final MapCodec<CrateBlock> CODEC = createCodec(CrateBlock::new);

	public CrateBlock(Settings settings) {
		super(settings, 5, 1.0f);
	}

	@Override
	protected MapCodec<CrateBlock> getCodec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new CrateBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
			BlockEntityType<T> type) {
		if (world.isClient || type != P7BlockEntities.CRATE.get()) {
			return null;
		}
		return (tickWorld, pos, tickState, blockEntity) -> {
			if (blockEntity instanceof CrateBlockEntity crate) {
				CrateBlockEntity.serverTick(tickWorld, pos, tickState, crate);
			}
		};
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock())
				&& world.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
			// Broken (or cooked off) mid-unpack: spill whatever cargo it was
			// still holding, matching StorageDeckBlock's drop-on-break.
			ItemScatterer.spawn(world, pos, crate.getCargo());
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
