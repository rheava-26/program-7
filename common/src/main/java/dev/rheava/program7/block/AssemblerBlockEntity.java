package dev.rheava.program7.block;

import java.util.Map;

import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.director.Resources;
import dev.rheava.program7.entity.AutogunTurretEntity;
import dev.rheava.program7.entity.GroundDroneEntity;
import dev.rheava.program7.entity.LogisticsDroneEntity;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.entity.WheeledHaulerEntity;
import dev.rheava.program7.registry.P7BlockEntities;
import dev.rheava.program7.registry.P7Blocks;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The production queue behind the {@link AssemblerBlock}. Every few seconds
 * it takes stock of the guards within its patrol footprint; if the base is
 * under complement AND the Director's ledger can pay, it starts a build —
 * half a minute of sparks and machining noise, then the unit rolls off next
 * to the block.
 *
 * <p>Counterplay is baked in: kill guards to drain the ledger through
 * replacements, kill the harvesters to stop the ledger refilling, or crack
 * the assembler itself and nothing gets built at all.
 *
 * <p>The ledger being charged isn't the end of it, either: a courier has to
 * physically fly or drive the payment over from the probe core before the
 * build starts. Shoot the courier down in transit and that build's cost is
 * lost — the whole supply line is interceptable.
 */
public class AssemblerBlockEntity extends BlockEntity {
	/** Tier of unit this assembler class can produce. */
	public static final int TIER = 1;
	private static final int BUILD_TICKS = 600;
	private static final int EVALUATE_INTERVAL = 100;
	/** How long a courier gets to complete its run before the payment is written off. */
	private static final int SUPPLY_TIMEOUT = 1600;
	/** How far out (and how far up/down) to look for a probe core to launch a courier from. */
	private static final int CORE_SEARCH_RADIUS = 8;
	private static final int CORE_SEARCH_HEIGHT = 4;
	/** How far out this assembler counts (and credits) existing guards. */
	private static final double COMPLEMENT_RADIUS = 48.0;
	private static final int GROUND_DRONE_QUOTA = 2;
	private static final int AUTOGUN_QUOTA = 2;

	private static final String JOB_GROUND_DRONE = "ground_drone";
	private static final String JOB_AUTOGUN_TURRET = "autogun_turret";
	private static final Map<String, Integer> GROUND_DRONE_COST = Map.of(
			Resources.IRON, 6, Resources.COPPER, 2, Resources.REDSTONE, 2);
	private static final Map<String, Integer> AUTOGUN_COST = Map.of(
			Resources.IRON, 8, Resources.REDSTONE, 3, Resources.GUNPOWDER, 2);

	private String currentJob = "";
	private int buildTicksLeft = 0;
	private int evaluateCooldown = EVALUATE_INTERVAL;
	/** True while a courier is (supposedly) inbound with this job's payment. */
	private boolean awaitingSupply = false;
	private int supplyTimeout = 0;

