package dev.rheava.program7.block;

import com.mojang.serialization.MapCodec;

import dev.rheava.program7.registry.P7BlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The block form of the Tier 1 fixed defense: a twin-barrel autogun bolted
 * onto a pedestal (see {@code AutogunTurretEntity} for the mob version this
 * is replacing). Being an actual block rather than a mob, it can be placed
 * and destroyed like any other piece of base infrastructure instead of
 * being knocked around or pathed away from its post.
 *
 * <p>All of the targeting/firing/overheat behavior lives in
 * {@link AutogunTurretBlockEntity}; this class is just the block shell,
 * mirroring {@link AssemblerBlock}'s structure.
 */
public class AutogunTurretBlock extends BlockWithEntity {
	public static final MapCodec<AutogunTurretBlock> CODEC = createCodec(AutogunTurretBlock::new);

	public AutogunTurretBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<AutogunTurretBlock> getCodec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new AutogunTurretBlockEntity(pos, state);
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
				: validateTicker(type, P7BlockEntities.AUTOGUN_TURRET.get(), AutogunTurretBlockEntity::serverTick);
	}
}
