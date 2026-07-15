package dev.rheava.program7.item;

import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * A controlled ender pearl — reverse-engineered from the Program's own drone
 * teleport logistics. Right-click and the shooter instantly blinks forward
 * along their look vector to just before the first solid surface, capped at
 * {@link #MAX_DISTANCE} blocks, with no fall damage on arrival. The landing
 * spot is verified, not just guessed at: it has to actually fit the player's
 * whole bounding box and can't be lava, backing off toward the shooter in
 * small steps until it finds one (or gives up and fizzles — see {@link
 * #fizzle}). If the ray finds a {@link LivingEntity} target before it finds a
 * wall, nothing warps: the shot just shoves them instead, same as a graze
 * from any other close-range hit. A non-living entity (a boat, a minecart,
 * an item frame, ...) in the way doesn't stop the blink at all — it just
 * blinks past it to the wall behind.
 *
 * <p>Design intent: a fun mobility toy, not a trivializer. It's gated by a
 * short {@link #COOLDOWN_TICKS} cooldown so it can't chain into a spammable
 * escape, and every blink is loud enough that {@link
 * dev.rheava.program7.audio.ProgramAcoustics#reportNoise} lets nearby Program
 * units notice the anomalous discharge and go investigate — see {@link
 * dev.rheava.program7.entity.ai.InvestigateNoiseGoal}.
 *
 * <p>Fully server-authoritative: the client's {@link #use} call only ever
 * returns immediately (so the arm still swings locally) and every effect —
 * the raycast, the teleport itself, the sounds, the pearl consumption — runs
 * on the server and is synced back down.
 */
public class EnderBlinkGunItem extends Item {
	/** Blink range cap, in blocks — "a controlled ender pearl," not a long-range escape. */
	public static final double MAX_DISTANCE = 12.0;
	/** How far short of a hit surface the landing spot backs off, so the shooter doesn't arrive embedded in a wall. */
	private static final double SURFACE_MARGIN = 0.5;
	/** ~2.5 seconds between blinks — enough that this can't chain into a spammable escape. */
	private static final int COOLDOWN_TICKS = 50;
	/** Small shove, well below vanilla melee's ~0.4 baseline — this is a graze, not a weapon hit. */
	private static final double KNOCKBACK_STRENGTH = 0.2;
	/** Bow-level attention on the network, same band as the charge laser's own noise report. */
	private static final float NOISE_LOUDNESS = 0.6f;
	/**
	 * Minimum resolved travel, in blocks, for a blink to actually go through.
	 * A point-blank wall clamps travel down near zero; below this it's not a
	 * blink, it's a whiff, so it fizzles instead of spending the pearl — see #2.
	 */
	private static final double MIN_TRAVEL = 1.5;
	/** Step size used when backing a blocked candidate landing spot off toward the shooter, hunting for clear space (see #1). */
	private static final double LANDING_STEP = 0.25;
	/** Short lockout on a fizzled blink (blocked landing / point-blank clamp) — long enough to block instant retries, far shorter than the real cooldown. */
	private static final int FIZZLE_COOLDOWN_TICKS = 10;
	/** Dry-fire (no pearl) lockout — mirrors the illuminator's ~10-tick dry-click pacing so this can't be mashed for free. */
	private static final int DRY_FIRE_COOLDOWN_TICKS = 10;

	public EnderBlinkGunItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		// Client-side call: just let the arm swing and wait for the server's
		// authoritative result (teleport, sounds, pearl consumption) to sync
		// back down. Vanilla's own cooldown manager already refuses to invoke
		// use() at all while this item is cooling down, so no explicit
		// isCoolingDown check is needed here (see GlowStickItem for the same
		// pattern).
		if (!(world instanceof ServerWorld serverWorld)) {
			return TypedActionResult.success(stack);
		}

		boolean creative = user.getAbilities().creativeMode;
		ItemStack pearl = creative ? ItemStack.EMPTY : findEnderPearl(user);
		if (!creative && pearl.isEmpty()) {
			this.dryFire(serverWorld, user);
			return TypedActionResult.fail(stack);
		}

		Vec3d origin = user.getPos();
		Vec3d eyeStart = user.getEyePos();
		Vec3d look = user.getRotationVec(1.0f);
		Vec3d eyeRangeEnd = eyeStart.add(look.multiply(MAX_DISTANCE));

		// Same block-then-entity idiom as the charge laser's hitscan (see
		// HitscanImpact / ChargeLaserItem#usageTick): raycast the block world
		// first so a wall stops the ray, then look for a living target in
		// front of whatever stopped it.
		BlockHitResult blockHit = world.raycast(new RaycastContext(eyeStart, eyeRangeEnd,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
		double blockDistance = blockHit.getType() == HitResult.Type.MISS
				? MAX_DISTANCE
				: eyeStart.distanceTo(blockHit.getPos());

		Box searchBox = user.getBoundingBox().stretch(look.multiply(MAX_DISTANCE)).expand(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityCollision(world, user, eyeStart,
				eyeStart.add(look.multiply(blockDistance)), searchBox,
				candidate -> candidate != user && candidate.canHit() && !candidate.isSpectator());

		// The emitter always discharges once triggered, whether that lands a
		// blink or just a shove — same "fired a round" accounting either way.
		world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.BLINK_GUN_CHARGE.get(),
				SoundCategory.PLAYERS, 0.7f, 0.95f + world.getRandom().nextFloat() * 0.1f);

		if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
			// Shove AWAY from the shooter: takeKnockback shoves opposite the
			// (x,z) it's handed, and vanilla passes (attacker - target) — see
			// the identical comment in GunAttackGoal.
			Vec3d shove = origin.subtract(target.getPos());
			if (shove.lengthSquared() > 1.0e-4) {
				target.takeKnockback(KNOCKBACK_STRENGTH, shove.x, shove.z);
			}
			// A close-range shove is still an anomalous discharge — report it
			// on the Program network same as a landed blink, not just the
			// teleport path (see #3).
			ProgramAcoustics.reportNoise(serverWorld, user.getX(), user.getY(), user.getZ(), NOISE_LOUDNESS);
			this.finishUse(user, pearl, creative);
			return TypedActionResult.success(stack);
		}
		// A non-living hittable entity in the ray (boat, minecart, item frame,
		// ...) does NOT cancel the blink — fall through and land on the block
		// impact behind it, same as if nothing had been hit at all (see #4).

		double rawTravel = MathHelper.clamp(blockDistance - SURFACE_MARGIN, 0.0, MAX_DISTANCE);
		Vec3d destination = rawTravel >= MIN_TRAVEL
				? findSafeDestination(world, user, origin, look, rawTravel)
				: null;

		if (destination == null) {
			// Either the clamp left basically no travel (point-blank wall) or
			// every candidate landing spot down to MIN_TRAVEL was blocked or
			// lava: fizzle instead of spending the pearl and charging the full
			// cooldown for a blink that didn't actually go anywhere (see #1/#2).
			this.fizzle(serverWorld, user);
			return TypedActionResult.success(stack);
		}

		spawnWarpParticles(serverWorld, eyeStart);
		world.playSound(null, origin.x, origin.y, origin.z, P7Sounds.BLINK_GUN_WARP.get(),
				SoundCategory.PLAYERS, 1.0f, 1.0f);

		if (user.hasVehicle()) {
			user.stopRiding();
		}
		user.requestTeleportAndDismount(destination.x, destination.y, destination.z);
		// No fall damage on arrival — this is a controlled blink, not a plunge.
		user.fallDistance = 0.0f;

		spawnWarpParticles(serverWorld, destination);
		world.playSound(null, destination.x, destination.y, destination.z, P7Sounds.BLINK_GUN_WARP.get(),
				SoundCategory.PLAYERS, 1.0f, 1.0f);

		ProgramAcoustics.reportNoise(serverWorld, origin.x, origin.y, origin.z, NOISE_LOUDNESS);

		this.finishUse(user, pearl, creative);
		return TypedActionResult.success(stack);
	}

	private void finishUse(PlayerEntity user, ItemStack pearl, boolean creative) {
		if (!creative && !pearl.isEmpty()) {
			pearl.decrement(1);
		}
		user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
		user.incrementStat(Stats.USED.getOrCreateStat(this));
	}

	private void dryFire(ServerWorld world, PlayerEntity user) {
		world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.WEAPON_DRY_FIRE.get(),
				SoundCategory.PLAYERS, 0.6f, 1.0f);
		user.getItemCooldownManager().set(this, DRY_FIRE_COOLDOWN_TICKS);
	}

	/**
	 * A blink that didn't go anywhere — point-blank clamp or no clear landing
	 * spot found short of {@link #MIN_TRAVEL} (see #1/#2). No pearl spent, no
	 * long cooldown: just a short lockout and a soft "that didn't work" cue so
	 * it can't be mashed for free against a wall.
	 */
	private void fizzle(ServerWorld world, PlayerEntity user) {
		world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.WEAPON_DRY_FIRE.get(),
				SoundCategory.PLAYERS, 0.5f, 0.8f);
		user.getItemCooldownManager().set(this, FIZZLE_COOLDOWN_TICKS);
	}

	/**
	 * Walks the candidate landing spot back from {@code travel} toward
	 * {@code origin} in {@link #LANDING_STEP} increments, looking for a
	 * distance whose full player bounding box is actually clear — see #1.
	 * Returns {@code null} if nothing clear turns up before travel drops
	 * under {@link #MIN_TRAVEL}.
	 */
	private static Vec3d findSafeDestination(World world, PlayerEntity user, Vec3d origin, Vec3d look, double travel) {
		for (double t = travel; t >= MIN_TRAVEL; t -= LANDING_STEP) {
			Vec3d candidate = origin.add(look.multiply(t));
			if (isSafeLanding(world, user, origin, candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Whether the player's actual bounding box — not just the eye-level point
	 * the block raycast found — fits at {@code dest} without overlapping any
	 * collision, and isn't landing in lava. Water is fine (the block raycast
	 * already passes through fluids via {@code FluidHandling.NONE}); lava
	 * specifically has to be rejected by hand because fluids carry no
	 * blocking collision shape, so {@code isSpaceEmpty} alone would happily
	 * call a lava pool "clear".
	 */
	private static boolean isSafeLanding(World world, PlayerEntity user, Vec3d origin, Vec3d dest) {
		Box destBox = user.getBoundingBox().offset(dest.subtract(origin));
		if (!world.isSpaceEmpty(user, destBox)) {
			return false;
		}
		if (isLava(world, dest)) {
			return false;
		}
		// Also check roughly eye height at the destination: a low ceiling
		// over a lava pool could otherwise leave the feet clear while the
		// head lands in lava.
		Vec3d eyeOffset = user.getEyePos().subtract(origin);
		return !isLava(world, dest.add(eyeOffset));
	}

	private static boolean isLava(World world, Vec3d pos) {
		return world.getFluidState(BlockPos.ofFloored(pos)).isIn(FluidTags.LAVA);
	}

	private static ItemStack findEnderPearl(PlayerEntity user) {
		for (int i = 0; i < user.getInventory().size(); i++) {
			ItemStack candidate = user.getInventory().getStack(i);
			if (candidate.isOf(Items.ENDER_PEARL)) {
				return candidate;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void spawnWarpParticles(ServerWorld world, Vec3d pos) {
		world.spawnParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 24, 0.3, 0.5, 0.3, 0.4);
		world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 12, 0.3, 0.5, 0.3, 0.05);
	}
}
