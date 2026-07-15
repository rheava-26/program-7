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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * A controlled ender pearl — reverse-engineered from the Program's own drone
 * teleport logistics. Right-click and the shooter instantly blinks forward
 * along their look vector to just before the first solid surface, capped at
 * {@link #MAX_DISTANCE} blocks, with no fall damage on arrival. If the ray
 * finds a living target before it finds a wall, nothing warps: the shot just
 * shoves them instead, same as a graze from any other close-range hit.
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

		if (entityHit != null) {
			if (entityHit.getEntity() instanceof LivingEntity target) {
				// Shove AWAY from the shooter: takeKnockback shoves opposite the
				// (x,z) it's handed, and vanilla passes (attacker - target) — see
				// the identical comment in GunAttackGoal.
				Vec3d shove = origin.subtract(target.getPos());
				if (shove.lengthSquared() > 1.0e-4) {
					target.takeKnockback(KNOCKBACK_STRENGTH, shove.x, shove.z);
				}
			}
			this.finishUse(user, pearl, creative);
			return TypedActionResult.success(stack);
		}

		double travel = MathHelper.clamp(blockDistance - SURFACE_MARGIN, 0.0, MAX_DISTANCE);
		Vec3d destination = origin.add(look.multiply(travel));

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
