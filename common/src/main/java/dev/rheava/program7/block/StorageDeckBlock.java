package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Program's physical stockpile: a lootable crate-bank planted in every
 * base alongside the probe core. Right-click to pull stores out one stack at
 * a time (the deck breaks itself when emptied), or break it outright to
 * spill everything at once. Unlike the probe core, it's inert — the
 * Program's own drones skip {@code program7}-namespace block entities and
 * won't loot it back out.
 */
public class StorageDeckBlock extends BlockWithEntity {
	public static final MapCodec<StorageDeckBlock> CODEC = createCodec(StorageDeckBlock::new);

	public StorageDeckBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<StorageDeckBlock> getCodec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new StorageDeckBlockEntity(pos, state);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
			BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (world.getBlockEntity(pos) instanceof StorageDeckBlockEntity storageDeck) {
			ItemStack salvage = storageDeck.takeNextStack();
			if (!salvage.isEmpty()) {
				player.getInventory().offerOrDrop(salvage);
				world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS,
						0.7f, 0.8f + world.random.nextFloat() * 0.4f);
			}
			if (storageDeck.isEmpty()) {
				world.breakBlock(pos, false);
			}
		}
		return ActionResult.CONSUME;
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock())
				&& world.getBlockEntity(pos) instanceof StorageDeckBlockEntity storageDeck) {
			ItemScatterer.spawn(world, pos, storageDeck.getItems());
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	/**
	 * Plant a storage deck a short distance from the probe core and seed it with
	 * the base's physical stores. Mirrors ProgramDirectorState.placeAssembler's
	 * terrain scan. Falls back to due east of the core if the terrain doesn't cooperate.
	 */
	public static void plant(ServerWorld world, BlockPos core, java.util.List<ItemStack> contents) {
		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos base = core.offset(direction, 4);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos candidate = base.up(dy);
				if (world.getBlockState(candidate).isReplaceable()
						&& world.getBlockState(candidate.down()).isSolidBlock(world, candidate.down())) {
					place(world, candidate, contents);
					return;
				}
			}
		}
		place(world, core.east(4), contents);
	}

	private static void place(ServerWorld world, BlockPos pos, java.util.List<ItemStack> contents) {
		world.setBlockState(pos, dev.rheava.program7.registry.P7Blocks.STORAGE_DECK.get().getDefaultState());
		if (world.getBlockEntity(pos) instanceof StorageDeckBlockEntity deck) {
			deck.fill(contents);
		}
	}
}
