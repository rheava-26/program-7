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
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Program's Tier 1 production building: battery + assembler, per the
 * unit bible. One arrives with every probe core. It watches the base's
 * defense complement and builds replacements out of the Director's ledger —
 * so wrecking its guards costs the Program real resources, and starving
 * the ledger silences it entirely.
 *
 * <p>Physically a 2x2 multiblock (see {@link AssemblerPart}): four adjacent
 * full-block cells sharing this one {@code Block} class, distinguished by the
 * {@link #PART} state property. Only the {@link AssemblerPart#MASTER}
 * (northwest) cell ever gets a real {@link AssemblerBlockEntity} — the other
 * three are inert filler that occupy the rest of the footprint. Every piece
 * of existing production logic keys off the master's own position exactly as
 * before ({@code AssemblerBlockEntity#getPos()} is always the master), so
 * behavior is unchanged; only the physical footprint grew from 1 block to 4.
 *
 * <p>Higher tech tiers will need physically larger assembler classes; this
 * block is the smallest one.
 */
public class AssemblerBlock extends BlockWithEntity {
	public static final MapCodec<AssemblerBlock> CODEC = createCodec(AssemblerBlock::new);
	/** Which of the 2x2 footprint's four cells this particular state occupies. */
	public static final EnumProperty<AssemblerPart> PART = EnumProperty.of("part", AssemblerPart.class);

	public AssemblerBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState().with(PART, AssemblerPart.MASTER));
	}

	@Override
	protected MapCodec<AssemblerBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		// Only the master cell carries the real block entity; the other three
		// footprint cells are inert.
		return state.get(PART) == AssemblerPart.MASTER ? new AssemblerBlockEntity(pos, state) : null;
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
				: validateTicker(type, P7BlockEntities.ASSEMBLER.get(), AssemblerBlockEntity::serverTick);
	}

	/**
	 * Best-effort fill-out for a player/creative placement of the assembler
	 * item: the master cell is already placed by the time this runs, so this
	 * just claims the other three footprint cells if they're free. Unlike
	 * {@link #placeMultiblock}, this never cancels the placement or destroys
	 * anything — a cramped placement just ends up with fewer filled cells,
	 * still fully functional off the master's block entity.
	 */
	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
			ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if (world.isClient || state.get(PART) != AssemblerPart.MASTER) {
			return;
		}
		for (AssemblerPart part : new AssemblerPart[] {AssemblerPart.EAST, AssemblerPart.SOUTH,
				AssemblerPart.SOUTHEAST}) {
			BlockPos childPos = pos.add(part.dx(), 0, part.dz());
			if (world.getBlockState(childPos).isReplaceable()) {
				world.setBlockState(childPos, state.with(PART, part));
			}
		}
	}

	/**
	 * Any one of the 2x2 footprint's four cells breaking takes the other
	 * three with it — a multiblock is one machine, not four independent
	 * blocks. Guarded the same way {@code FuelPlantBlock} guards its depot
	 * teardown, so re-setting the same block type (nothing actually changing)
	 * never recurses.
	 */
	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
			AssemblerPart part = state.get(PART);
			BlockPos origin = pos.add(-part.dx(), 0, -part.dz());
			for (BlockPos cell : footprintCells(origin)) {
				if (!cell.equals(pos) && serverWorld.getBlockState(cell).isOf(this)) {
					serverWorld.removeBlock(cell, false);
				}
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	/** Whether all four footprint cells starting at {@code origin} (its northwest corner) are free to build on. */
	public static boolean footprintClear(ServerWorld world, BlockPos origin) {
		for (BlockPos cell : footprintCells(origin)) {
			if (!world.getBlockState(cell).isReplaceable()) {
				return false;
			}
		}
		return true;
	}

	/** Stamp down all four cells of the multiblock, {@code origin} as the master (northwest) corner. */
	public static void placeMultiblock(ServerWorld world, BlockPos origin, BlockState masterState) {
		world.setBlockState(origin, masterState.with(PART, AssemblerPart.MASTER));
		world.setBlockState(origin.add(1, 0, 0), masterState.with(PART, AssemblerPart.EAST));
		world.setBlockState(origin.add(0, 0, 1), masterState.with(PART, AssemblerPart.SOUTH));
		world.setBlockState(origin.add(1, 0, 1), masterState.with(PART, AssemblerPart.SOUTHEAST));
	}

	private static BlockPos[] footprintCells(BlockPos origin) {
		return new BlockPos[] {origin, origin.add(1, 0, 0), origin.add(0, 0, 1), origin.add(1, 0, 1)};
	}
}
