package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.registry.P7BlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The physical seat of the Program's research — a single strategic choke
 * point per {@code RESEARCH_TREE.md} §1/§5: "cripple it... and the whole
 * tree stalls." Follows {@link ProbeCoreBlock} exactly, the doc's own
 * chosen template (§5: "Follows ProbeCoreBlock/ProbeCoreBlockEntity
 * exactly, because that pair already models everything a disruptable
 * Program structure needs").
 *
 * <ul>
 *   <li>{@link #onBlockBreakStart} fires the instant a player starts mining
 *   it — {@link PsionicResearchBlockEntity#markDamaged} records it, which
 *   {@code ResearchTree.tick} reads via {@code isUnderAttack} to pause feed
 *   steps without reversing progress (doc §5 "damaging it").</li>
 *   <li>{@link #onStateReplaced} fires once the building is actually gone
 *   and reports it to {@link dev.rheava.program7.director.ResearchTree#onBuildingDestroyed}
 *   — a real setback (half the in-flight node's progress, per doc §5), not
 *   a wipe.</li>
 * </ul>
 */
public class PsionicResearchBlock extends BlockWithEntity {
	public static final MapCodec<PsionicResearchBlock> CODEC = createCodec(PsionicResearchBlock::new);

	public PsionicResearchBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<PsionicResearchBlock> getCodec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new PsionicResearchBlockEntity(pos, state);
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
				: validateTicker(type, P7BlockEntities.PSIONIC_RESEARCH.get(), PsionicResearchBlockEntity::serverTick);
	}

	@Override
	public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		if (!world.isClient && world.getBlockEntity(pos) instanceof PsionicResearchBlockEntity building) {
			building.markDamaged(world.getTime());
		}
		super.onBlockBreakStart(state, world, pos, player);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
			ProgramDirectorState.get(serverWorld).getResearchTree().onBuildingDestroyed(serverWorld, pos);
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
