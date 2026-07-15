package dev.rheava.program7.item;

import java.util.List;

import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.entity.ai.HitscanImpact;
import dev.rheava.program7.entity.ai.RoundClass;
import dev.rheava.program7.registry.P7DamageTypes;
import dev.rheava.program7.registry.P7DataComponents;
import dev.rheava.program7.registry.P7Items;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
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
 * The Salvaged Firearm — the mod's bread-and-butter player weapon: a
 * semi-auto hitscan rifle assembled from recovered Program parts. One right
 * click fires exactly one round: a raycast from the player's eyes, walls
 * stop it dead, and it deals roughly Power II-III bow damage at a longer
 * range than the charge laser reaches.
 *
 * <p>Two costs keep it from just being a strictly-better bow:
 * <ul>
 *   <li><b>Loud.</b> Every shot reports a strong {@link
 *       ProgramAcoustics#reportNoise} pulse — this is the loudest cue in the
 *       player arsenal so far, a real tradeoff against the network of
 *       drones listening for exactly this.</li>
 *   <li><b>Ammo-hungry.</b> Fed from an internal {@value #MAGAZINE_CAPACITY}
 *       -round magazine, shown on the item's durability bar, reloaded by
 *       sneak + right-click consuming a {@code magazine} salvage item. A dry
 *       trigger pull just clicks.</li>
 * </ul>
 *
 * <p>Routed through {@link
 * dev.rheava.program7.entity.ArmorProfile.DamageClass#BALLISTIC} (see {@code
 * P7DamageTypes#BULLET} and {@code ProgramDroneEntity#classify}), the mirror
 * image of the charge laser's {@code ENERGY} routing: armored drones resist
 * this rifle outright — that's the anti-armor lane's job, not this gun's —
 * while light/unarmored fliers eat it in full. Fired the tool way (a single
 * {@link #use}, not a held charge-up), same idiom as every other hitscan
 * mount in the arsenal (see {@link HitscanImpact}).
 */
public class SalvagedRifleItem extends Item {
	/** Rounds held in the internal magazine, surfaced on the item's durability bar. */
	public static final int MAGAZINE_CAPACITY = 8;
	/** Hitscan range in blocks — reaches further than the charge laser's 30. */
	public static final double RANGE = 40.0;
	/** Roughly Power II-III bow damage per hit. */
	public static final float DAMAGE = 6.5f;
	/** Small per-shot cooldown so this reads as semi-auto, not a full-auto mash. */
	private static final int FIRE_COOLDOWN_TICKS = 8;
	/** Loudest tier in the player arsenal so far — guns attract the network. */
	private static final float NOISE_LOUDNESS = 1.0f;

	public SalvagedRifleItem(Settings settings) {
		super(settings.component(P7DataComponents.SALVAGED_RIFLE_STATE, SalvagedRifleState.DEFAULT));
	}

	public static int getRounds(ItemStack stack) {
		return stack.getOrDefault(P7DataComponents.SALVAGED_RIFLE_STATE, SalvagedRifleState.DEFAULT).rounds();
	}

	private static void setRounds(ItemStack stack, int rounds) {
		stack.set(P7DataComponents.SALVAGED_RIFLE_STATE, new SalvagedRifleState(rounds));
	}

	// ---- use / reload -------------------------------------------------------

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (user.isSneaking()) {
			return this.reload(world, user, stack);
		}

		int rounds = getRounds(stack);
		if (rounds <= 0) {
			this.dryClick(world, user);
			return TypedActionResult.fail(stack);
		}
		if (!(world instanceof ServerWorld serverWorld)) {
			// Client side: let the server-authoritative fire() below actually
			// resolve the shot; just don't double-consume a round here.
			return TypedActionResult.success(stack);
		}

		this.fire(serverWorld, user, stack, rounds);
		user.getItemCooldownManager().set(this, FIRE_COOLDOWN_TICKS);
		return TypedActionResult.success(stack);
	}

	private TypedActionResult<ItemStack> reload(World world, PlayerEntity user, ItemStack stack) {
		if (getRounds(stack) >= MAGAZINE_CAPACITY) {
			return TypedActionResult.pass(stack);
		}
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}
		ItemStack magazine = findMagazine(user);
		if (magazine.isEmpty() && !user.getAbilities().creativeMode) {
			this.dryClick(world, user);
			return TypedActionResult.fail(stack);
		}
		if (!user.getAbilities().creativeMode) {
			magazine.decrement(1);
		}
		setRounds(stack, MAGAZINE_CAPACITY);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), P7Sounds.RIFLE_RELOAD.get(),
				SoundCategory.PLAYERS, 1.0f, 0.95f + world.getRandom().nextFloat() * 0.1f);
		return TypedActionResult.success(stack);
	}

	private static ItemStack findMagazine(PlayerEntity user) {
		for (int i = 0; i < user.getInventory().size(); i++) {
			ItemStack candidate = user.getInventory().getStack(i);
			if (candidate.isOf(P7Items.MAGAZINE.get())) {
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

	// ---- firing ---------------------------------------------------------------

	private void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int rounds) {
		Vec3d start = player.getEyePos();
		Vec3d look = player.getRotationVec(1.0f);
		Vec3d rangeEnd = start.add(look.multiply(RANGE));

		// Repo's proven hitscan idiom (see HitscanImpact / ChargeLaserItem):
		// raycast the block world first so a wall genuinely stops the shot,
		// then look for an entity in front of whatever the block raycast
		// found. ProjectileUtil.getEntityCollision (NOT getCollision, which
		// rides an entity's near-zero velocity for a standing player) bounded
		// by the block hit is what keeps a drone hiding behind a wall safe.
		BlockHitResult blockHit = world.raycast(new RaycastContext(start, rangeEnd,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
		Vec3d boundedEnd = blockHit.getType() == HitResult.Type.MISS ? rangeEnd : blockHit.getPos();
		Box searchBox = player.getBoundingBox().stretch(look.multiply(RANGE)).expand(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityCollision(world, player, start, boundedEnd, searchBox,
				candidate -> candidate != player && candidate.canHit() && !candidate.isSpectator());

		HitResult hit = entityHit != null ? entityHit
				: (blockHit.getType() == HitResult.Type.MISS ? null : blockHit);
		Vec3d end = hit == null ? rangeEnd : hit.getPos();

		// Muzzle flash (a small bright spark burst, not the totem-scale FLASH
		// particle) + smoke puff + tracer + a falling shell casing, then the
		// loud report.
		HitscanImpact.drawTracer(world, start, end);
		world.spawnParticles(ParticleTypes.FIREWORK, start.x, start.y, start.z, 4, 0.08, 0.08, 0.08, 0.02);
		world.spawnParticles(ParticleTypes.SMOKE, start.x, start.y, start.z, 2, 0.05, 0.05, 0.05, 0.01);
		HitscanImpact.ejectCasing(world, start, world.getRandom());

		ProgramAcoustics.emit(world, player.getPos(), P7Sounds.RIFLE_SHOT.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);
		ProgramAcoustics.reportNoise(world, player.getX(), player.getY(), player.getZ(), NOISE_LOUDNESS);

		if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
			RegistryEntry<DamageType> bulletType = world.getRegistryManager()
					.getWrapperOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(P7DamageTypes.BULLET);
			DamageSource source = new DamageSource(bulletType, player);
			target.damage(source, DAMAGE);
		} else if (blockHit.getType() != HitResult.Type.MISS) {
			// No entity in the way: resolve where the round actually lands
			// against terrain (impact puff + chip damage), same as every
			// other hitscan mount in the arsenal.
			HitscanImpact.resolve(world, start, blockHit.getPos(), RANGE, player, RoundClass.MEDIUM);
		}

		setRounds(stack, rounds - 1);
	}

	// ---- durability bar = magazine rounds --------------------------------------

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return getRounds(stack) < MAGAZINE_CAPACITY;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		float fraction = getRounds(stack) / (float) MAGAZINE_CAPACITY;
		return MathHelper.clamp(Math.round(fraction * 13.0f), 0, 13);
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		float fraction = getRounds(stack) / (float) MAGAZINE_CAPACITY;
		return MathHelper.hsvToRgb(fraction / 3.0f, 1.0f, 1.0f);
	}

	// ---- tooltip ----------------------------------------------------------------

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		int rounds = getRounds(stack);
		tooltip.add(Text.translatable("item.program7.salvaged_rifle.tooltip.rounds", rounds, MAGAZINE_CAPACITY)
				.formatted(rounds <= 0 ? Formatting.RED : Formatting.GRAY));
	}
}
