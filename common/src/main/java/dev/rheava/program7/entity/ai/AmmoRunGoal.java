package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.block.AmmoBoxBlock;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.director.SupplyNetwork;
import dev.rheava.program7.entity.LogisticsDroneEntity;
import dev.rheava.program7.entity.ReloadableWeapon;
import dev.rheava.program7.registry.P7Blocks;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * The ammo delivery loop: an idle logistics drone (no build-payment mission
 * — see {@code director.BasePad#buildStockpile}, which spawns dedicated
 * ammo-runners this way) finds a nearby {@link ReloadableWeapon} that
 * {@code needsReload()}, flies to the nearest {@link SupplyNetwork}
 * {@code SUPPLY_AMMO} stockpile, visibly draws one {@link AmmoBoxBlock}
 * charge (a carry-particle trail plays for the return leg), flies to the
 * weapon's {@link ReloadableWeapon#getWeaponPos()}, and calls {@link
 * ReloadableWeapon#loadRounds(int)}. A full round trip is handling time
 * (2x{@link #HANDLING_TICKS}) plus flight at the drone's normal cruise
 * speed — roughly the ~5s/100-tick round trip the design calls for at
 * typical in-base stockpile-to-weapon distances; more idle logistics drones
 * means more weapons serviced in parallel, since each one only ever chases
 * a single job at a time.
 *
 * <p>Deliberately never interrupts {@link SupplyRunGoal}: {@link
 * #canStart()} bails immediately if the drone already has a build-payment
 * {@code destination} set, so a courier mid-delivery is never diverted onto
 * ammo duty.
 */
public class AmmoRunGoal extends Goal {
	private static final double ARRIVAL_RANGE = 2.5;
	/** How far out this drone looks for a weapon to service. */
	private static final double WEAPON_SEARCH_RADIUS = 96.0;
	/** How far out this drone looks for an ammo stockpile depot. */
	private static final double STOCKPILE_SEARCH_RADIUS = 256.0;
	/** How far around a stockpile depot's registered position to look for a real ammo box to draw down. */
	private static final int AMMO_BOX_SEARCH_RADIUS = 3;
	private static final int ROUNDS_PER_TRIP = 20;
	/** Pause at each end of the trip while "handling" the crate — about 0.75s. */
	private static final int HANDLING_TICKS = 15;

	private enum Phase {
		TO_STOCKPILE, PICKING_UP, TO_WEAPON, DELIVERING
	}

	private final LogisticsDroneEntity drone;

	@Nullable
	private Entity targetWeapon;
	@Nullable
	private BlockPos stockpilePos;
	private Phase phase = Phase.TO_STOCKPILE;
	private int handlingTicks;

	public AmmoRunGoal(LogisticsDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		// Never steals a drone that's mid build-payment courier run.
		if (this.drone.getDestination() != null || this.drone.isScrambled()) {
			return false;
		}
		if (!(this.drone.getWorld() instanceof ServerWorld world)) {
			return false;
		}
		Entity weapon = findWeaponNeedingReload(world, this.drone);
		if (weapon == null) {
			return false;
		}
		BlockPos stockpile = findStockpile(world, this.drone.getBlockPos());
		if (stockpile == null) {
			return false;
		}
		this.targetWeapon = weapon;
		this.stockpilePos = stockpile;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return this.targetWeapon != null && this.targetWeapon.isAlive() && !this.drone.isScrambled();
	}

	@Override
	public void start() {
		this.phase = Phase.TO_STOCKPILE;
		this.handlingTicks = 0;
	}

	@Override
	public void stop() {
		this.targetWeapon = null;
		this.stockpilePos = null;
		this.drone.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (!(this.drone.getWorld() instanceof ServerWorld world) || this.stockpilePos == null) {
			return;
		}
		switch (this.phase) {
			case TO_STOCKPILE -> {
				BlockPos pos = this.stockpilePos;
				this.flyTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, () -> {
					this.phase = Phase.PICKING_UP;
					this.handlingTicks = HANDLING_TICKS;
				});
			}
			case PICKING_UP -> this.tickHandling(() -> {
				if (this.drawAmmoFromStockpile(world)) {
					this.phase = Phase.TO_WEAPON;
				} else {
					// Stockpile ran dry between canStart and now: give up this
					// run cleanly, a fresh one gets picked next time this goal
					// is offered.
					this.targetWeapon = null;
				}
			});
			case TO_WEAPON -> {
				if (!(this.targetWeapon instanceof ReloadableWeapon weapon) || !this.targetWeapon.isAlive()) {
					this.targetWeapon = null;
					return;
				}
				this.spawnCarryTrail(world);
				Vec3d dest = weapon.getWeaponPos();
				this.flyTo(dest.x, dest.y, dest.z, () -> {
					this.phase = Phase.DELIVERING;
					this.handlingTicks = HANDLING_TICKS;
				});
			}
			case DELIVERING -> this.tickHandling(() -> {
				if (this.targetWeapon instanceof ReloadableWeapon weapon && this.targetWeapon.isAlive()) {
					weapon.loadRounds(ROUNDS_PER_TRIP);
					world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
							this.drone.getX(), this.drone.getY() + 0.5, this.drone.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
					this.drone.playSound(P7Sounds.DRONE_SCAN_BEEP.get(), 0.7f, 1.4f);
				}
				this.targetWeapon = null; // done — shouldContinue() ends the goal next tick
			});
		}
	}

	private void flyTo(double x, double y, double z, Runnable onArrive) {
		this.drone.getLookControl().lookAt(x, y, z);
		if (this.drone.squaredDistanceTo(x, y, z) > ARRIVAL_RANGE * ARRIVAL_RANGE) {
			if (this.drone.getNavigation().isIdle()) {
				this.drone.getNavigation().startMovingTo(x, y, z, 1.0);
			}
			return;
		}
		this.drone.getNavigation().stop();
		onArrive.run();
	}

	private void tickHandling(Runnable onDone) {
		this.handlingTicks--;
		if (this.handlingTicks <= 0) {
			onDone.run();
		}
	}

	/** Carry-state tell for the return leg: a light particle trail so "this drone is loaded" reads at a glance. */
	private void spawnCarryTrail(ServerWorld world) {
		if (this.drone.age % 4 != 0) {
			return;
		}
		world.spawnParticles(ParticleTypes.CRIT,
				this.drone.getX(), this.drone.getY() - 0.2, this.drone.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
	}

	/**
	 * Debit one crate from the depot's abstract stock and, best-effort,
	 * decrement (or remove) a real {@link AmmoBoxBlock} near the stockpile
	 * position so the pile visibly shrinks. The ledger debit is the source of
	 * truth for "is there ammo to fetch"; the physical block is cosmetic and
	 * skipped silently if it's not there (e.g. a player already looted it).
	 */
	private boolean drawAmmoFromStockpile(ServerWorld world) {
		if (this.stockpilePos == null) {
			return false;
		}
		SupplyNetwork net = ProgramDirectorState.get(world).getSupplyNetwork();
		if (!net.drawSupply(this.stockpilePos, SupplyNetwork.SUPPLY_AMMO, 1)) {
			return false;
		}
		BlockPos box = findNearbyAmmoBox(world, this.stockpilePos);
		if (box != null) {
			this.consumeAmmoBox(world, box);
		}
		this.drone.playSound(P7Sounds.DRONE_SCAN_BEEP.get(), 0.6f, 0.8f);
		world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
				this.drone.getX(), this.drone.getY() + 0.4, this.drone.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
		return true;
	}

	private void consumeAmmoBox(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (!(state.getBlock() instanceof AmmoBoxBlock)) {
			return;
		}
		int count = state.get(AmmoBoxBlock.COUNT);
		if (count <= 1) {
			world.removeBlock(pos, false);
		} else {
			world.setBlockState(pos, state.with(AmmoBoxBlock.COUNT, count - 1));
		}
	}

	@Nullable
	private static BlockPos findNearbyAmmoBox(ServerWorld world, BlockPos center) {
		for (BlockPos candidate : BlockPos.iterate(
				center.add(-AMMO_BOX_SEARCH_RADIUS, -1, -AMMO_BOX_SEARCH_RADIUS),
				center.add(AMMO_BOX_SEARCH_RADIUS, 2, AMMO_BOX_SEARCH_RADIUS))) {
			if (world.getBlockState(candidate).isOf(P7Blocks.AMMO_BOX.get())) {
				return candidate.toImmutable();
			}
		}
		return null;
	}

	@Nullable
	private static BlockPos findStockpile(ServerWorld world, BlockPos dronePos) {
		SupplyNetwork net = ProgramDirectorState.get(world).getSupplyNetwork();
		SupplyNetwork.Depot nearest = null;
		double bestDistanceSq = STOCKPILE_SEARCH_RADIUS * STOCKPILE_SEARCH_RADIUS;
		for (SupplyNetwork.Depot depot : net.getDepots()) {
			if (!SupplyNetwork.SUPPLY_AMMO.equals(depot.supplyType) || depot.stock <= 0) {
				continue;
			}
			double distanceSq = depot.pos.getSquaredDistance(dronePos);
			if (distanceSq < bestDistanceSq) {
				bestDistanceSq = distanceSq;
				nearest = depot;
			}
		}
		return nearest != null ? nearest.pos : null;
	}

	@Nullable
	private static Entity findWeaponNeedingReload(ServerWorld world, Entity searcher) {
		Box box = new Box(searcher.getBlockPos()).expand(WEAPON_SEARCH_RADIUS);
		Entity nearest = null;
		double bestDistanceSq = Double.MAX_VALUE;
		for (Entity candidate : world.getEntitiesByClass(Entity.class, box,
				e -> e instanceof ReloadableWeapon rw && rw.needsReload())) {
			double distanceSq = candidate.squaredDistanceTo(searcher.getX(), searcher.getY(), searcher.getZ());
			if (distanceSq < bestDistanceSq) {
				bestDistanceSq = distanceSq;
				nearest = candidate;
			}
		}
		return nearest;
	}
}
