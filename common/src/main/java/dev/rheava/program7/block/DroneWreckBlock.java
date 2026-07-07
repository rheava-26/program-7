package dev.rheava.program7.block;

import java.util.List;

import com.mojang.serialization.MapCodec;

import dev.rheava.program7.registry.P7Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * What's left after a drone crashes: a low pile of twisted plating holding
 * the unit's salvage. Right-click to pull parts out (the wreck collapses
 * when emptied), or break it to spill everything at once.
 */
public class DroneWreckBlock extends BlockWithEntity {
	public static final MapCodec<DroneWreckBlock> CODEC = createCodec(DroneWreckBlock::new);
	private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 6.0, 15.0);

	public DroneWreckBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<DroneWreckBlock> getCodec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new DroneWreckBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
			BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (world.getBlockEntity(pos) instanceof DroneWreckBlockEntity wreck) {
			ItemStack salvage = wreck.takeNextStack();
			if (!salvage.isEmpty()) {
				player.getInventory().offerOrDrop(salvage);
				world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS,
						0.7f, 0.8f + world.random.nextFloat() * 0.4f);
			}
			if (wreck.isEmpty()) {
				world.breakBlock(pos, false);
			}
		}
		return ActionResult.CONSUME;
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock())
				&& world.getBlockEntity(pos) instanceof DroneWreckBlockEntity wreck) {
			ItemScatterer.spawn(world, pos, wreck.getItems());
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	/**
	 * Drop a wreck at (or near) the crash site and load it with salvage.
	 * Falls back to scattering items if there's genuinely no room.
	 */
	public static void placeWreck(ServerWorld world, BlockPos crashPos, List<ItemStack> salvage) {
		BlockPos target = findSpot(world, crashPos);
		if (target == null) {
			for (ItemStack stack : salvage) {
				world.spawnEntity(new ItemEntity(world,
						crashPos.getX() + 0.5, crashPos.getY() + 0.5, crashPos.getZ() + 0.5, stack));
			}
			return;
		}
		world.setBlockState(target, P7Blocks.DRONE_WRECK.get().getDefaultState());
		if (world.getBlockEntity(target) instanceof DroneWreckBlockEntity wreck) {
			wreck.fill(salvage);
		}
		world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
				target.getX() + 0.5, target.getY() + 0.6, target.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.01);
		world.spawnParticles(ParticleTypes.LAVA,
				target.getX() + 0.5, target.getY() + 0.4, target.getZ() + 0.5, 3, 0.2, 0.1, 0.2, 0.0);
	}

	@Nullable
	private static BlockPos findSpot(ServerWorld world, BlockPos pos) {
		for (BlockPos candidate : new BlockPos[] {pos, pos.down(), pos.up(), pos.down(2)}) {
			if (world.getBlockState(candidate).isReplaceable()
					&& !world.getBlockState(candidate.down()).isReplaceable()) {
				return candidate;
			}
		}
		if (world.getBlockState(pos).isReplaceable()) {
			return pos;
		}
		return null;
	}
}
