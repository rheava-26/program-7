package dev.rheava.program7.entity;

import dev.rheava.program7.block.GlowStickBlock;
import dev.rheava.program7.registry.P7Blocks;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Items;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The thrown glow stick — a light tool, not a weapon. Flies with a gentle,
 * floaty arc (lighter gravity than a snowball); on hitting a block it sticks
 * to the face it hit and becomes a {@link GlowStickBlock} oriented to that
 * surface (see that class for the burn-down mechanic). Hitting an entity, or
 * a block face that's already occupied, just drops it as a normal item
 * instead of destroying it — no damage either way.
 */
public class GlowStickEntity extends ThrownEntity {
	/** Floatier than {@code ThrownEntity}'s snowball-style default (~0.03) — a gentler, slower arc. */
	private static final double GRAVITY = 0.02;

	public GlowStickEntity(EntityType<? extends GlowStickEntity> entityType, World world) {
		super(entityType, world);
	}

	public GlowStickEntity(World world, LivingEntity owner) {
		super(P7Entities.GLOW_STICK.get(), owner, world);
	}

	/**
	 * Sets this glow stick flying from {@code thrower}'s look direction.
	 * {@code Entity#setVelocity(Entity, float, float, float, float, float)} is
	 * protected, so a launcher here (called from {@link
	 * dev.rheava.program7.item.GlowStickItem#use}) is the entry point rather
	 * than doing it from the item class directly — the plain
	 * {@code setVelocity(double, double, double)} attack goals use for shells
	 * is public, so this just computes the same shape from the player's look
	 * vector instead.
	 */
	public void launch(PlayerEntity thrower, float speed) {
		Vec3d look = thrower.getRotationVec(1.0F);
		this.setVelocity(look.x * speed, look.y * speed, look.z * speed);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		// No synced payload beyond position/velocity — nothing to track.
	}

	@Override
	protected double getGravity() {
		return GRAVITY;
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		if (!(this.getWorld() instanceof ServerWorld world)) {
			return;
		}
		if (hitResult instanceof BlockHitResult blockHit) {
			this.stickToBlock(world, blockHit);
		} else {
			// Hit an entity (or something else) — no damage, just falls to the
			// ground as a normal item instead of sticking.
			this.dropAsItem(world);
		}
		this.discard();
	}

	private void stickToBlock(ServerWorld world, BlockHitResult blockHit) {
		Direction hitSide = blockHit.getSide();
		BlockPos placePos = blockHit.getBlockPos().offset(hitSide);
		// Replaceable covers plain air as well as tall grass, snow layers, and
		// water/lava, so the stick can plant itself in foliage or snow instead of only
		// ever sticking over open air. Outside the world's build height, isReplaceable
		// can still read true (void air) even though world.setBlockState would silently
		// no-op there — check isInBuildLimit first so an out-of-range hit falls back to
		// dropping the item instead of scheduling a tick at a position that will never
		// hold a block.
		if (!world.isInBuildLimit(placePos) || !world.getBlockState(placePos).isReplaceable()) {
			// Target position is out of the world, or the face is already occupied by
			// something non-replaceable (e.g. another glow stick beat it there); don't
			// overwrite whatever's there.
			this.dropAsItem(world);
			return;
		}
		Direction facing = hitSide.getOpposite();
		BlockState state = P7Blocks.GLOW_STICK.get().getDefaultState()
				.with(GlowStickBlock.FACING, facing)
				.with(GlowStickBlock.STAGE, 0);
		if (!world.setBlockState(placePos, state)) {
			// Placement didn't actually take (e.g. lost a race with another change to
			// this position) — don't schedule a burn-down tick or play the stick sound
			// for a block that isn't there.
			this.dropAsItem(world);
			return;
		}
		world.scheduleBlockTick(placePos, P7Blocks.GLOW_STICK.get(), GlowStickBlock.STAGE_INTERVAL_TICKS);
		world.playSound(null, placePos, P7Sounds.GLOW_STICK_STICK.get(), SoundCategory.BLOCKS, 1.0F, 1.0F);
	}

	private void dropAsItem(ServerWorld world) {
		ItemEntity item = new ItemEntity(world, this.getX(), this.getY(), this.getZ(),
				new ItemStack(P7Items.GLOW_STICK.get()));
		world.spawnEntity(item);
	}
}
