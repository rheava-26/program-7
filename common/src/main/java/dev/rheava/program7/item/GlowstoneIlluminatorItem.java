package dev.rheava.program7.item;

import java.util.List;

import org.joml.Vector3f;

import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * The glowstone illuminator — a handheld tracking-chip launcher, not a
 * weapon. Right-click fires a short-range hitscan "tag": a mob it connects
 * with gets slapped with {@link StatusEffects#GLOWING} for a long while, so
 * it lights up and stays trackable through walls, but the mob itself is
 * untouched — no damage, no knockback, no threat removed. Every shot — hit
 * or miss — draws a short tracer streak along the ray, and anything in the
 * way that isn't a taggable mob (a block, or a non-living hittable entity
 * like a boat or minecart) just gets a brief glow-particle flare, so even a
 * pure whiff into open sky still reads as "something happened" instead of a
 * silent whiff.
 *
 * <p>Deliberately not free to spam: it eats a glowstone dust per shot
 * (waived in creative) and carries a short cooldown, and the shot itself is
 * loud enough ({@link ProgramAcoustics#reportNoise}) that using it isn't
 * risk-free. Utility, not a horror-trivializer — see the design brief's line
 * on reverse-engineered player tools.
 */
public class GlowstoneIlluminatorItem extends Item {
	/** Short-range hitscan reach — a tagging tool, not a sniper rifle. */
	public static final double RANGE = 20.0;
	/** ~30 seconds of GLOWING on a tagged mob. */
	public static final int GLOW_DURATION_TICKS = 600;
	/** Small cooldown so the trigger can't be mashed into a machine gun. */
	private static final int USE_COOLDOWN_TICKS = 20;
	/** Slightly longer dry-fire cooldown — mirrors the charge laser's dry click pacing. */
	private static final int DRY_FIRE_COOLDOWN_TICKS = 10;
	/** Reported noise loudness on every shot, hit or miss — "loud-ish", carries further than a footstep. */
	private static final float NOISE_LOUDNESS = 0.6f;

	/**
	 * The tracer: a thin streak of small warm-yellow dust drawn muzzle-to-impact
	 * on every shot, mirroring {@link dev.rheava.program7.entity.ai.HitscanImpact#drawTracer}
	 * but tinted to match the item's glowstone theme rather than the firearms'
	 * red. Guarantees a pure whiff (nothing in range) still has a visible tell
	 * beyond the fire sound.
	 */
	private static final DustParticleEffect TRACER =
			new DustParticleEffect(new Vector3f(1.0f, 0.85f, 0.35f), 0.5f);
	/** Spacing (blocks) between tracer dots — tight, so the streak reads as a continuous line. */
	private static final double TRACER_SPACING = 0.4;

	public GlowstoneIlluminatorItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.pass(stack);
		}
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}
		if (!(world instanceof ServerWorld serverWorld)) {
			return TypedActionResult.pass(stack);
		}

		boolean creative = user.getAbilities().creativeMode;
		ItemStack ammo = creative ? ItemStack.EMPTY : findGlowstoneDust(user);
		if (!creative && ammo.isEmpty()) {
			world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.WEAPON_DRY_FIRE.get(),
					SoundCategory.PLAYERS, 0.6f, 1.0f);
			user.getItemCooldownManager().set(this, DRY_FIRE_COOLDOWN_TICKS);
			return TypedActionResult.fail(stack);
		}

		fire(serverWorld, user);

		if (!creative) {
			ammo.decrement(1);
		}
		user.getItemCooldownManager().set(this, USE_COOLDOWN_TICKS);
		user.incrementStat(Stats.USED.getOrCreateStat(this));
		return TypedActionResult.success(stack);
	}

	/**
	 * Resolves the shot: raycast the block world first (same proven idiom as
	 * {@link dev.rheava.program7.entity.ai.HitscanImpact}/{@code ChargeLaserItem}
	 * — {@code ProjectileUtil.getCollision} raycasts along the entity's
	 * velocity, which is ~zero for a standing player, so it never registers a
	 * hit), then look for an entity in front of whatever block stopped it so
	 * a wall blocks the tag rather than letting it punch through.
	 */
	private void fire(ServerWorld world, PlayerEntity user) {
		Vec3d start = user.getEyePos();
		Vec3d look = user.getRotationVec(1.0f);
		Vec3d rangeEnd = start.add(look.multiply(RANGE));

		BlockHitResult blockHit = world.raycast(new RaycastContext(start, rangeEnd,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
		Vec3d beamEnd = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getPos();
		Box searchBox = user.getBoundingBox().stretch(look.multiply(RANGE)).expand(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityCollision(world, user, start, beamEnd, searchBox,
				candidate -> candidate != user && candidate.canHit() && !candidate.isSpectator());

		ProgramAcoustics.emit(world, user.getPos(), P7Sounds.ILLUMINATOR_FIRE.get(), SoundCategory.PLAYERS, 0.7f, 1.0f);
		ProgramAcoustics.reportNoise(world, user.getX(), user.getY(), user.getZ(), NOISE_LOUDNESS);

		// Draw the tracer on every shot — hit or miss — so a pure whiff into
		// open sky (no entity, no block within range) still has a visible
		// tell beyond the fire sound.
		Vec3d tracerEnd = entityHit != null ? entityHit.getPos() : beamEnd;
		drawTracer(world, start, tracerEnd);

		if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target
				&& !(target instanceof PlayerEntity)) {
			tagMob(world, target);
		} else if (blockHit.getType() != HitResult.Type.MISS) {
			illuminatePoint(world, blockHit.getPos());
		} else if (entityHit != null) {
			// Hit something hittable that isn't a taggable living entity
			// (a boat, a minecart, a player) with no block behind it — fall
			// through to the same flare a block impact gets rather than
			// silently consuming the dust with no effect.
			illuminatePoint(world, entityHit.getPos());
		}
	}

	/**
	 * Draws the visible tracer streak from {@code from} (eye position) to
	 * {@code to} (where the shot ends up — an entity, a block, or the end of
	 * its range) as a thin line of small warm-yellow dust. Server-side; call
	 * once per shot, hit or miss.
	 */
	private void drawTracer(ServerWorld world, Vec3d from, Vec3d to) {
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
	 * The actual point of the item: mark {@code target} with a long GLOWING
	 * instance so it's trackable through walls, plus a burst of glow
	 * particles and a confirmation chime so the shooter gets an unambiguous
	 * "tagged" tell. No damage — this is a marker, not a weapon.
	 */
	private void tagMob(ServerWorld world, LivingEntity target) {
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, false));
		world.spawnParticles(ParticleTypes.GLOW,
				target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ(),
				16, target.getWidth() * 0.4, target.getHeight() * 0.4, target.getWidth() * 0.4, 0.01);
		world.playSound(null, target.getBlockPos(), P7Sounds.ILLUMINATOR_TAG.get(), SoundCategory.PLAYERS, 0.8f, 1.0f);
	}

	/**
	 * A shot that doesn't tag a mob still has to land somewhere: a small
	 * flare of glow/end-rod particles at the impact point — whether that's a
	 * block the beam struck, or a non-living hittable entity (boat,
	 * minecart) that stopped it with no block behind it. Deliberately just
	 * particles for this first cut — no light-emitting block placement,
	 * keeping the "marker, not a base-building tool" brief simple.
	 */
	private void illuminatePoint(ServerWorld world, Vec3d pos) {
		world.spawnParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 10, 0.15, 0.15, 0.15, 0.01);
		world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 4, 0.1, 0.1, 0.1, 0.01);
	}

	private static ItemStack findGlowstoneDust(PlayerEntity user) {
		for (int i = 0; i < user.getInventory().size(); i++) {
			ItemStack candidate = user.getInventory().getStack(i);
			if (candidate.isOf(Items.GLOWSTONE_DUST)) {
				return candidate;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.program7.glowstone_illuminator.tooltip").formatted(Formatting.GRAY));
	}
}
