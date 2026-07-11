package dev.rheava.program7.block;

import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.director.SupplyNetwork;
import dev.rheava.program7.entity.ReloadableWeapon;
import dev.rheava.program7.entity.ai.HitscanImpact;
import dev.rheava.program7.entity.ai.RoundClass;
import dev.rheava.program7.registry.P7BlockEntities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The block-entity brains of the bolted-down autogun turret (see
 * {@code AutogunTurretEntity} for the mob version this replaces). A block
 * can't run a {@code Goal}, so the target-scan, line-of-sight, and hitscan
 * firing logic that {@code GunAttackGoal} would normally provide is
 * reproduced here by hand, tuned to the same numbers the entity used
 * (range 20, fireInterval 6 ticks). Per the gun-feel pass, per-shot damage
 * (7.0, {@link RoundClass#LIGHT}) is kept below a mobile gun drone's of the
 * same caliber — a static turret trades punch for uptime.
 *
 * <p>New on top of the mob version: an overheat gate. Sustained fire builds
 * heat; crossing {@link #HEAT_MAX} shuts the gun down until it cools back
 * below {@link #HEAT_RESET} (hysteresis, so it doesn't immediately resume
 * and re-trip). While overheated it vents smoke and sounds off once so the
 * player can read "it's down — push now."
 *
 * <p>Also carries a finite {@link ReloadableWeapon} magazine: once
 * {@link #roundsRemaining} hits zero the gun goes quiet (a soft dry click
 * instead of a shot) until a logistics delivery calls {@link #loadRounds}.
 */
public class AutogunTurretBlockEntity extends BlockEntity implements ReloadableWeapon {
	/** Heat added to the gun for every shot fired. */
	private static final float HEAT_PER_SHOT = 8.0f;
	/** Heat level at which the gun locks out and starts venting. */
	private static final float HEAT_MAX = 100.0f;
	/** Heat must drop back to (at most) this before firing resumes — hysteresis. */
	private static final float HEAT_RESET = 20.0f;
	/** Heat bled off per tick whenever the gun didn't just fire. */
	private static final float COOL_PER_TICK = 0.6f;
	/** Ticks between shots while not overheated; matches the old entity's GunAttackGoal. */
	private static final int FIRE_INTERVAL = 6;
	/** Engagement range, matching the old entity's GunAttackGoal + approach alarm radius. */
	private static final double RANGE = 20.0;
	/** Gun-feel pass: 3.0->7.0, kept below a mobile gun drone's damage for the same LIGHT caliber. */
	private static final float DAMAGE = 7.0f;
	private static final RoundClass ROUND_CLASS = RoundClass.LIGHT;
	/** How often (in ticks) to puff vent smoke while overheated. */
	private static final int VENT_PARTICLE_INTERVAL = 10;
	/** Ready-magazine size for this fixed emplacement — see {@code docs/ARTILLERY_AND_INDIRECT_FIRE.md} §6a. */
	private static final int MAGAZINE_CAPACITY = 20;
	/** Minimum gap between "magazine's empty" clicks so a dry gun doesn't spam it every tick. */
	private static final int DRY_FIRE_CLICK_INTERVAL_TICKS = 40;
	/**
	 * Emplaced-gun resupply: a bolted-down turret is wired straight into the
	 * base's ammo supply (see {@code docs/ARTILLERY_AND_INDIRECT_FIRE.md} §4 —
	 * emplaced guns get more ammo and are fed from the depot), so instead of
	 * waiting on a logistics-drone courier the way the mobile weapons do, it
	 * pulls rounds directly from the nearest {@link SupplyNetwork} ammo depot
	 * on a slow timer. The base's ammo pool is still finite, so a turret run
	 * dry stays dry once the base is starved.
	 */
	private static final int RESUPPLY_INTERVAL_TICKS = 30;
	private static final int ROUNDS_PER_RESUPPLY = 4;
	private static final double RESUPPLY_RANGE = 80.0;

	private static final String NBT_HEAT = "Heat";
	private static final String NBT_OVERHEATED = "Overheated";
	private static final String NBT_FIRE_COOLDOWN = "FireCooldown";
	private static final String NBT_ROUNDS = "RoundsRemaining";

	private float heat = 0.0f;
	private boolean overheated = false;
	private int fireCooldown = 0;
	private int ventCooldown = 0;
	private int roundsRemaining = MAGAZINE_CAPACITY;
	private int dryFireCooldown = 0;
	private int resupplyCooldown = 0;
	/** Tracks the no-target -&gt; has-target transition so the loud opening cue plays once per engagement, not once per shot. */
	private boolean wasEngaging = false;

	public AutogunTurretBlockEntity(BlockPos pos, BlockState state) {
		super(P7BlockEntities.AUTOGUN_TURRET.get(), pos, state);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, AutogunTurretBlockEntity turret) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		turret.tick(serverWorld, pos);
	}

	private void tick(ServerWorld world, BlockPos pos) {
		if (this.overheated) {
			this.heat = Math.max(0.0f, this.heat - COOL_PER_TICK);
			if (this.heat <= HEAT_RESET) {
				this.overheated = false;
			}
			this.tickVenting(world, pos);
			this.markDirty();
			return;
		}

		// Emplaced gun: top the magazine back up from the base ammo depot.
		this.tickResupply(world, pos);

		PlayerEntity target = this.acquireTarget(world, pos);
		boolean firedThisTick = false;
		if (target != null) {
			if (!this.wasEngaging) {
				this.playOpeningFireCue(world, pos);
			}
			this.wasEngaging = true;
			if (this.roundsRemaining <= 0) {
				this.tickDryFireClick(world, pos);
			} else if (this.fireCooldown > 0) {
				this.fireCooldown--;
			} else {
				this.fire(world, pos, target);
				this.fireCooldown = FIRE_INTERVAL;
				firedThisTick = true;
			}
		} else {
			this.wasEngaging = false;
		}

		if (!firedThisTick) {
			this.heat = Math.max(0.0f, this.heat - COOL_PER_TICK);
		}

		if (this.heat >= HEAT_MAX) {
			this.overheated = true;
			this.ventCooldown = 0;
			world.playSound(null, pos, P7Sounds.UNIT_ALARM.get(), SoundCategory.BLOCKS, 1.0f, 0.7f);
		}

		this.markDirty();
	}

	/**
	 * Nearest non-creative/non-spectator player in range, with a manual
	 * raycast for line of sight — a block entity has no
	 * {@code EntityVisibilityCache} to lean on the way the mob version did.
	 */
	@Nullable
	private PlayerEntity acquireTarget(ServerWorld world, BlockPos pos) {
		Vec3d muzzle = muzzlePos(pos);
		PlayerEntity candidate = world.getClosestPlayer(muzzle.x, muzzle.y, muzzle.z, RANGE, false);
		if (candidate == null || !candidate.isAlive()) {
			return null;
		}
		if (!hasLineOfSight(world, muzzle, candidate.getEyePos())) {
			return null;
		}
		return candidate;
	}

	private static boolean hasLineOfSight(World world, Vec3d from, Vec3d to) {
		// ShapeContext.absent() rather than a null entity — a bare null is
		// ambiguous between the Entity and ShapeContext raycast overloads.
		BlockHitResult hit = world.raycast(new RaycastContext(from, to,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, ShapeContext.absent()));
		return hit.getType() == HitResult.Type.MISS;
	}

	private static Vec3d muzzlePos(BlockPos pos) {
		// Block center, then up ~0.5 for the barrel height, per the task spec.
		return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5 + 0.5, pos.getZ() + 0.5);
	}

	/**
	 * The shot itself: a hitscan burst with a tracer line, reproduced from
	 * {@code GunAttackGoal#fire}/{@code hitChance} — there's no projectile
	 * entity to spawn, the original goal never used one either.
	 */
	private void fire(ServerWorld world, BlockPos pos, PlayerEntity target) {
		Vec3d muzzle = muzzlePos(pos);
		Vec3d aimCenter = target.getBoundingBox().getCenter();
		double distance = muzzle.distanceTo(target.getPos());

		boolean hit = world.random.nextDouble() < hitChance(distance);
		Vec3d aim = aimCenter;
		if (!hit) {
			aim = aim.add((world.random.nextDouble() - 0.5) * 2.4,
					(world.random.nextDouble() - 0.5) * 1.6,
					(world.random.nextDouble() - 0.5) * 2.4);
		}

		// Muzzle flash + tracer line, same particles GunAttackGoal.fire() uses.
		world.spawnParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 2, 0.05, 0.05, 0.05, 0.01);
		Vec3d step = aim.subtract(muzzle);
		int steps = Math.max(2, (int) (step.length() / 0.8));
		step = step.multiply(1.0 / steps);
		Vec3d point = muzzle;
		for (int i = 0; i < steps; i++) {
			point = point.add(step);
			world.spawnParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		HitscanImpact.ejectCasing(world, muzzle, world.random);

		ProgramAcoustics.emit(world, muzzle, P7Sounds.GUN_FIRE.get(), SoundCategory.HOSTILE, 1.0f,
				1.1f + world.random.nextFloat() * 0.2f);

		if (hit) {
			// Rapid fire: bypass the target's post-hit invulnerability window
			// (see GunAttackGoal's gun-feel pass) so a sustained hose stacks.
			// This is timeUntilRegen (the field damage() gates repeat hits on),
			// NOT hurtTime (just the hurt animation), paired with knockback well
			// below vanilla melee's ~0.4 so it doesn't juggle the target.
			target.timeUntilRegen = 0;
			// No LivingEntity shooter to attribute this to (a block entity isn't one),
			// so this uses a generic damage source rather than GunAttackGoal's mobAttack().
			target.damage(world.getDamageSources().generic(), DAMAGE);
			// Push AWAY from the turret: (turret - target), since takeKnockback
			// shoves opposite the vector it's given (vanilla passes attacker-target).
			Vec3d shove = Vec3d.ofCenter(pos).subtract(target.getPos());
			if (shove.lengthSquared() > 1.0E-4) {
				target.takeKnockback(ROUND_CLASS.knockbackStrength(), shove.x, shove.z);
			}
		}

		// Wherever the round actually lands — chews up whatever block or
		// fluid caught it, hit or miss. No Entity to attribute a break to,
		// same reasoning as the damage source above.
		HitscanImpact.resolve(world, muzzle, aim, RANGE, null, ROUND_CLASS);

		this.heat += HEAT_PER_SHOT;
		this.roundsRemaining--;
	}

	/** Chance of a hit at the given distance; identical formula to GunAttackGoal.hitChance(). */
	private static double hitChance(double distance) {
		return 0.9 - (distance / RANGE) * 0.35;
	}

	/**
	 * Draws rounds from the nearest base ammo depot on a slow timer while the
	 * magazine isn't full — the emplaced-gun equivalent of a logistics-drone
	 * ammo run. Reuses the exact {@link SupplyNetwork} draw pattern the courier
	 * {@code AmmoRunGoal} uses, just pulled by the gun itself.
	 */
	private void tickResupply(ServerWorld world, BlockPos pos) {
		if (this.roundsRemaining >= MAGAZINE_CAPACITY) {
			return;
		}
		if (this.resupplyCooldown > 0) {
			this.resupplyCooldown--;
			return;
		}
		this.resupplyCooldown = RESUPPLY_INTERVAL_TICKS;
		SupplyNetwork net = ProgramDirectorState.get(world).getSupplyNetwork();
		SupplyNetwork.Depot depot = nearestAmmoDepot(net, pos);
		if (depot != null && net.drawSupply(depot.pos, SupplyNetwork.SUPPLY_AMMO, 1)) {
			this.loadRounds(ROUNDS_PER_RESUPPLY);
			Vec3d muzzle = muzzlePos(pos);
			world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, muzzle.x, muzzle.y, muzzle.z, 4, 0.2, 0.2, 0.2, 0.02);
		}
	}

	@Nullable
	private static SupplyNetwork.Depot nearestAmmoDepot(SupplyNetwork net, BlockPos pos) {
		SupplyNetwork.Depot best = null;
		double bestSq = RESUPPLY_RANGE * RESUPPLY_RANGE;
		for (SupplyNetwork.Depot depot : net.getDepots()) {
			if (!SupplyNetwork.SUPPLY_AMMO.equals(depot.supplyType) || depot.stock <= 0) {
				continue;
			}
			double sq = depot.pos.getSquaredDistance(pos);
			if (sq < bestSq) {
				bestSq = sq;
				best = depot;
			}
		}
		return best;
	}

	/** Plays the empty-magazine click on a cooldown so a dry gun doesn't spam it every tick. */
	private void tickDryFireClick(ServerWorld world, BlockPos pos) {
		if (this.dryFireCooldown > 0) {
			this.dryFireCooldown--;
			return;
		}
		this.dryFireCooldown = DRY_FIRE_CLICK_INTERVAL_TICKS;
		world.playSound(null, pos, P7Sounds.WEAPON_DRY_FIRE.get(), SoundCategory.BLOCKS, 0.5f,
				1.2f + world.random.nextFloat() * 0.15f);
	}

	/**
	 * Loud, low-pitched "the gun's opening up" report played once when a
	 * fresh target is acquired — distinct from the per-shot {@link
	 * P7Sounds#GUN_FIRE} tick, per the gun-feel pass. Mirrors
	 * {@code GunAttackGoal#playOpeningFireCue}.
	 */
	private void playOpeningFireCue(ServerWorld world, BlockPos pos) {
		Vec3d muzzle = muzzlePos(pos);
		ProgramAcoustics.emit(world, muzzle, P7Sounds.MORTAR_FIRE.get(), SoundCategory.HOSTILE, 1.3f,
				0.7f + world.random.nextFloat() * 0.1f);
	}

	/** The "it's down, push now" tell: periodic vent smoke while overheated. */
	private void tickVenting(ServerWorld world, BlockPos pos) {
		if (--this.ventCooldown > 0) {
			return;
		}
		this.ventCooldown = VENT_PARTICLE_INTERVAL;
		Vec3d muzzle = muzzlePos(pos);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 6, 0.15, 0.1, 0.15, 0.02);
	}

	public boolean isOverheated() {
		return this.overheated;
	}

	@Override
	public int getRoundsRemaining() {
		return this.roundsRemaining;
	}

	@Override
	public int getMagazineCapacity() {
		return MAGAZINE_CAPACITY;
	}

	@Override
	public void loadRounds(int rounds) {
		this.roundsRemaining = Math.min(MAGAZINE_CAPACITY, this.roundsRemaining + rounds);
	}

	@Override
	public Vec3d getWeaponPos() {
		return muzzlePos(this.getPos());
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putFloat(NBT_HEAT, this.heat);
		nbt.putBoolean(NBT_OVERHEATED, this.overheated);
		nbt.putInt(NBT_FIRE_COOLDOWN, this.fireCooldown);
		nbt.putInt(NBT_ROUNDS, this.roundsRemaining);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.heat = nbt.getFloat(NBT_HEAT);
		this.overheated = nbt.getBoolean(NBT_OVERHEATED);
		this.fireCooldown = nbt.getInt(NBT_FIRE_COOLDOWN);
		if (nbt.contains(NBT_ROUNDS)) {
			this.roundsRemaining = nbt.getInt(NBT_ROUNDS);
		}
	}
}
