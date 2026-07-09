package dev.rheava.program7.block;

import dev.rheava.program7.entity.ai.HitscanImpact;
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
 * (range 20, fireInterval 6 ticks, 3.0 damage).
 *
 * <p>New on top of the mob version: an overheat gate. Sustained fire builds
 * heat; crossing {@link #HEAT_MAX} shuts the gun down until it cools back
 * below {@link #HEAT_RESET} (hysteresis, so it doesn't immediately resume
 * and re-trip). While overheated it vents smoke and sounds off once so the
 * player can read "it's down — push now."
 */
public class AutogunTurretBlockEntity extends BlockEntity {
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
	private static final float DAMAGE = 3.0f;
	/** How often (in ticks) to puff vent smoke while overheated. */
	private static final int VENT_PARTICLE_INTERVAL = 10;

	private static final String NBT_HEAT = "Heat";
	private static final String NBT_OVERHEATED = "Overheated";
	private static final String NBT_FIRE_COOLDOWN = "FireCooldown";

	private float heat = 0.0f;
	private boolean overheated = false;
	private int fireCooldown = 0;
	private int ventCooldown = 0;

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

		PlayerEntity target = this.acquireTarget(world, pos);
		boolean firedThisTick = false;
		if (target != null) {
			if (this.fireCooldown > 0) {
				this.fireCooldown--;
			} else {
				this.fire(world, pos, target);
				this.fireCooldown = FIRE_INTERVAL;
				firedThisTick = true;
			}
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

		world.playSound(null, pos, P7Sounds.GUN_FIRE.get(), SoundCategory.BLOCKS, 1.0f,
				1.1f + world.random.nextFloat() * 0.2f);

		if (hit) {
			// No LivingEntity shooter to attribute this to (a block entity isn't one),
			// so this uses a generic damage source rather than GunAttackGoal's mobAttack().
			target.damage(world.getDamageSources().generic(), DAMAGE);
		}

		// Wherever the round actually lands — chews up whatever block or
		// fluid caught it, hit or miss. No Entity to attribute a break to,
		// same reasoning as the damage source above.
		HitscanImpact.resolve(world, muzzle, aim, RANGE, null);

		this.heat += HEAT_PER_SHOT;
	}

	/** Chance of a hit at the given distance; identical formula to GunAttackGoal.hitChance(). */
	private static double hitChance(double distance) {
		return 0.9 - (distance / RANGE) * 0.35;
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
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putFloat(NBT_HEAT, this.heat);
		nbt.putBoolean(NBT_OVERHEATED, this.overheated);
		nbt.putInt(NBT_FIRE_COOLDOWN, this.fireCooldown);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.heat = nbt.getFloat(NBT_HEAT);
		this.overheated = nbt.getBoolean(NBT_OVERHEATED);
		this.fireCooldown = nbt.getInt(NBT_FIRE_COOLDOWN);
	}
}
