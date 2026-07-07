package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

import dev.rheava.program7.registry.P7BlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The first piece of support infrastructure, per the unit bible: support
 * infrastructure gates unit classes — no catapult, no UAVs. It watches its
 * own footprint for an active spotter and, when the Director's ledger can
 * pay, slings a fresh one skyward off its facing. Break the catapult and the
 * base goes blind upstairs; nothing replaces a downed Air UAV without one.
 */
public class LaunchCatapultBlock extends BlockWithEntity {
	public static final MapCodec<LaunchCatapultBlock> CODEC = createCodec(LaunchCatapultBlock::new);
	/** Which way the sling fires; wired in the same way vanilla HorizontalFacingBlock does. */
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

	public LaunchCatapultBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<LaunchCatapultBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		// Fires the way the player was looking when they set it down.
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new LaunchCatapultBlockEntity(pos, state);
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
				: validateTicker(type, P7BlockEntities.LAUNCH_CATAPULT.get(), LaunchCatapultBlockEntity::serverTick);
	}
}
