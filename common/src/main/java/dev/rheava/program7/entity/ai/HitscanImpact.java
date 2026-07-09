package dev.rheava.program7.entity.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import dev.rheava.program7.Program7;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

/**
 * Shared "where did the round actually land" resolver for every hitscan
 * firearm in the Program's arsenal ({@link GunAttackGoal}, {@link
 * SniperAttackGoal}, and the autogun turret block entity). A shot that
 * doesn't connect with its target still has to go <i>somewhere</i> — this
 * finds the block (or fluid) it hits along the way and makes the world react:
 * a puff of block-crack dust or a splash on every impact, and — gated on the
 * {@code mobGriefing} gamerule — accumulating chip damage that eventually
 * breaks softer cover after enough hits.
 *
 * <p>Callers just hand over what they already computed for the tracer: the
 * muzzle position, the point the shot is aimed at (after any miss offset has
 * already been applied), the weapon's nominal range, and who to attribute a
 * block break to (may be {@code null} — the autogun turret block entity has
 * no {@link Entity} of its own to hand over).
 */
public final class HitscanImpact {
	/** Chip damage added to a block's accumulator for every round that lands on it. */
	private static final float CHIP_PER_HIT = 1.0f;
	/**
	 * Break threshold = {@code THRESHOLD_BASE + blastResistance * THRESHOLD_BLAST_SCALE}.
	 * Tuned so dirt/sand (blast resistance 0.5) go down in ~2 hits, planks
	 * (3.0) in ~3, stone/cobblestone (6.0) in ~5, and anything with
	 * obsidian-or-higher resistance (1200+) needs several hundred rounds —
	 * effectively immune to small-arms fire.
	 */
	private static final float THRESHOLD_BASE = 1.5f;
	private static final float THRESHOLD_BLAST_SCALE = 0.5f;
	/** Crack overlay stages run 0..9; anything higher isn't a valid stage. */
	private static final int MAX_CRACK_STAGE = 9;

	/** Per-world, per-position chip accumulation. Weak on the world so a closed/unloaded world doesn't leak. */
	private static final Map<ServerWorld, Map<BlockPos, Float>> CHIP_DAMAGE = new WeakHashMap<>();

	private HitscanImpact() {
	}

	/**
	 * Resolves a single hitscan round: raycasts for blocks from {@code origin}
	 * toward {@code aimPoint} (traveling at least as far as the aim point
	 * itself, plus a small buffer, so suppressive fire aimed beyond a mount's
	 * nominal {@code range} still registers where it lands), and — on the
	 * first solid block or fluid it finds — spawns impact particles and, for
	 * solid blocks, applies chip damage.
	 *
	 * <p>Safe to call on every shot, hit or miss: a round that flies clear
	 * (or connects with an entity, which this raycast ignores) simply finds
	 * no block and does nothing.
	 *
	 * @param world    the server world the shot is fired in
	 * @param origin   the muzzle position
	 * @param aimPoint where the shot is aimed, after any miss offset has
	 *                 already been folded in — i.e. where the tracer actually
	 *                 travels to
	 * @param range    the weapon's nominal range, used as a floor for how far
	 *                 to raycast
	 * @param shooter  entity to attribute a resulting block break to; may be
	 *                 {@code null} (e.g. a block-entity turret)
	 */
	public static void resolve(ServerWorld world, Vec3d origin, Vec3d aimPoint, double range,
			@Nullable Entity shooter) {
		Vec3d toAim = aimPoint.subtract(origin);
		double aimDistance = toAim.length();
		if (aimDistance < 1.0e-4) {
			return;
		}
		Vec3d direction = toAim.multiply(1.0 / aimDistance);
		// At least as far as the aim point, plus a hair further so a round
		// that clips terrain right at the aim point still registers a hit.
		double travel = Math.max(range, aimDistance) + 0.5;
		Vec3d endpoint = origin.add(direction.multiply(travel));

		// ShapeContext.absent() rather than a bare null entity, same as the
		// autogun turret's own raycast — null is ambiguous between the
		// Entity and ShapeContext raycast overloads.
		BlockHitResult hit = world.raycast(new RaycastContext(origin, endpoint,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, ShapeContext.absent()));
		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		BlockPos pos = hit.getBlockPos();
		BlockState state = world.getBlockState(pos);
		Vec3d hitPos = hit.getPos();

		if (!state.getFluidState().isEmpty()) {
			// Water (or lava) caught the round: a splash, no chip damage —
			// there's nothing here to break.
			world.spawnParticles(ParticleTypes.SPLASH, hitPos.x, hitPos.y, hitPos.z,
					10, 0.3, 0.1, 0.3, 0.05);
			return;
		}
		if (state.isAir()) {
			return;
		}

		// A little puff of the block's own material on every impact, hit or miss.
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
				hitPos.x, hitPos.y, hitPos.z, 12, 0.25, 0.25, 0.25, 0.0);

		chipBlock(world, pos, state, shooter);
	}

	/**
	 * Accumulates chip damage on {@code pos} and breaks it once the
	 * accumulator crosses a threshold scaled by the block's blast
	 * resistance. Gated on {@code mobGriefing}; never touches unbreakable
	 * blocks (negative hardness, e.g. bedrock) or anything in the Program's
	 * own {@code program7} namespace.
	 */
	private static void chipBlock(ServerWorld world, BlockPos pos, BlockState state, @Nullable Entity shooter) {
		if (!world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
			return;
		}
		float hardness = state.getHardness(world, pos);
		if (hardness < 0.0f) {
			// Unbreakable (bedrock and the like) — don't even start chipping it.
			return;
		}
		Identifier blockId = Registries.BLOCK.getId(state.getBlock());
		if (Program7.MOD_ID.equals(blockId.getNamespace())) {
			// Never grief the Program's own structures.
			return;
		}

		Map<BlockPos, Float> chip = CHIP_DAMAGE.computeIfAbsent(world, w -> new HashMap<>());
		float blastResistance = state.getBlock().getBlastResistance();
		float threshold = THRESHOLD_BASE + blastResistance * THRESHOLD_BLAST_SCALE;
		float accumulated = chip.getOrDefault(pos, 0.0f) + CHIP_PER_HIT;

		// A stable id per position for the crack overlay, offset into
		// negative space so it doesn't collide with any real entity's own
		// breaking-progress id (entity ids are always non-negative).
		int breakId = -1 - (pos.hashCode() & 0x7FFFFFFF) % 1_000_000;

		if (accumulated >= threshold) {
			chip.remove(pos);
			world.setBlockBreakingInfo(breakId, pos, -1);
			world.breakBlock(pos, false, shooter);
		} else {
			chip.put(pos, accumulated);
			int stage = Math.min(MAX_CRACK_STAGE, (int) ((accumulated / threshold) * (MAX_CRACK_STAGE + 1)));
			world.setBlockBreakingInfo(breakId, pos, stage);
		}
	}
}
