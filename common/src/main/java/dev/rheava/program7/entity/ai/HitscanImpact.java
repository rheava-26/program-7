package dev.rheava.program7.entity.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import dev.rheava.program7.Program7;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
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
	/**
	 * Break threshold = {@code THRESHOLD_BASE + blastResistance * THRESHOLD_BLAST_SCALE}.
	 * Retuned (see the gun-feel/terrain-erosion pass) so small arms barely
	 * erode terrain instead of shredding a wall in one burst: against dirt/
	 * sand (blast resistance 0.5, threshold 9.0) a {@link RoundClass#LIGHT}
	 * turret round takes ~22 hits, {@link RoundClass#MEDIUM} ~16, and
	 * {@link RoundClass#HEAVY} (the gunship's belly gun) ~12 — the floor the
	 * design calls for. Stone/cobblestone (6.0, threshold 53.0) needs 70+
	 * rounds even from the heaviest round class — a full magazine and then
	 * some — reading as effectively small-arms-immune, and anything with
	 * obsidian-or-higher resistance (1200+) needs many thousands. Sustained
	 * explosive/howitzer fire is a different code path entirely and isn't
	 * gated by this at all.
	 */
	private static final float THRESHOLD_BASE = 5.0f;
	private static final float THRESHOLD_BLAST_SCALE = 8.0f;
	/** Crack overlay stages run 0..9; anything higher isn't a valid stage. */
	private static final int MAX_CRACK_STAGE = 9;
	/** Baseline particle count for the block-dust impact puff, before {@link RoundClass#particleCountScale()}. */
	private static final int BASE_IMPACT_PARTICLE_COUNT = 20;
	/** Baseline spread for the block-dust impact puff, before {@link RoundClass#particleSpreadScale()}. */
	private static final double BASE_IMPACT_SPREAD = 0.3;

	/** Per-world, per-position chip accumulation. Weak on the world so a closed/unloaded world doesn't leak. */
	private static final Map<ServerWorld, Map<BlockPos, Float>> CHIP_DAMAGE = new WeakHashMap<>();

	/**
	 * The tracer: a thin, soft red streak (the {@code DUST} particle is a small
	 * rounded glow, not a blocky sparkle) drawn muzzle-to-impact. Packed-int
	 * color per the 1.20.5+ {@link DustParticleEffect} constructor; scale below
	 * 1.0 keeps each dot small so the line reads thin.
	 */
	private static final DustParticleEffect TRACER = new DustParticleEffect(0xE01818, 0.6f);
	/** Spacing (blocks) between tracer dots — tight, so the streak looks continuous rather than dotted. */
	private static final double TRACER_SPACING = 0.4;

	private HitscanImpact() {
	}

	/**
	 * Draws the visible tracer streak from {@code from} (muzzle) to {@code to}
	 * (where the round is going) as a thin line of small red dust — replaces
	 * the old blocky white crit-sparkle tracer. Server-side; call once per shot.
	 */
	public static void drawTracer(ServerWorld world, Vec3d from, Vec3d to) {
		Vec3d delta = to.subtract(from);
		double length = delta.length();
		if (length < 1.0e-4) {
			return;
		}
		int steps = Math.max(2, (int) (length / TRACER_SPACING));
		Vec3d step = delta.multiply(1.0 / steps);
		Vec3d point = from;
		for (int i = 0; i < steps; i++) {
			point = point.add(step);
			world.spawnParticles(TRACER, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
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
	 * @param range      the weapon's nominal range, used as a floor for how far
	 *                   to raycast
	 * @param shooter    entity to attribute a resulting block break to; may be
	 *                   {@code null} (e.g. a block-entity turret)
	 * @param roundClass how big this round is — see {@link RoundClass} —
	 *                   scaling impact-particle size and terrain-chip weight
	 */
	public static void resolve(ServerWorld world, Vec3d origin, Vec3d aimPoint, double range,
			@Nullable Entity shooter, RoundClass roundClass) {
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
			int splashCount = Math.round(10 * roundClass.particleCountScale());
			world.spawnParticles(ParticleTypes.SPLASH, hitPos.x, hitPos.y, hitPos.z,
					splashCount, 0.3, 0.1, 0.3, 0.05);
			return;
		}
		if (state.isAir()) {
			return;
		}

		spawnImpactEffects(world, hitPos, state, roundClass);
		chipBlock(world, pos, state, shooter, roundClass);
	}

	/**
	 * The visible "something just got hit" tell: a puff of the block's own
	 * material, a spark pop, and a wisp of smoke, every one of them scaled up
	 * for a heavier round class so a gunship's belly gun reads as visibly
	 * more violent on impact than a turret's pop-gun rounds. Runs on every
	 * hit, hit or miss on the target, since this only cares about what the
	 * round actually struck.
	 */
	private static void spawnImpactEffects(ServerWorld world, Vec3d hitPos, BlockState state, RoundClass roundClass) {
		int blockDustCount = Math.round(BASE_IMPACT_PARTICLE_COUNT * roundClass.particleCountScale());
		double spread = BASE_IMPACT_SPREAD * roundClass.particleSpreadScale();
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
				hitPos.x, hitPos.y, hitPos.z, blockDustCount, spread, spread, spread, 0.03);

		int sparkCount = Math.max(3, Math.round(6 * roundClass.particleCountScale()));
		world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, hitPos.x, hitPos.y, hitPos.z,
				sparkCount, spread * 0.5, spread * 0.5, spread * 0.5, 0.08);

		int smokeCount = Math.max(2, Math.round(4 * roundClass.particleCountScale()));
		world.spawnParticles(ParticleTypes.SMOKE, hitPos.x, hitPos.y, hitPos.z,
				smokeCount, spread * 0.4, spread * 0.4, spread * 0.4, 0.02);
	}

	/**
	 * Ejects a single falling shell casing at the muzzle so a gun that's been
	 * firing visibly litters brass underneath itself — a cheap particle, not
	 * a real entity. Uses {@link net.minecraft.particle.ParticleTypes#FALLING_DUST}
	 * (the same physics-driven "falls, settles, fades" particle vanilla uses
	 * for suspicious sand) tinted with a copper block state for a warm brass
	 * read, offset a little to the side to suggest an ejection port rather
	 * than spawning dead-center on the barrel.
	 */
	public static void ejectCasing(ServerWorld world, Vec3d muzzle, Random random) {
		double sideOffset = (random.nextDouble() - 0.5) * 0.6;
		double x = muzzle.x + sideOffset;
		double y = muzzle.y - 0.1;
		double z = muzzle.z + sideOffset;
		world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, CASING_PARTICLE_STATE),
				x, y, z, 1, 0.05, 0.02, 0.05, 0.0);
	}

	/** Warm copper tint stood in for a brass shell casing — see {@link #ejectCasing}. */
	private static final BlockState CASING_PARTICLE_STATE = Blocks.RAW_COPPER_BLOCK.getDefaultState();

	/**
	 * Accumulates chip damage on {@code pos} and breaks it once the
	 * accumulator crosses a threshold scaled by the block's blast
	 * resistance. Gated on {@code mobGriefing}; never touches unbreakable
	 * blocks (negative hardness, e.g. bedrock) or anything in the Program's
	 * own {@code program7} namespace.
	 */
	private static void chipBlock(ServerWorld world, BlockPos pos, BlockState state, @Nullable Entity shooter,
			RoundClass roundClass) {
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
		float accumulated = chip.getOrDefault(pos, 0.0f) + roundClass.terrainChipWeight();

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