	public AssemblerBlockEntity(BlockPos pos, BlockState state) {
		super(P7BlockEntities.ASSEMBLER.get(), pos, state);
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, AssemblerBlockEntity assembler) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		if (assembler.awaitingSupply) {
			assembler.tickSupplyWait();
		} else if (!assembler.currentJob.isEmpty()) {
			assembler.tickBuild(serverWorld, pos);
		} else if (--assembler.evaluateCooldown <= 0) {
			assembler.evaluateCooldown = EVALUATE_INTERVAL;
			assembler.chooseJob(serverWorld, pos);
		}
	}

	/** Courier's overdue: write off the payment and go back to the drawing board. */
	private void tickSupplyWait() {
		if (--this.supplyTimeout <= 0) {
			this.awaitingSupply = false;
			this.currentJob = "";
			this.markDirty();
		}
	}

	private void tickBuild(ServerWorld world, BlockPos pos) {
		this.buildTicksLeft--;
		if (this.buildTicksLeft % 20 == 0) {
			world.playSound(null, pos, P7Sounds.ASSEMBLER_WORKING.get(), SoundCategory.BLOCKS,
					0.6f, 0.9f + world.random.nextFloat() * 0.2f);
			world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
					pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 4, 0.3, 0.15, 0.3, 0.02);
		}
		if (this.buildTicksLeft <= 0) {
			this.completeJob(world, pos);
			this.currentJob = "";
			this.markDirty();
		}
	}

	private void chooseJob(ServerWorld world, BlockPos pos) {
		ProgramDirectorState director = ProgramDirectorState.get(world);
		Box footprint = new Box(pos).expand(COMPLEMENT_RADIUS);
		int turrets = world.getEntitiesByClass(AutogunTurretEntity.class, footprint, e -> true).size();
		int patrols = world.getEntitiesByClass(GroundDroneEntity.class, footprint, e -> true).size();

		// Fixed defenses first, then the patrol ring.
		if (turrets < AUTOGUN_QUOTA && director.tryConsume(AUTOGUN_COST)) {
			this.beginProduction(world, pos, JOB_AUTOGUN_TURRET, AUTOGUN_COST);
		} else if (patrols < GROUND_DRONE_QUOTA && director.tryConsume(GROUND_DRONE_COST)) {
			this.beginProduction(world, pos, JOB_GROUND_DRONE, GROUND_DRONE_COST);
		}
	}

	/**
	 * The ledger's already been charged; now the payment has to physically
	 * get here. Launch a courier from the nearest probe core carrying the
	 * cost as cargo and wait for it to dock — the build itself doesn't start
	 * until then. No core in range (or no courier available) falls back to
	 * the old instant-build behavior.
	 */
	private void beginProduction(ServerWorld world, BlockPos pos, String job, Map<String, Integer> cost) {
		BlockPos corePos = findProbeCore(world, pos);
		ProgramDroneEntity courier = corePos != null ? spawnCourier(world, corePos, pos, job, cost) : null;
		if (courier == null) {
			this.startJob(world, pos, job);
			return;
		}
		this.currentJob = job;
		this.awaitingSupply = true;
		this.supplyTimeout = SUPPLY_TIMEOUT;
		this.markDirty();
	}

	@Nullable
	private static BlockPos findProbeCore(ServerWorld world, BlockPos pos) {
		for (BlockPos candidate : BlockPos.iterateOutwards(pos,
				CORE_SEARCH_RADIUS, CORE_SEARCH_HEIGHT, CORE_SEARCH_RADIUS)) {
			if (world.getBlockState(candidate).isOf(P7Blocks.PROBE_CORE.get())) {
				return candidate.toImmutable();
			}
		}
		return null;
	}

	@Nullable
	private static ProgramDroneEntity spawnCourier(ServerWorld world, BlockPos corePos, BlockPos destination,
			String job, Map<String, Integer> cargo) {
		ProgramDroneEntity courier = world.random.nextBoolean()
				? P7Entities.LOGISTICS_DRONE.get().create(world)
				: P7Entities.WHEELED_HAULER.get().create(world);
		if (courier == null) {
			return null;
		}
		courier.refreshPositionAndAngles(corePos.getX() + 0.5, corePos.getY() + 1.5, corePos.getZ() + 0.5,
				world.random.nextFloat() * 360.0f, 0.0f);
		if (courier instanceof LogisticsDroneEntity logisticsDrone) {
			logisticsDrone.beginMission(destination, job, cargo);
		} else if (courier instanceof WheeledHaulerEntity wheeledHauler) {
			wheeledHauler.beginMission(destination, job, cargo);
		}
		world.spawnEntity(courier);
		return courier;
	}

	private void startJob(ServerWorld world, BlockPos pos, String job) {
		this.currentJob = job;
		this.buildTicksLeft = BUILD_TICKS;
		this.markDirty();
		world.playSound(null, pos, P7Sounds.ASSEMBLER_WORKING.get(), SoundCategory.BLOCKS, 0.8f, 0.7f);
	}

	/**
	 * A courier docked with this job's cargo. Only accepted while actually
	 * waiting on that exact job — stray or duplicate calls are ignored.
	 */
	public void onSupplyDelivered(String job) {
		if (!this.awaitingSupply || !this.currentJob.equals(job)) {
			return;
		}
		this.awaitingSupply = false;
		if (this.getWorld() instanceof ServerWorld world) {
			this.startJob(world, this.getPos(), job);
		}
	}

	private void completeJob(ServerWorld world, BlockPos pos) {
		ProgramDroneEntity unit = switch (this.currentJob) {
			case JOB_GROUND_DRONE -> P7Entities.GROUND_DRONE.get().create(world);
			case JOB_AUTOGUN_TURRET -> P7Entities.AUTOGUN_TURRET.get().create(world);
			default -> null;
		};
		if (unit == null) {
			return;
		}
		BlockPos spot = findRollOffSpot(world, pos);
		unit.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
				world.random.nextFloat() * 360.0f, 0.0f);
		if (unit instanceof GroundDroneEntity groundDrone) {
			groundDrone.setHomePos(pos);
		}
		world.spawnEntity(unit);
		world.playSound(null, pos, P7Sounds.ASSEMBLER_COMPLETE.get(), SoundCategory.BLOCKS, 1.0f, 1.0f);
		world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
				spot.getX() + 0.5, spot.getY() + 0.5, spot.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.01);
	}

	private static BlockPos findRollOffSpot(ServerWorld world, BlockPos pos) {
		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos side = pos.offset(direction);
			for (BlockPos candidate : new BlockPos[] {side, side.down(), side.up()}) {
				if (world.getBlockState(candidate).isReplaceable()
						&& !world.getBlockState(candidate.down()).isReplaceable()) {
					return candidate;
				}
			}
		}
		return pos.up();
	}

	/** Debug/inspection: what's on the line right now, if anything. */
	@Nullable
	public String getCurrentJob() {
		return this.currentJob.isEmpty() ? null : this.currentJob;
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.writeNbt(nbt, registryLookup);
		nbt.putString("CurrentJob", this.currentJob);
		nbt.putInt("BuildTicksLeft", this.buildTicksLeft);
		nbt.putBoolean("AwaitingSupply", this.awaitingSupply);
		nbt.putInt("SupplyTimeout", this.supplyTimeout);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		super.readNbt(nbt, registryLookup);
		this.currentJob = nbt.getString("CurrentJob");
		this.buildTicksLeft = nbt.getInt("BuildTicksLeft");
		this.awaitingSupply = nbt.getBoolean("AwaitingSupply");
		this.supplyTimeout = nbt.getInt("SupplyTimeout");
	}
}
