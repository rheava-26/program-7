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
 * The block form of a landed probe core — the heart of a Program base.
 * Promoted from a plain {@code Block} into a block entity so the base can
 * carry an HP/under-attack model ({@link ProbeCoreBlockEntity}) and fire a
 * real consequence when it's actually cracked open, mirroring {@link
 * AutogunTurretBlock}'s BlockWithEntity shape (see {@link
 * AutogunTurretBlockEntity} for the pattern this follows).
 *
 * <p>Two hooks matter here:
 * <ul>
 *   <li>{@link #onBlockBreakStart} fires the instant a player starts mining
 *   the core — that's the "someone is attacking the base right now" signal,
 *   recorded on the block entity for a later defense-escalation task to
 *   read.</li>
 *   <li>{@link #onStateReplaced} fires once the core is actually gone
 *   (matches how {@link DroneWreckBlock} tells a real removal from a
 *   same-block state update) and reports it to the Director as the base
 *   being destroyed.</li>
 * </ul>
 */
public class ProbeCoreBlock extends BlockWithEntity {
	public static final MapCodec<ProbeCoreBlock> CODEC = createCodec(ProbeCoreBlock::new);

	public ProbeCoreBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<ProbeCoreBlock> getCodec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ProbeCoreBlockEntity(pos, state);
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
				: validateTicker(type, P7BlockEntities.PROBE_CORE.get(), ProbeCoreBlockEntity::serverTick);
	}

	@Override
	public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		if (!world.isClient && world.getBlockEntity(pos) instanceof ProbeCoreBlockEntity core) {
			core.markDamaged(world.getTime());
		}
		super.onBlockBreakStart(state, world, pos, player);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
			ProgramDirectorState.get(serverWorld).onBaseDestroyed(serverWorld, pos);
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
