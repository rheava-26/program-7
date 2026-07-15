package dev.rheava.program7.item;

import java.util.List;

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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
 * short raycast) to mark that instead. Either way the target's UUID and
 * current position are written onto the stack as a {@link
 * TrackingChipTarget} data component, and the target is made to glow for a
 * while so you can eyeball it immediately.
 *
 * <p><b>Read</b> — right-click the air with nothing nearby to tag: if a
 * target is stored, resolves it against the world's currently loaded
 * entities. Loaded: refresh the stored position, re-apply the glow, and
 * print a bearing + distance to the actionbar. Not loaded (out of range, in
 * an unloaded chunk, or dead and gone): print "signal lost" — the chip never
 * shows a target it can't currently see, only the direction/range as of the
 * last successful read.
 *
 * <p>Server-authoritative throughout: both branches bail out on the client
 * and let the server resolve everything, same as {@code ChargeLaserItem}'s
 * reload path.
 *
 * <p><b>Deferred:</b> no datapad-screen integration yet — the datapad's
 * radar (see {@code DatapadItem}/{@code DatapadScreen}) doesn't plot tracked
 * targets. A natural follow-up is surfacing the chip's stored target (or a
 * whole pocketful of them) as blips on that screen instead of/alongside the
 * actionbar readout here.
 */
public class TrackingChipItem extends Item {
	/** How far the "mark a dropped item" raycast reaches — a deliberate wrist flick, not a sniper mark. */
	private static final double ITEM_TAG_RANGE = 4.5;

	/** How long the glow lasts per tag/refresh — long enough to spot the target, short enough to need a re-read. */
	private static final int GLOW_TICKS = 20 * 30;

	private static final String[] COMPASS_POINTS =
			{"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	public TrackingChipItem(Settings settings) {
		super(settings);
	}

	// ---- tag: right-click on a living entity ---------------------------------

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		World world = user.getWorld();
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (entity == user) {
			// Tagging yourself is a no-op, not an error — let the interaction fall
			// through instead of consuming it.
			return ActionResult.PASS;
		}
		this.tag(stack, user, entity);
		return ActionResult.CONSUME;
	}

	// ---- tag a nearby dropped item, or read the stored target -----------------

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}
		if (!(world instanceof ServerWorld serverWorld)) {
			return TypedActionResult.pass(stack);
		}

		ItemEntity nearbyItem = this.raycastItemEntity(serverWorld, user);
		if (nearbyItem != null) {
			this.tag(stack, user, nearbyItem);
			return TypedActionResult.consume(stack);
		}

		this.read(stack, user, serverWorld);
		return TypedActionResult.consume(stack);
	}

	private ItemEntity raycastItemEntity(ServerWorld world, PlayerEntity user) {
		Vec3d start = user.getEyePos();
		Vec3d look = user.getRotationVec(1.0f);
		Vec3d end = start.add(look.multiply(ITEM_TAG_RANGE));
		Box searchBox = user.getBoundingBox().stretch(look.multiply(ITEM_TAG_RANGE)).expand(1.0);

		EntityHitResult hit = ProjectileUtil.getEntityCollision(world, user, start, end, searchBox,
				candidate -> candidate instanceof ItemEntity item && item.isAlive() && !item.getStack().isEmpty());
		return hit != null && hit.getEntity() instanceof ItemEntity item ? item : null;
	}

	// ---- shared tag/read logic --------------------------------------------

	private void tag(ItemStack stack, PlayerEntity user, Entity target) {
		World world = user.getWorld();
		stack.set(P7DataComponents.TRACKING_CHIP_TARGET,
				new TrackingChipTarget(target.getUuid(), target.getX(), target.getY(), target.getZ()));
		applyGlow(target);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.TRACKING_CHIP_PING.get(),
				SoundCategory.PLAYERS, 0.5f, 1.5f);
		user.sendMessage(Text.translatable("item.program7.tracking_chip.tagged", target.getDisplayName())
				.formatted(Formatting.AQUA), true);
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
			applyGlow(found);
			world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.TRACKING_CHIP_PING.get(),
					SoundCategory.PLAYERS, 0.4f, 1.7f);
		} else {
			user.sendMessage(Text.translatable("item.program7.tracking_chip.signal_lost").formatted(Formatting.RED), true);
			return;
		}

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
	 * built-in expiry; it clears on the next tag/read of a different target,
	 * or simply stops mattering once the item despawns. Documented
	 * simplification, not a bug: see the class doc's deferred-work note.
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
