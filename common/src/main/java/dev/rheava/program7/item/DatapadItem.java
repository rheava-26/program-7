package dev.rheava.program7.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.architectury.networking.NetworkManager;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.entity.AbstractShellEntity;
import dev.rheava.program7.entity.AirUAVEntity;
import dev.rheava.program7.entity.HarvesterDroneEntity;
import dev.rheava.program7.entity.LogisticsDroneEntity;
import dev.rheava.program7.entity.MediumMiningDroneEntity;
import dev.rheava.program7.entity.ProgramDroneEntity;
import dev.rheava.program7.entity.ReconHelicopterEntity;
import dev.rheava.program7.entity.ScoutCarEntity;
import dev.rheava.program7.entity.SniperDroneEntity;
import dev.rheava.program7.entity.SurveyorDroneEntity;
import dev.rheava.program7.entity.TransportDroneEntity;
import dev.rheava.program7.entity.WheeledHaulerEntity;
import dev.rheava.program7.network.DatapadSnapshotPayload;
import dev.rheava.program7.registry.P7DataComponents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * The Datapad — the player's window into the Program's own instrumentation.
 * Reading it (right-click) opens the v2 radar screen: a chunk-grid sweep of
 * everything the Program knows about the area, its posture/heat, the size of
 * the off-screen fleet, the escalation tier, and a bearing to the nearest
 * known base — plus, see {@link #trackedFor}, every target the player has
 * personally marked with a {@link TrackingChipItem}, plotted as an exact,
 * non-fog-limited blip. All of that is resolved server-side here and pushed
 * to the client as a {@link DatapadSnapshotPayload}; the client opens the
 * screen when it arrives.
 */
public class DatapadItem extends Item {
	/** Radar reach: everything within this many blocks of the player is plotted (~7 chunks). */
	private static final double RADAR_RANGE = 112.0;
	/** How far out to look for an inbound shell — past the gun's own reach to catch one still climbing. */
	private static final double WARN_DETECT_RADIUS = 160.0;
	/** A shell whose flight path closes to within this of the player counts as inbound "at" them. */
	private static final double WARN_NEAR_RADIUS = 16.0;

	public DatapadItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(net.minecraft.world.World world, PlayerEntity user, Hand hand) {
		if (world.isClient || !(user instanceof ServerPlayerEntity serverPlayer)) {
			return TypedActionResult.success(user.getStackInHand(hand));
		}
		NetworkManager.sendToPlayer(serverPlayer, snapshotFor(serverPlayer));
		return TypedActionResult.success(user.getStackInHand(hand));
	}

	/**
	 * Build the datapad's current read for {@code player}: nearby contacts as
	 * offsets from the player, plus the status-rail scalars. Called both when
	 * the datapad is first read and on each live-refresh poll from the open
	 * screen (see {@link dev.rheava.program7.network.DatapadRefreshPayload}).
	 */
	public static DatapadSnapshotPayload snapshotFor(ServerPlayerEntity player) {
		ServerWorld serverWorld = player.getServerWorld();
		ProgramDirectorState state = ProgramDirectorState.get(serverWorld);

		List<DatapadSnapshotPayload.Contact> contacts = new ArrayList<>();
		List<ProgramDroneEntity> nearby = serverWorld.getEntitiesByClass(ProgramDroneEntity.class,
				player.getBoundingBox().expand(RADAR_RANGE), e -> true);
		for (ProgramDroneEntity drone : nearby) {
			double dx = drone.getX() - player.getX();
			double dz = drone.getZ() - player.getZ();
			if (dx * dx + dz * dz > RADAR_RANGE * RADAR_RANGE) {
				continue;
			}
			contacts.add(new DatapadSnapshotPayload.Contact(
					(float) dx, (float) dz, drone.getAlertState().ordinal(), guessCategory(drone)));
		}

		float baseYaw = Float.NaN;
		int baseDistance = -1;
		BlockPos basePos = state.getProbeCorePos();
		if (basePos != null) {
			double dx = basePos.getX() + 0.5 - player.getX();
			double dz = basePos.getZ() + 0.5 - player.getZ();
			// atan2(dx, -dz): 0 = north (-Z), 90 = east (+X), matching Minecraft's axes.
			baseYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dx, -dz)));
			baseDistance = (int) Math.sqrt(dx * dx + dz * dz);
		}

		DatapadSnapshotPayload.Header header = new DatapadSnapshotPayload.Header(
				state.posture(serverWorld.getTime()),
				state.getHeat(),
				state.getVirtualFleet().size(),
				state.currentTierEstimate(),
				baseYaw,
				baseDistance);
		return new DatapadSnapshotPayload(header, contacts, trackedFor(player), incomingFor(player));
	}

	/**
	 * The datapad's DATAPAD INTEGRATION for the tracking chip: scan the
	 * player's whole inventory (main + armor + offhand, same {@code
	 * getInventory().size()} sweep as {@code ChargeLaserItem#findPowerBank})
	 * for every {@link TrackingChipItem} stack carrying a stored {@link
	 * TrackingChipTarget}, and resolve each one's <em>current</em> position
	 * directly — independent of whether the chip itself has been read
	 * recently. Unlike {@link #snapshotFor}'s {@link DatapadSnapshotPayload.Contact}
	 * list this isn't range-limited or fog-limited: it's the player's own
	 * marked intel, so the datapad shows it exactly, wherever it is. A target
	 * that's currently loaded, alive, and in this dimension resolves live; a
	 * dead, unloaded, or cross-dimension target falls back to the chip's
	 * last-known stored position and is flagged {@code live = false} so the
	 * client can grey it as "signal lost", mirroring {@code
	 * TrackingChipItem#read}'s own null/isAlive handling. Duplicate chips
	 * tracking the same target only ever produce one blip.
	 */
	private static List<DatapadSnapshotPayload.Tracked> trackedFor(ServerPlayerEntity player) {
		ServerWorld serverWorld = player.getServerWorld();
		List<DatapadSnapshotPayload.Tracked> tracked = new ArrayList<>();
		Set<UUID> seen = new HashSet<>();

		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!(stack.getItem() instanceof TrackingChipItem)) {
				continue;
			}
			TrackingChipTarget target = stack.get(P7DataComponents.TRACKING_CHIP_TARGET);
			if (target == null || !seen.add(target.targetUuid())) {
				continue;
			}

			Entity found = serverWorld.getEntity(target.targetUuid());
			boolean live = found != null && found.isAlive();
			double x = live ? found.getX() : target.lastX();
			double z = live ? found.getZ() : target.lastZ();

			double dx = x - player.getX();
			double dz = z - player.getZ();
			tracked.add(new DatapadSnapshotPayload.Tracked((float) dx, (float) dz, live));
		}
		return tracked;
	}

	/**
	 * The §5/§9 inbound-artillery telegraph: scan for any indirect-fire
	 * munition (every {@link AbstractShellEntity} subclass — howitzer, mortar,
	 * rocket, missile, naval, bomb) in the air whose flight path closes to
	 * within {@link #WARN_NEAR_RADIUS} of the player, and report the most
	 * imminent one as a rough bearing (the direction it's coming from) and an
	 * ETA in seconds. Closest-horizontal-approach math on the shell's own
	 * velocity, so it fires the warning while the round is still climbing —
	 * the datapad half of the whistle the player already hears.
	 */
	private static DatapadSnapshotPayload.Incoming incomingFor(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		List<AbstractShellEntity> shells = world.getEntitiesByClass(AbstractShellEntity.class,
				player.getBoundingBox().expand(WARN_DETECT_RADIUS), e -> true);

		AbstractShellEntity soonest = null;
		double soonestTicks = Double.MAX_VALUE;
		for (AbstractShellEntity shell : shells) {
			double vx = shell.getVelocity().x;
			double vz = shell.getVelocity().z;
			double speedSq = vx * vx + vz * vz;
			if (speedSq < 1.0e-4) {
				continue;
			}
			// Time of closest horizontal approach of the shell to the player.
			double px = shell.getX() - player.getX();
			double pz = shell.getZ() - player.getZ();
			double t = -(px * vx + pz * vz) / speedSq;
			if (t < 0.0) {
				continue;
			}
			double missX = px + vx * t;
			double missZ = pz + vz * t;
			double missSq = missX * missX + missZ * missZ;
			if (missSq > WARN_NEAR_RADIUS * WARN_NEAR_RADIUS) {
				continue;
			}
			if (t < soonestTicks) {
				soonestTicks = t;
				soonest = shell;
			}
		}

		if (soonest == null) {
			return DatapadSnapshotPayload.Incoming.NONE;
		}
		// Bearing toward where the round currently is — the direction it's inbound from.
		double dx = soonest.getX() - player.getX();
		double dz = soonest.getZ() - player.getZ();
		float bearing = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dx, -dz)));
		int etaSeconds = Math.max(0, (int) Math.ceil(soonestTicks / 20.0));
		return new DatapadSnapshotPayload.Incoming(bearing, etaSeconds);
	}

	/**
	 * A deliberately coarse type read: the datapad can tell roughly what class
	 * of hardware it's looking at, not the exact unit. 1 = gun/armed, 2 =
	 * recon/spotter, 3 = logistics. (Acoustic bluffing — a logistics drone
	 * faking gunfire — is a later layer; for now this is an honest guess.)
	 */
	private static int guessCategory(ProgramDroneEntity drone) {
		if (drone instanceof SurveyorDroneEntity || drone instanceof AirUAVEntity
				|| drone instanceof ScoutCarEntity || drone instanceof ReconHelicopterEntity
				|| drone instanceof SniperDroneEntity) {
			return 2;
		}
		if (drone instanceof LogisticsDroneEntity || drone instanceof WheeledHaulerEntity
				|| drone instanceof TransportDroneEntity || drone instanceof MediumMiningDroneEntity
				|| drone instanceof HarvesterDroneEntity) {
			return 3;
		}
		return 1;
	}
}
