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
 * The Program's Tier 1 production building: battery + assembler, per the
 * unit bible. One arrives with every probe core. It watches the base's
 * defense complement and builds replacements out of the Director's ledger —
 * so wrecking its guards costs the Program real resources, and starving
 * the ledger silences it entirely.
 *
 * <p>Higher tech tiers will need physically larger assembler classes; this
 * block is the smallest one.
 */
public class AssemblerBlock extends BlockWithEntity {
	public static final MapCodec<AssemblerBlock> CODEC = createCodec(AssemblerBlock::new);

	public AssemblerBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<AssemblerBlock> getCodec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new AssemblerBlockEntity(pos, state);
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
}
