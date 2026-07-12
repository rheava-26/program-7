package dev.rheava.program7.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for the Program's carried cargo: the small pseudo-blocks a
 * logistics drone hauls in and clusters on the ground next to whatever it's
 * servicing. Two behaviours live here:
 *
 * <ul>
 *   <li><b>Additive stacking</b> — exactly vanilla {@code SeaPickleBlock}'s
 *       {@link #COUNT} trick (1-4): placing another of the same item against
 *       an existing, non-full pile grows the pile in place instead of taking
 *       an adjacent cell, so a delivered stockpile reads as a growing cluster
 *       of crates/shells rather than a wall of blocks.</li>
 *   <li><b>Cook-off</b> — this is live ordnance sitting in the open. Fire or
 *       lava next to it, or a burning projectile (flame arrow) striking it,
 *       sets it off: the pile is removed and detonates with a blast that
 *       scales by munition type and by how many are stacked. Shooting a
 *       shell dump with fire arrows should be a real tactic.</li>
 * </ul>
 *
 * <p>Concrete types ({@link AmmoBoxBlock}, {@link CrateBlock},
 * {@link AutocannonMagazineBlock}, {@link ArtilleryShellBlock},
 * {@link PowerCellBlock}) supply their silhouette height and base blast
 * power; the drone-facing draw-down logic lives in {@code entity.ai.AmmoRunGoal}.
 */
public abstract class CargoBlock extends Block {
	/** How many are piled in this cell, 1-4 — same range/idiom as vanilla's sea pickle {@code PICKLES}. */
	public static final IntProperty COUNT = IntProperty.of("count", 1, 4);

	private final VoxelShape[] shapes;
	private final float baseBlastPower;
	private final float blastPerExtra;

	/**
	 * @param height         silhouette height in pixels for the collision/outline box
	 * @param baseBlastPower cook-off power at COUNT 1 (vanilla TNT is 4.0)
	 */
	protected CargoBlock(Settings settings, int height, float baseBlastPower) {
		super(settings);
		this.baseBlastPower = baseBlastPower;
		this.blastPerExtra = baseBlastPower * 0.4f;
		int h = Math.max(1, height);
		// Footprint widens with the pile, matching the ammo-box progression.
		this.shapes = new VoxelShape[] {
				Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, h, 12.0),
				Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, h, 13.0),
				Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, h, 14.0),
				Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, h, 15.0),
		};
		this.setDefaultState(this.getStateManager().getDefaultState().with(COUNT, 1));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(COUNT);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.shapes[Math.min(this.shapes.length - 1, state.get(COUNT) - 1)];
	}

	/** Additive placement, exactly {@code SeaPickleBlock}'s trick. */
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

	// ---- cook-off ----------------------------------------------------------

	/** A burning projectile (flame arrow, etc.) striking the pile sets it off. */
	@Override
	public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		super.onProjectileHit(world, state, hit, projectile);
		if (!world.isClient && projectile.isOnFire()) {
			this.cookOff(world, hit.getBlockPos(), state.get(COUNT));
		}
	}

	/** Fire or lava coming to rest against the pile cooks it off. */
	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
		super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
		if (world.isClient) {
			return;
		}
		for (Direction dir : Direction.values()) {
			BlockState neighbor = world.getBlockState(pos.offset(dir));
			if (neighbor.isIn(BlockTags.FIRE) || neighbor.getFluidState().isIn(FluidTags.LAVA)) {
				this.cookOff(world, pos, state.get(COUNT));
				return;
			}
		}
	}

	private void cookOff(World world, BlockPos pos, int count) {
		if (world.getBlockState(pos).getBlock() != this) {
			return;
		}
		float power = this.baseBlastPower + this.blastPerExtra * (count - 1);
		world.removeBlock(pos, false);
		world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				power, World.ExplosionSourceType.BLOCK);
	}
}
