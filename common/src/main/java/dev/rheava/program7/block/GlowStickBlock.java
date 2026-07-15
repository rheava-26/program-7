package dev.rheava.program7.block;

import java.util.EnumMap;
import java.util.Map;

import dev.rheava.program7.registry.P7Items;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

/**
 * A thrown {@link dev.rheava.program7.entity.GlowStickEntity} sticks itself
 * to whatever face it hits and becomes one of these — the doc's Phase 4
 * "reverse engineering" first tool, and deliberately NOT a permanent light
 * source (the mod's own line: player tools have to stay cool without
 * trivializing the horror). {@link #FACING} records which face it's stuck
 * to (the direction from this block toward the block supporting it, i.e.
 * exactly {@code BlockHitResult.getSide().getOpposite()} at the moment it
 * landed) so the same block class covers floor/wall/ceiling like a torch.
 *
 * <p>{@link #STAGE} (0-3) is the burn-down clock: a {@link #scheduledTick}
 * chain steps it up every {@link #STAGE_INTERVAL_TICKS}, each step dimming
 * {@link #luminanceForStage}, until the last stage's tick removes the block
 * outright with {@link net.minecraft.world.World#removeBlock} rather than
 * {@code breakBlock} — that bypasses the loot table entirely, so a glow
 * stick that burns out on its own drops nothing. A player who mines it
 * early (any stage) gets the ordinary block-broken drop from the loot table
 * instead, satisfying "breaking it while still glowing may drop it back"
 * with no special-case code.
 *
 * <p>No block entity: the whole burn-down state fits in one {@link
 * IntProperty}, so this follows the "prefer scheduled ticks over a BE"
 * option instead of {@code CrateBlockEntity}'s countdown pattern.
 *
 * <p>Known gap: unlike a real torch this doesn't watch its support block —
 * mining out the block it's stuck to leaves it floating rather than popping
 * off. Left out to keep the neighbor-update/support-check surface (a part
 * of the 1.20.5+ block API that changed shape) out of a change nobody can
 * compile-check locally; a later pass can add it once it can be verified
 * against a real client.
 */
public class GlowStickBlock extends Block {
	public static final DirectionProperty FACING = Properties.FACING;
	public static final IntProperty STAGE = IntProperty.of("stage", 0, 3);

	/** ~800 ticks (40s) per stage; 4 stages = ~2m40s total, inside the doc's 2-4 minute window. */
	public static final int STAGE_INTERVAL_TICKS = 800;

	private static final int[] LUMINANCE_BY_STAGE = {12, 9, 6, 3};

	private static final Map<Direction, VoxelShape> SHAPES = buildShapes();

	public GlowStickBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.DOWN).with(STAGE, 0));
	}

	/** Exposed so {@code P7Blocks} can wire {@code Settings#luminance} straight to the stage table. */
	public static int luminanceForStage(int stage) {
		return LUMINANCE_BY_STAGE[MathHelper.clamp(stage, 0, LUMINANCE_BY_STAGE.length - 1)];
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, STAGE);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES.getOrDefault(state.get(FACING), VoxelShapes.fullCube());
	}

	/**
	 * There's no {@code BlockItem} for this block (see {@code P7Blocks}'s
	 * javadoc on {@link dev.rheava.program7.registry.P7Blocks#GLOW_STICK}), so
	 * without this override middle-click pick-block on a placed stick would
	 * fall through to {@link Block#asItem()} and yield air.
	 */
	@Override
	public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
		return new ItemStack(P7Items.GLOW_STICK.get());
	}

	/**
	 * The burn-down clock. {@link dev.rheava.program7.entity.GlowStickEntity}
	 * kicks off the first tick when it places the block; every tick after
	 * that is scheduled here, so nothing else needs to touch this block again.
	 */
	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!world.getBlockState(pos).isOf(this)) {
			return;
		}
		int stage = state.get(STAGE);
		if (stage >= LUMINANCE_BY_STAGE.length - 1) {
			// Burned all the way down. Direct removal (not breakBlock/harvest) so
			// the loot table never fires — a spent glow stick drops nothing.
			world.playSound(null, pos, P7Sounds.GLOW_STICK_FADE.get(), SoundCategory.BLOCKS, 0.6F, 0.9F);
			world.removeBlock(pos, false);
			return;
		}
		world.setBlockState(pos, state.with(STAGE, stage + 1));
		world.scheduleBlockTick(pos, this, STAGE_INTERVAL_TICKS);
	}

	private static Map<Direction, VoxelShape> buildShapes() {
		Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
		double lo = 7.0, hi = 9.0;
		// Each shape is flush against the supporting face (the direction this
		// block's FACING points) and reaches 10px out from it.
		shapes.put(Direction.DOWN, Block.createCuboidShape(lo, 0.0, lo, hi, 10.0, hi));
		shapes.put(Direction.UP, Block.createCuboidShape(lo, 6.0, lo, hi, 16.0, hi));
		shapes.put(Direction.NORTH, Block.createCuboidShape(lo, lo, 0.0, hi, hi, 10.0));
		shapes.put(Direction.SOUTH, Block.createCuboidShape(lo, lo, 6.0, hi, hi, 16.0));
		shapes.put(Direction.WEST, Block.createCuboidShape(0.0, lo, lo, 10.0, hi, hi));
		shapes.put(Direction.EAST, Block.createCuboidShape(6.0, lo, lo, 16.0, hi, hi));
		return shapes;
	}
}
