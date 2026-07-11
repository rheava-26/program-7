package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

/**
 * A small pseudo-block "ammo box" that stacks additively in the same
 * position — mirrors vanilla {@code SeaPickleBlock}'s {@code PICKLES}
 * property exactly, just renamed and re-themed: placing another ammo box
 * item against an existing ammo box grows its {@link #COUNT} (1-4) instead
 * of occupying a new position, so a mini stockpile reads as one crate pile
 * getting taller rather than four separate blocks jammed together.
 *
 * <p>This is the physical unit {@code AmmoRunGoal} consumes: a logistics
 * drone servicing a starved {@code ReloadableWeapon} decrements (or, at
 * count 1, removes) the nearest ammo box at a base's ammo stockpile — see
 * {@code director.BasePad#buildStockpile} for where these get planted and
 * {@code entity.ai.AmmoRunGoal} for the delivery loop.
 */
public class AmmoBoxBlock extends Block {
	public static final MapCodec<AmmoBoxBlock> CODEC = createCodec(AmmoBoxBlock::new);
	/** How many crates are piled in this cell, 1-4 — same range/idiom as vanilla's sea pickle {@code PICKLES}. */
	public static final IntProperty COUNT = IntProperty.of("count", 1, 4);

	private static final VoxelShape SHAPE_1 = Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 6.0, 12.0);
	private static final VoxelShape SHAPE_2 = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 6.0, 13.0);
	private static final VoxelShape SHAPE_3 = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 6.0, 14.0);
	private static final VoxelShape SHAPE_4 = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 6.0, 15.0);

	public AmmoBoxBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState().with(COUNT, 1));
	}

	@Override
	protected MapCodec<AmmoBoxBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(COUNT);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return switch (state.get(COUNT)) {
			case 2 -> SHAPE_2;
			case 3 -> SHAPE_3;
			case 4 -> SHAPE_4;
			default -> SHAPE_1;
		};
	}

	/**
	 * Additive placement, exactly {@code SeaPickleBlock}'s trick: placing
	 * another ammo box item while aimed at an existing (non-full) ammo box
	 * grows the pile instead of the placement sliding onto an adjacent face.
	 */
	@Override
	public boolean canReplace(BlockState state, ItemPlacementContext context) {
		return context.getStack().isOf(this.asItem()) && state.get(COUNT) < 4 || super.canReplace(state, context);
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState existing = ctx.getWorld().getBlockState(ctx.getBlockPos());
		if (existing.isOf(this)) {
			return existing.with(COUNT, Math.min(4, existing.get(COUNT) + 1));
		}
		return this.getDefaultState();
	}
}
