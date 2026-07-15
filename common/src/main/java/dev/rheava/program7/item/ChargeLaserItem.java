package dev.rheava.program7.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import dev.rheava.program7.Program7;
import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.block.CargoBlock;
import dev.rheava.program7.registry.P7DamageTypes;
import dev.rheava.program7.registry.P7DataComponents;
import dev.rheava.program7.registry.P7Items;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
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
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * The charge laser — a long-range multitool, not a boss-killer. It shreds
 * light/small drones, is a genuinely bad, battery-wasting trade against
 * armored vehicles (see {@link dev.rheava.program7.entity.ArmorProfile}'s
 * {@code ENERGY} damage class), drills faraway blocks at a rate respecting
 * their hardness, and is an ignition source: it lights mobs and flammables
 * on fire, primes TNT, and cooks off the mod's ammo/cargo blocks and a
 * downed light drone's onboard magazine.
 *
 * <p>Three juggled resources, all carried on the stack as a {@link
 * ChargeLaserState} data component:
 * <ul>
 *   <li><b>Battery</b> — consumes a {@code power_bank} item on sneak +
 *       right-click reload; ~6s of fire per cell. Surfaced on the item's
 *       durability bar via {@link #getItemBarStep}/{@link #getItemBarColor}.</li>
 *   <li><b>Heat</b> — builds while firing, ~20s of continuous fire forces an
 *       overheat lockout until it vents back down. Surfaced via the client
 *       HUD heat bar ({@code ChargeLaserHud}), not the item bar.</li>
 *   <li><b>Lens wear</b> — cumulative firing ticks, wears far slower than a
 *       battery lasts; a spent lens refuses to fire until repaired with
 *       amethyst shards in the crafting grid ({@code LensRepairRecipe}).</li>
 * </ul>
 *
 * <p>Fired the bow/trident way: {@link #use} starts the use action, {@link
 * #usageTick} runs every tick while held down and does the actual raycast +
 * effects, {@link #onStoppedUsing} handles an early release.
 */
public class ChargeLaserItem extends Item {
	/** Beam range in blocks — a "ranged mining multitool," not a sniper rifle. */
	public static final double RANGE = 30.0;

	/** ~6 seconds of fire (120 ticks) per loaded power bank. */
	public static final int BATTERY_CAPACITY_TICKS = 120;

	/** ~20 seconds of continuous fire (400 ticks) before overheat locks the trigger. */
	public static final int HEAT_MAX = 400;
	private static final int HEAT_PER_FIRE_TICK = 1;
	private static final int HEAT_VENT_PER_IDLE_TICK = 2;
	/** Once overheated, heat must vent back below this before firing resumes. */
	private static final int OVERHEAT_CLEAR_HEAT = (int) (HEAT_MAX * 0.35f);

	/** Cumulative firing ticks the amethyst lens tolerates before it needs repair — "lasts many batteries." */
	public static final int LENS_MAX_TICKS = 24000;
	private static final float LENS_WARN_FRACTION = 0.2f;
	private static final float LENS_CRITICAL_FRACTION = 0.05f;

	/** Entity damage is applied every 4th fired tick — a burst cadence, not per-tick chip damage. */
	private static final int DAMAGE_INTERVAL_TICKS = 4;
	private static final float BASE_DAMAGE_PER_APPLICATION = 2.0f;
	private static final int FIRE_TICKS_ON_HIT = 60;

	/** Block-mining pacing: roughly hardness * this many ticks, clamped, mirroring vanilla-ish break speed but at range. */
	private static final float MINE_TICKS_PER_HARDNESS = 24.0f;
	private static final int MINE_BASE_TICKS = 6;
	private static final int MINE_MIN_TICKS = 6;
	private static final int MINE_MAX_TICKS = 200;

	/** Every this-many ticks while dwelling on a flammable-ish block, try to lay area-denial fire on top of it. */
	private static final int IGNITE_INTERVAL_TICKS = 30;

	private static final double BEAM_SEGMENT_SPACING = 0.75;
	private static final int PARTICLE_INTERVAL_TICKS = 2;
	private static final int BEAM_LOOP_SOUND_INTERVAL_TICKS = 10;
	/** Bow-level attention: noticeably louder than nothing, noticeably quieter than the mod's future firearms. */
	private static final float NOISE_LOUDNESS = 0.5f;
	private static final int NOISE_INTERVAL_TICKS = 20;

	/** Transient per-player "what block am I drilling and how far along" — never persisted, cleared on release/retarget. */
	private static final Map<UUID, MiningTarget> MINING = new HashMap<>();

	private record MiningTarget(BlockPos pos, int progress) {
	}

	public ChargeLaserItem(Settings settings) {
		super(settings.component(P7DataComponents.CHARGE_LASER_STATE, ChargeLaserState.DEFAULT));
	}

	public static ChargeLaserState getState(ItemStack stack) {
		return stack.getOrDefault(P7DataComponents.CHARGE_LASER_STATE, ChargeLaserState.DEFAULT);
	}

	private static void setState(ItemStack stack, ChargeLaserState state) {
		stack.set(P7DataComponents.CHARGE_LASER_STATE, state);
	}

	// ---- use / reload ------------------------------------------------------

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		ChargeLaserState laserState = getState(stack);

		if (user.isSneaking()) {
			return this.reload(world, user, stack, laserState);
		}

		if (laserState.lensWear() >= LENS_MAX_TICKS) {
			this.dryClick(world, user, P7Sounds.LASER_LENS_WARN.get());
			return TypedActionResult.fail(stack);
		}
		if (laserState.overheated() || laserState.batteryTicks() <= 0) {
			this.dryClick(world, user, laserState.overheated() ? P7Sounds.LASER_OVERHEAT.get() : P7Sounds.WEAPON_DRY_FIRE.get());
			return TypedActionResult.fail(stack);
		}

		if (!world.isClient) {
			world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.LASER_SPINUP.get(),
					SoundCategory.PLAYERS, 0.8f, 0.95f + world.getRandom().nextFloat() * 0.1f);
		}
		user.setCurrentHand(hand);
		return TypedActionResult.consume(stack);
	}

	private TypedActionResult<ItemStack> reload(World world, PlayerEntity user, ItemStack stack, ChargeLaserState laserState) {
		if (laserState.batteryTicks() >= BATTERY_CAPACITY_TICKS) {
			return TypedActionResult.pass(stack);
		}
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}
		ItemStack bank = findPowerBank(user);
		if (bank.isEmpty() && !user.getAbilities().creativeMode) {
			this.dryClick(world, user, P7Sounds.WEAPON_DRY_FIRE.get());
			return TypedActionResult.fail(stack);
		}
		if (!user.getAbilities().creativeMode) {
			bank.decrement(1);
		}
		setState(stack, laserState.withBattery(BATTERY_CAPACITY_TICKS));
		world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.LASER_RELOAD.get(),
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

	private void dryClick(World world, PlayerEntity user, SoundEvent sound) {
		if (!world.isClient) {
			world.playSound(null, user.getX(), user.getY(), user.getZ(), sound, SoundCategory.PLAYERS, 0.6f, 1.0f);
		}
	}

	// ---- firing loop --------------------------------------------------------

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (world.isClient || !(user instanceof PlayerEntity player)) {
			return;
		}
		ChargeLaserState laserState = getState(stack);
		if (laserState.overheated() || laserState.batteryTicks() <= 0 || laserState.lensWear() >= LENS_MAX_TICKS) {
			player.stopUsingItem();
			return;
		}
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}

		int elapsed = this.getMaxUseTime(stack) - remainingUseTicks;

		Vec3d start = player.getEyePos();
		Vec3d look = player.getRotationVec(1.0f);
		Vec3d rangeEnd = start.add(look.multiply(RANGE));

		// Raycast the block world first (the beam passes through fluids), then
		// look for an entity in front of whatever block stopped it — so a wall
		// blocks the beam and you can't tag a drone hiding behind one. This is
		// the repo's proven world.raycast idiom (see HitscanImpact);
		// ProjectileUtil.getCollision raycasts along the entity's *velocity*,
		// which is ~zero for a standing player, so it never registers a hit.
		BlockHitResult blockHit = world.raycast(new RaycastContext(start, rangeEnd,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
		Vec3d beamEnd = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getPos();
		Box searchBox = player.getBoundingBox().stretch(look.multiply(RANGE)).expand(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityCollision(world, player, start, beamEnd, searchBox,
				candidate -> candidate != player && candidate.canHit() && !candidate.isSpectator());

		HitResult hit = entityHit != null ? entityHit
				: (blockHit.getType() == HitResult.Type.MISS ? null : blockHit);
		Vec3d end = hit == null ? rangeEnd : hit.getPos();

		if (elapsed % PARTICLE_INTERVAL_TICKS == 0) {
			this.drawBeam(serverWorld, start, end);
		}
		if (elapsed % BEAM_LOOP_SOUND_INTERVAL_TICKS == 0) {
			ProgramAcoustics.emit(serverWorld, player.getPos(), P7Sounds.LASER_BEAM_LOOP.get(), SoundCategory.PLAYERS, 0.5f, 1.0f);
		}
		if (elapsed % NOISE_INTERVAL_TICKS == 0) {
			ProgramAcoustics.reportNoise(serverWorld, player.getX(), player.getY(), player.getZ(), NOISE_LOUDNESS);
		}

		if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
			this.clearMining(serverWorld, player);
			this.applyEntityHit(serverWorld, player, target, elapsed);
		} else if (hit instanceof BlockHitResult blockHitResult) {
			this.applyBlockHit(serverWorld, player, blockHitResult, elapsed);
		} else {
			this.clearMining(serverWorld, player);
		}

		// Consume charge / build heat / wear the lens — once per fired tick,
		// regardless of what the beam actually hit.
		int newBattery = laserState.batteryTicks() - 1;
		int newHeat = Math.min(HEAT_MAX, laserState.heatTicks() + HEAT_PER_FIRE_TICK);
		boolean nowOverheated = newHeat >= HEAT_MAX;
		int newLensWear = Math.min(LENS_MAX_TICKS, laserState.lensWear() + 1);
		setState(stack, new ChargeLaserState(newBattery, newHeat, nowOverheated, newLensWear));

		if (nowOverheated && !laserState.overheated()) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), P7Sounds.LASER_OVERHEAT.get(),
					SoundCategory.PLAYERS, 1.0f, 1.0f);
			serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, end.x, end.y, end.z, 6, 0.2, 0.2, 0.2, 0.02);
			this.clearMining(serverWorld, player);
			player.stopUsingItem();
			return;
		}
		if (newBattery <= 0) {
			this.clearMining(serverWorld, player);
			player.stopUsingItem();
			return;
		}
		if (crossedLensThreshold(laserState.lensWear(), newLensWear, LENS_WARN_FRACTION)
				|| crossedLensThreshold(laserState.lensWear(), newLensWear, LENS_CRITICAL_FRACTION)) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), P7Sounds.LASER_LENS_WARN.get(),
					SoundCategory.PLAYERS, 0.8f, 1.0f);
		}
	}

	private static boolean crossedLensThreshold(int oldWear, int newWear, float remainingFraction) {
		int threshold = LENS_MAX_TICKS - (int) (LENS_MAX_TICKS * remainingFraction);
		return oldWear < threshold && newWear >= threshold;
	}

	private void applyEntityHit(ServerWorld world, PlayerEntity player, LivingEntity target, int elapsed) {
		target.setFireTicks(Math.max(target.getFireTicks(), FIRE_TICKS_ON_HIT));
		if (elapsed % DAMAGE_INTERVAL_TICKS != 0) {
			return;
		}
		DamageSource source = world.getDamageSources().create(P7DamageTypes.LASER, player);
		target.damage(source, BASE_DAMAGE_PER_APPLICATION);
	}

	private void applyBlockHit(ServerWorld world, PlayerEntity player, BlockHitResult blockHit, int elapsed) {
		BlockPos pos = blockHit.getBlockPos();
		BlockState state = world.getBlockState(pos);

		if (state.getBlock() instanceof TntBlock) {
			this.clearMining(world, player);
			// Belt-and-suspenders: remove the block ourselves before priming
			// so there's no risk of a duplicate/ghost TNT block if primeTnt
			// doesn't already do so for a caller that isn't the block's own
			// neighbor-update path.
			world.removeBlock(pos, false);
			TntBlock.primeTnt(world, pos, player);
			return;
		}
		if (state.getBlock() instanceof CargoBlock cargoBlock) {
			this.clearMining(world, player);
			cargoBlock.igniteExternally(world, pos);
			return;
		}

		if (elapsed % IGNITE_INTERVAL_TICKS == 0 && canIgnite(world)) {
			BlockPos above = pos.up();
			BlockState aboveState = world.getBlockState(above);
			if (aboveState.isAir() && state.isSolidBlock(world, pos) && state.isBurnable()) {
				world.setBlockState(above, Blocks.FIRE.getDefaultState());
				world.spawnParticles(ParticleTypes.FLAME, above.getX() + 0.5, above.getY() + 0.2, above.getZ() + 0.5,
						4, 0.2, 0.05, 0.2, 0.01);
			}
		}

		this.tickMining(world, player, pos, state);
	}

	private static boolean canIgnite(ServerWorld world) {
		return Program7.CONFIG.terrainDestruction && world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
	}

	// ---- ranged mining --------------------------------------------------------

	private void tickMining(ServerWorld world, PlayerEntity player, BlockPos pos, BlockState state) {
		int ticksToBreak = computeMineTicks(world, pos, state);
		UUID id = player.getUuid();
		if (ticksToBreak < 0) {
			this.clearMining(world, player);
			return;
		}
		MiningTarget current = MINING.get(id);
		int progress;
		if (current != null && current.pos().equals(pos)) {
			progress = current.progress() + 1;
		} else {
			if (current != null) {
				world.setBlockBreakingInfo(player.getId(), current.pos(), -1);
			}
			progress = 1;
		}
		if (progress >= ticksToBreak) {
			world.setBlockBreakingInfo(player.getId(), pos, -1);
			world.breakBlock(pos, true, player);
			MINING.remove(id);
		} else {
			world.setBlockBreakingInfo(player.getId(), pos, MathHelper.clamp(progress * 10 / ticksToBreak, 0, 9));
			MINING.put(id, new MiningTarget(pos, progress));
		}
	}

	private void clearMining(ServerWorld world, PlayerEntity player) {
		MiningTarget current = MINING.remove(player.getUuid());
		if (current != null) {
			world.setBlockBreakingInfo(player.getId(), current.pos(), -1);
		}
	}

	private static int computeMineTicks(World world, BlockPos pos, BlockState state) {
		if (state.isAir()) {
			return -1;
		}
		float hardness = state.getHardness(world, pos);
		if (hardness < 0.0f) {
			return -1;
		}
		int ticks = Math.round(hardness * MINE_TICKS_PER_HARDNESS) + MINE_BASE_TICKS;
		return MathHelper.clamp(ticks, MINE_MIN_TICKS, MINE_MAX_TICKS);
	}

	// ---- vfx ------------------------------------------------------------------

	private void drawBeam(ServerWorld world, Vec3d start, Vec3d end) {
		double length = start.distanceTo(end);
		int segments = Math.max(1, (int) Math.round(length / BEAM_SEGMENT_SPACING));
		DustParticleEffect beamColor = new DustParticleEffect(new Vector3f(0.65f, 0.4f, 0.9f), 1.1f);
		for (int i = 0; i <= segments; i++) {
			Vec3d point = start.lerp(end, (double) i / segments);
			world.spawnParticles(beamColor, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.0);
		}
		world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 5, 0.15, 0.15, 0.15, 0.03);
		world.spawnParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0.01);
	}

	// ---- release / idle venting -------------------------------------------

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (!world.isClient && user instanceof PlayerEntity player && world instanceof ServerWorld serverWorld) {
			this.clearMining(serverWorld, player);
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (world.isClient || !(entity instanceof LivingEntity living)) {
			return;
		}
		boolean firingThis = living.isUsingItem() && living.getActiveItem() == stack;
		if (firingThis) {
			// usageTick already advances heat/charge/lens this tick.
			return;
		}
		ChargeLaserState laserState = getState(stack);
		if (laserState.heatTicks() <= 0 && !laserState.overheated()) {
			return;
		}
		int vented = Math.max(0, laserState.heatTicks() - HEAT_VENT_PER_IDLE_TICK);
		boolean stillOverheated = laserState.overheated() && vented > OVERHEAT_CLEAR_HEAT;
		if (vented != laserState.heatTicks() || stillOverheated != laserState.overheated()) {
			setState(stack, laserState.withHeat(vented, stillOverheated));
		}
	}

	// ---- use-action plumbing -----------------------------------------------

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 72000;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BOW;
	}

	// ---- durability bar = battery charge -----------------------------------

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return getState(stack).batteryTicks() < BATTERY_CAPACITY_TICKS;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		float fraction = getState(stack).batteryTicks() / (float) BATTERY_CAPACITY_TICKS;
		return MathHelper.clamp(Math.round(fraction * 13.0f), 0, 13);
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		float fraction = getState(stack).batteryTicks() / (float) BATTERY_CAPACITY_TICKS;
		return MathHelper.hsvToRgb(fraction / 3.0f, 1.0f, 1.0f);
	}

	// ---- tooltip -------------------------------------------------------------

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		ChargeLaserState laserState = getState(stack);
		float lensFraction = 1.0f - laserState.lensWear() / (float) LENS_MAX_TICKS;
		tooltip.add(Text.translatable("item.program7.charge_laser.tooltip.battery",
				laserState.batteryTicks() / 20).formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.program7.charge_laser.tooltip.lens",
				Math.round(lensFraction * 100)).formatted(lensFraction <= LENS_CRITICAL_FRACTION ? Formatting.RED
						: lensFraction <= LENS_WARN_FRACTION ? Formatting.GOLD : Formatting.GRAY));
		if (laserState.overheated()) {
			tooltip.add(Text.translatable("item.program7.charge_laser.tooltip.overheated").formatted(Formatting.RED));
		}
		if (lensFraction <= LENS_CRITICAL_FRACTION) {
			tooltip.add(Text.translatable("item.program7.charge_laser.tooltip.lens_critical").formatted(Formatting.RED));
		} else if (lensFraction <= LENS_WARN_FRACTION) {
			tooltip.add(Text.translatable("item.program7.charge_laser.tooltip.lens_warn").formatted(Formatting.GOLD));
		}
	}
}
