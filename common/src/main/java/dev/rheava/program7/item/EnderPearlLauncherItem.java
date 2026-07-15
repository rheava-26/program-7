package dev.rheava.program7.item;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.entity.ai.BallisticSolver;
import dev.rheava.program7.registry.P7DataComponents;
import dev.rheava.program7.registry.P7Items;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
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
 * The ender pearl launcher — a battery-powered, charge-up gun that lobs a
 * vanilla {@link EnderPearlEntity} a very long distance. Because a thrown
 * ender pearl already teleports its thrower on impact (vanilla handles
 * that entirely — this class never touches teleport logic), a well-aimed
 * long shot is a precision, long-range blink across the map instead of the
 * usual short toss.
 *
 * <p>Two things happen while the trigger is held ({@link #use} starts it,
 * {@link #usageTick} drives it every tick, mirroring {@link ChargeLaserItem}'s
 * bow-style use action):
 * <ul>
 *   <li><b>Charge builds</b> over {@link #CHARGE_TICKS_TO_FULL} ticks, capped
 *       at full — releasing below {@link #MIN_CHARGE_FRACTION} is a soft
 *       click, nothing launches. At/above that, {@link #onStoppedUsing}
 *       spawns the pearl with a look-vector velocity scaled by charge.</li>
 *   <li><b>A laser sight</b> forward-simulates the pearl's own ballistic arc
 *       (gravity + the {@link BallisticSolver#DRAG drag every {@code
 *       ThrownEntity} shares}) at the <em>current</em> charge speed every
 *       couple of ticks and draws a particle beam + landing marker, so the
 *       player can see exactly where a release right now would land — the
 *       aiming aid that makes a warp shot usable instead of a guess.</li>
 * </ul>
 *
 * <p>Battery is a single juggled resource (see {@link EnderPearlLauncherState}):
 * sneak + right-click reloads from a {@code power_bank} in the inventory
 * (not in creative), each shot spends a flat {@link #SHOT_COST} regardless of
 * how hard it was charged, and the remaining fraction drives the item's real
 * durability bar exactly like {@code ChargeLaserItem}'s battery does.
 */
public class EnderPearlLauncherItem extends Item {
	/** A full power bank's worth of charge — {@link #SHOT_COST} makes this ~4 shots per reload. */
	public static final int BATTERY_CAPACITY = 400;
	/** Flat cost per shot, independent of how charged it was — "each shot costs a chunk of charge." */
	public static final int SHOT_COST = 100;

	/** Ticks of holding the trigger to reach full charge — within the "~30-45 ticks" the brief called for. */
	public static final int CHARGE_TICKS_TO_FULL = 40;
	/** Below this charge fraction, releasing is a soft click — no shot, no battery spent. */
	private static final float MIN_CHARGE_FRACTION = 0.2f;
	/** Speed at charge fraction 0 — the sight still shows *something* the instant the trigger is pulled, just a short lob at your feet. */
	private static final float MIN_PREVIEW_SPEED = 0.3f;
	/** Speed at full charge — "super long distance," per the brief's ~3.5 blocks/tick target. */
	public static final float MAX_LAUNCH_SPEED = 3.5f;

	/**
	 * Gravity used for the landing-prediction sim, matching {@code
	 * ThrownEntity}'s own un-overridden default (see {@code AbstractShellEntity}'s
	 * and {@code GlowStickEntity}'s doc comments, which both cite "~0.03" as
	 * that baseline) — vanilla {@link EnderPearlEntity} never overrides {@code
	 * getGravity()}, so this is what it actually falls under.
	 */
	private static final double PREDICTION_GRAVITY = 0.03;
	/** Safety cap on the forward sim so aiming into open sky can't spin it forever. ~280 blocks of horizontal travel at max charge. */
	private static final int MAX_PREDICTION_TICKS = 200;
	/** Only every Nth simulated point gets a beam particle — a full-resolution line is unnecessary and needlessly expensive. */
	private static final int PATH_PARTICLE_STRIDE = 3;

	private static final int PARTICLE_INTERVAL_TICKS = 2;
	private static final int CHARGE_SOUND_INTERVAL_TICKS = 8;
	private static final int NOISE_INTERVAL_TICKS = 20;
	private static final float CHARGE_NOISE_LOUDNESS = 0.5f;
	private static final float LAUNCH_NOISE_LOUDNESS = 0.8f;
	/** Short kick-back cooldown after a launch — not a balance lever (battery already gates spam), just gun feel. */
	private static final int LAUNCH_COOLDOWN_TICKS = 15;

	public EnderPearlLauncherItem(Settings settings) {
		super(settings.component(P7DataComponents.ENDER_PEARL_LAUNCHER_STATE, EnderPearlLauncherState.DEFAULT));
	}

	public static EnderPearlLauncherState getState(ItemStack stack) {
		return stack.getOrDefault(P7DataComponents.ENDER_PEARL_LAUNCHER_STATE, EnderPearlLauncherState.DEFAULT);
	}

	private static void setState(ItemStack stack, EnderPearlLauncherState state) {
		stack.set(P7DataComponents.ENDER_PEARL_LAUNCHER_STATE, state);
	}

	// ---- use / reload ------------------------------------------------------

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		EnderPearlLauncherState state = getState(stack);

		if (user.isSneaking()) {
			return this.reload(world, user, stack, state);
		}
		if (user.getItemCooldownManager().isCoolingDown(this)) {
			return TypedActionResult.fail(stack);
		}
		if (state.batteryUnits() < SHOT_COST) {
			this.dryClick(world, user);
			return TypedActionResult.fail(stack);
		}

		user.setCurrentHand(hand);
		return TypedActionResult.consume(stack);
	}

	private TypedActionResult<ItemStack> reload(World world, PlayerEntity user, ItemStack stack, EnderPearlLauncherState state) {
		if (state.batteryUnits() >= BATTERY_CAPACITY) {
			return TypedActionResult.pass(stack);
		}
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}
		ItemStack bank = findPowerBank(user);
		if (bank.isEmpty() && !user.getAbilities().creativeMode) {
			this.dryClick(world, user);
			return TypedActionResult.fail(stack);
		}
		if (!user.getAbilities().creativeMode) {
			bank.decrement(1);
		}
		setState(stack, state.withBattery(BATTERY_CAPACITY));
		world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.LAUNCHER_RELOAD.get(),
				SoundCategory.PLAYERS, 1.0f, 0.95f + world.getRandom().nextFloat() * 0.1f);
		return TypedActionResult.success(stack);
	}

	private static ItemStack findPowerBank(PlayerEntity user) {
		for (int i = 0; i < user.getInventory().size(); i++) {
			ItemStack candidate = user.getInventory().getStack(i);
			if (candidate.isOf(P7Items.POWER_BANK.get())) {
				return candidate;
			}
		}
		return ItemStack.EMPTY;
	}

	private void dryClick(World world, PlayerEntity user) {
		if (!world.isClient) {
			world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.WEAPON_DRY_FIRE.get(),
					SoundCategory.PLAYERS, 0.6f, 1.0f);
		}
	}

	// ---- charge-up loop ------------------------------------------------------

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (world.isClient || !(user instanceof PlayerEntity player) || !(world instanceof ServerWorld serverWorld)) {
			return;
		}
		EnderPearlLauncherState state = getState(stack);
		if (state.batteryUnits() < SHOT_COST) {
			// Drained mid-charge (shouldn't normally happen single-player, but be defensive) — abort quietly.
			player.stopUsingItem();
			return;
		}

		int elapsed = this.getMaxUseTime(stack, user) - remainingUseTicks;
		float chargeFraction = computeChargeFraction(elapsed);
		float previewSpeed = speedForCharge(chargeFraction);

		if (elapsed % PARTICLE_INTERVAL_TICKS == 0) {
			Vec3d muzzle = player.getEyePos();
			Vec3d look = player.getRotationVec(1.0f);
			Vec3d velocity = look.multiply(previewSpeed);
			PredictedLanding landing = this.predictLanding(serverWorld, player, muzzle, velocity);
			this.drawLaserSight(serverWorld, landing);
		}
		if (elapsed % CHARGE_SOUND_INTERVAL_TICKS == 0) {
			// Pitch climbs with charge — the "rising whine" cue.
			float pitch = 0.7f + chargeFraction * 0.9f;
			ProgramAcoustics.emit(serverWorld, player.getPos(), P7Sounds.LAUNCHER_CHARGE.get(),
					SoundCategory.PLAYERS, 0.6f, pitch);
		}
		if (elapsed % NOISE_INTERVAL_TICKS == 0) {
			ProgramAcoustics.reportNoise(serverWorld, player.getX(), player.getY(), player.getZ(), CHARGE_NOISE_LOUDNESS);
		}
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (world.isClient || !(user instanceof PlayerEntity player) || !(world instanceof ServerWorld serverWorld)) {
			return;
		}
		EnderPearlLauncherState state = getState(stack);
		int elapsed = this.getMaxUseTime(stack, user) - remainingUseTicks;
		float chargeFraction = computeChargeFraction(elapsed);

		if (chargeFraction < MIN_CHARGE_FRACTION || state.batteryUnits() < SHOT_COST) {
			// Released too early, or drained mid-charge — a soft click, no shot, no battery spent.
			this.dryClick(world, player);
			return;
		}

		float speed = speedForCharge(chargeFraction);
		Vec3d look = player.getRotationVec(1.0f);

		EnderPearlEntity pearl = new EnderPearlEntity(serverWorld, player);
		pearl.setVelocity(look.x * speed, look.y * speed, look.z * speed);
		serverWorld.spawnEntity(pearl);

		setState(stack, state.withBattery(state.batteryUnits() - SHOT_COST));
		player.getItemCooldownManager().set(this, LAUNCH_COOLDOWN_TICKS);

		ProgramAcoustics.emit(serverWorld, player.getPos(), P7Sounds.LAUNCHER_LAUNCH.get(),
				SoundCategory.PLAYERS, 1.0f, 1.0f);
		ProgramAcoustics.reportNoise(serverWorld, player.getX(), player.getY(), player.getZ(), LAUNCH_NOISE_LOUDNESS);
		// The teleport itself — sound, particles, fall-damage negation-or-not — is entirely vanilla
		// EnderPearlEntity#onCollision behavior. Reused as-is, not reimplemented here.
	}

	private static float computeChargeFraction(int elapsed) {
		return MathHelper.clamp(elapsed / (float) CHARGE_TICKS_TO_FULL, 0.0f, 1.0f);
	}

	/** Same curve drives both the live laser-sight preview and the actual launch speed at release — what you see is what you get. */
	private static float speedForCharge(float chargeFraction) {
		return MathHelper.lerp(chargeFraction, MIN_PREVIEW_SPEED, MAX_LAUNCH_SPEED);
	}

	// ---- laser sight: forward-simulated landing prediction --------------------

	private record PredictedLanding(List<Vec3d> path, Vec3d impact) {
	}

	/**
	 * Steps the pearl's own physics (drag then gravity, matching {@code
	 * ThrownEntity}'s integration order — see {@link BallisticSolver}'s doc)
	 * forward from {@code start} at {@code velocity}, one simulated tick at a
	 * time, until it would enter a non-air block. That coarse per-tick check
	 * is deliberately cheap (no collision-shape math) since it runs up to
	 * {@link #MAX_PREDICTION_TICKS} times a call; the final short segment
	 * gets one precise {@link World#raycast} to pin down the exact impact
	 * point, the same idiom {@link ChargeLaserItem#usageTick} uses for its
	 * beam. A single entity-collision check across the whole resolved path
	 * (not one per simulated tick) then lets a mob standing in the arc
	 * pre-empt the block landing, without paying for a box search every tick.
	 */
	private PredictedLanding predictLanding(ServerWorld world, PlayerEntity shooter, Vec3d start, Vec3d velocity) {
		List<Vec3d> path = new ArrayList<>();
		Vec3d pos = start;
		Vec3d vel = velocity;
		path.add(pos);

		Vec3d blockImpact = null;
		for (int tick = 0; tick < MAX_PREDICTION_TICKS; tick++) {
			Vec3d next = pos.add(vel);
			if (!world.getBlockState(BlockPos.ofFloored(next)).isAir()) {
				BlockHitResult refined = world.raycast(new RaycastContext(pos, next,
						RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, shooter));
				blockImpact = refined.getType() == HitResult.Type.MISS ? next : refined.getPos();
				path.add(blockImpact);
				break;
			}
			path.add(next);
			pos = next;
			vel = new Vec3d(vel.x * BallisticSolver.DRAG, vel.y * BallisticSolver.DRAG - PREDICTION_GRAVITY,
					vel.z * BallisticSolver.DRAG);
		}
		Vec3d endpoint = blockImpact != null ? blockImpact : pos;

		Box searchBox = new Box(start, endpoint).expand(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityCollision(world, shooter, start, endpoint, searchBox,
				candidate -> candidate != shooter && candidate.canHit());
		Vec3d impact = entityHit != null ? entityHit.getPos() : endpoint;
		return new PredictedLanding(path, impact);
	}

	private void drawLaserSight(ServerWorld world, PredictedLanding landing) {
		DustParticleEffect beamColor = new DustParticleEffect(new Vector3f(0.55f, 0.15f, 0.85f), 0.9f);
		List<Vec3d> path = landing.path();
		for (int i = 0; i < path.size(); i += PATH_PARTICLE_STRIDE) {
			Vec3d point = path.get(i);
			world.spawnParticles(beamColor, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		Vec3d impact = landing.impact();
		world.spawnParticles(ParticleTypes.END_ROD, impact.x, impact.y + 0.15, impact.z, 8, 0.2, 0.1, 0.2, 0.01);
	}

	// ---- use-action plumbing -----------------------------------------------

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BOW;
	}

	// ---- durability bar = battery charge -----------------------------------

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return getState(stack).batteryUnits() < BATTERY_CAPACITY;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		float fraction = getState(stack).batteryUnits() / (float) BATTERY_CAPACITY;
		return MathHelper.clamp(Math.round(fraction * 13.0f), 0, 13);
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		float fraction = getState(stack).batteryUnits() / (float) BATTERY_CAPACITY;
		return MathHelper.hsvToRgb(fraction / 3.0f, 1.0f, 1.0f);
	}

	// ---- tooltip -------------------------------------------------------------

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		EnderPearlLauncherState state = getState(stack);
		int shots = state.batteryUnits() / SHOT_COST;
		int batteryPercent = Math.round(state.batteryUnits() * 100.0f / BATTERY_CAPACITY);
		tooltip.add(Text.translatable("item.program7.ender_pearl_launcher.tooltip.battery", batteryPercent)
				.formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.program7.ender_pearl_launcher.tooltip.shots", shots)
				.formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.program7.ender_pearl_launcher.tooltip.hint")
				.formatted(Formatting.DARK_GRAY));
	}
}
