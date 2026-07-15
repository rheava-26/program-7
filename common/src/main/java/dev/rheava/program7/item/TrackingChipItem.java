package dev.rheava.program7.item;

import java.util.List;

import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.registry.P7DataComponents;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
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
 * A reverse-engineered {@code transmitter} — the player's first piece of
 * intel gear (see {@code docs/DESIGN.md}: "tracking chips let you mark
 * valuables and follow them"). Deliberately narrow in scope: it only ever
 * tracks the one target <em>you</em> personally mark, unlike the Program's
 * own always-on scanning, so it never turns into a free radar.
 *
 * <p><b>Tag</b> — right-click a living entity ({@link #useOnEntity}) to mark
 * it, or right-click the air near a dropped item stack ({@link #use}, via a
 * short raycast bounded by the block world so a wall stops it, same idiom as
 * {@code ChargeLaserItem}/{@code GlowstoneIlluminatorItem}) to mark that
 * instead. Either way the target's UUID and current position are written
 * onto the stack as a {@link TrackingChipTarget} data component, and the
 * target is made to glow for a while so you can eyeball it immediately —
 * <em>only</em> on this in-person tag, not on every idle read (see below).
 *
 * <p><b>Read</b> — right-click the air with nothing nearby to tag: if a
 * target is stored, resolves it against the world's currently loaded
 * entities. Loaded: refresh the stored position and print a bearing +
 * distance to the actionbar — but it does <em>not</em> re-apply the glow.
 * Not loaded (out of range, in an unloaded chunk, or dead and gone): print
 * "signal lost" — the chip never shows a target it can't currently see, only
 * the direction/range as of the last successful read. Both actions share a
 * short cooldown, and a read reports a faint noise, so this can't be used as
 * a free, silent, unlimited-range through-wall wallhack by mashing the
 * button — that's what made the old always-glow read a renewable version of
 * the illuminator's dust+noise+cooldown-gated tag, without the cost. The
 * datapad (see {@code DatapadItem}/{@code DatapadScreen}) is the intended
 * "see the exact location" surface now: it plots every carried chip's
 * stored target directly as a distinct tracked blip, independent of this
 * item's own glow/readout.
 *
 * <p>Server-authoritative throughout: both branches bail out on the client
 * and let the server resolve everything, same as {@code ChargeLaserItem}'s
 * reload path.
 */
public class TrackingChipItem extends Item {
	/** How far the "mark a dropped item" raycast reaches — a deliberate wrist flick, not a sniper mark. */
	private static final double ITEM_TAG_RANGE = 4.5;

	/** How long the glow lasts per in-person tag — long enough to spot the target, short enough to need a re-tag. */
	private static final int GLOW_TICKS = 20 * 30;

	/** Shared tag/read cooldown — stops either action from being mashed into a free, spammable ping. */
	private static final int USE_COOLDOWN_TICKS = 20;

	/** Faint noise a read reports — quieter than the illuminator's shot, but no longer silent. */
	private static final float READ_NOISE_LOUDNESS = 0.2f;

	private static final String[] COMPASS_POINTS =
			{"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	public TrackingChipItem(Settings settings) {
		super(settings);
	}

	// ---- tag: right-click on a living entity ---------------------------------

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		if (entity == user) {
			// Tagging yourself is a no-op, not an error — let the interaction fall
			// through instead of consuming it. Checked first, before the
			// client/server branch below, so both sides agree on PASS instead of
			// the client swinging an arm over an interaction the server no-ops.
			return ActionResult.PASS;
		}
		// Same reasoning for the cooldown: check it before the client/server
		// split too (the cooldown manager is synced to the client), so a
		// cooling-down chip doesn't swing an arm on the client only to have the
		// server silently no-op it.
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return ActionResult.PASS;
		}
		World world = user.getWorld();
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		this.tag(stack, user, entity);
		user.getItemCooldownManager().set(this, USE_COOLDOWN_TICKS);
		return ActionResult.CONSUME;
	}

	// ---- tag a nearby dropped item, or read the stored target -----------------

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

		ItemEntity nearbyItem = this.raycastItemEntity(serverWorld, user);
		if (nearbyItem != null) {
			this.tag(stack, user, nearbyItem);
		} else {
			this.read(stack, user, serverWorld);
		}
		user.getItemCooldownManager().set(this, USE_COOLDOWN_TICKS);
		return TypedActionResult.consume(stack);
	}

	/**
	 * Raycasts the block world first (the repo's proven idiom — see
	 * {@code ChargeLaserItem}/{@code GlowstoneIlluminatorItem} — since {@code
	 * ProjectileUtil.getCollision} raycasts along the entity's velocity, which
	 * is ~zero for a standing player and never registers a hit), then bounds
	 * the entity search to whatever's nearer, the block hit or full range, so
	 * a wall stops the tag instead of letting it punch through to a dropped
	 * item on the other side.
	 */
	private ItemEntity raycastItemEntity(ServerWorld world, PlayerEntity user) {
		Vec3d start = user.getEyePos();
		Vec3d look = user.getRotationVec(1.0f);
		Vec3d rangeEnd = start.add(look.multiply(ITEM_TAG_RANGE));

		BlockHitResult blockHit = world.raycast(new RaycastContext(start, rangeEnd,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
		Vec3d end = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getPos();

		Box searchBox = user.getBoundingBox().stretch(look.multiply(ITEM_TAG_RANGE)).expand(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityCollision(world, user, start, end, searchBox,
				candidate -> candidate instanceof ItemEntity item && item.isAlive() && !item.getStack().isEmpty());
		return hit != null && hit.getEntity() instanceof ItemEntity item ? item : null;
	}

	// ---- shared tag/read logic --------------------------------------------

	private void tag(ItemStack stack, PlayerEntity user, Entity target) {
		World world = user.getWorld();
		this.clearPreviousGlow(stack, user, target);
		stack.set(P7DataComponents.TRACKING_CHIP_TARGET,
				new TrackingChipTarget(target.getUuid(), target.getX(), target.getY(), target.getZ()));
		applyGlow(target);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.TRACKING_CHIP_PING.get(),
				SoundCategory.PLAYERS, 0.5f, 1.5f);
		user.sendMessage(Text.translatable("item.program7.tracking_chip.tagged", target.getDisplayName())
				.formatted(Formatting.AQUA), true);
	}

	/**
	 * Re-tagging overwrites the stack's stored target — but a dropped item's
	 * glow is the raw render-only flag (see {@link #applyGlow}), which has no
	 * built-in expiry of its own, so without this the <em>old</em> item stays
	 * glowing through walls forever, even across restarts (it's persisted in
	 * the entity's NBT). Only item entities need this: a re-tagged living
	 * entity's glow is a timed {@link StatusEffects#GLOWING} instance that
	 * expires on its own.
	 */
	private void clearPreviousGlow(ItemStack stack, PlayerEntity user, Entity newTarget) {
		TrackingChipTarget previous = stack.get(P7DataComponents.TRACKING_CHIP_TARGET);
		if (previous == null || previous.targetUuid().equals(newTarget.getUuid())) {
			return;
		}
		if (!(user.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		Entity old = serverWorld.getEntity(previous.targetUuid());
		if (old != null && !(old instanceof LivingEntity)) {
			old.setGlowing(false);
		}
	}

	private void read(ItemStack stack, PlayerEntity user, ServerWorld world) {
		TrackingChipTarget target = stack.get(P7DataComponents.TRACKING_CHIP_TARGET);
		if (target == null) {
			user.sendMessage(Text.translatable("item.program7.tracking_chip.no_target").formatted(Formatting.GRAY), true);
			return;
		}

		Entity found = world.getEntity(target.targetUuid());
		Vec3d targetPos;
		if (found != null && found.isAlive()) {
			targetPos = found.getPos();
			stack.set(P7DataComponents.TRACKING_CHIP_TARGET, target.withPos(targetPos.x, targetPos.y, targetPos.z));
			world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.TRACKING_CHIP_PING.get(),
					SoundCategory.PLAYERS, 0.4f, 1.7f);
		} else {
			user.sendMessage(Text.translatable("item.program7.tracking_chip.signal_lost").formatted(Formatting.RED), true);
			return;
		}

		// Deliberately no applyGlow() here — a read no longer re-applies the
		// through-wall glow (that's now only an in-person tag()'s cost/reward),
		// and it reports a faint noise so idly mashing this for a bearing isn't
		// free and silent either. See the class doc.
		ProgramAcoustics.reportNoise(world, user.getX(), user.getY(), user.getZ(), READ_NOISE_LOUDNESS);

		double dx = targetPos.x - user.getX();
		double dz = targetPos.z - user.getZ();
		double dy = targetPos.y - user.getY();
		int distance = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
		// atan2(dx, -dz): 0 = north (-Z), 90 = east (+X), matching Minecraft's
		// axes — same convention as DatapadItem's base-bearing calculation.
		double bearingDeg = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dx, -dz)));
		user.sendMessage(Text.translatable("item.program7.tracking_chip.bearing",
				compassLabel(bearingDeg), distance).formatted(Formatting.AQUA), true);
	}

	/**
	 * A dropped item can't receive {@link StatusEffects#GLOWING} — it isn't a
	 * {@link LivingEntity} — so it gets the underlying render-only glow flag
	 * directly instead. Unlike the timed status effect, that flag has no
	 * built-in expiry; {@link #clearPreviousGlow} is what actually clears it
	 * when a re-tag moves on to a different target.
	 */
	private static void applyGlow(Entity target) {
		if (target instanceof LivingEntity living) {
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GLOW_TICKS, 0));
		} else {
			target.setGlowing(true);
		}
	}

	private static String compassLabel(double bearingDeg) {
		double normalized = bearingDeg < 0 ? bearingDeg + 360.0 : bearingDeg;
		int index = (int) Math.round(normalized / 45.0) % COMPASS_POINTS.length;
		return COMPASS_POINTS[index];
	}

	// ---- tooltip -----------------------------------------------------------

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		TrackingChipTarget target = stack.get(P7DataComponents.TRACKING_CHIP_TARGET);
		if (target == null) {
			tooltip.add(Text.translatable("item.program7.tracking_chip.tooltip.no_target").formatted(Formatting.GRAY));
		} else {
			tooltip.add(Text.translatable("item.program7.tracking_chip.tooltip.locked").formatted(Formatting.GRAY));
		}
	}
}
