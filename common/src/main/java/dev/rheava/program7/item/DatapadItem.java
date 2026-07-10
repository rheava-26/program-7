package dev.rheava.program7.item;

import java.util.ArrayList;
import java.util.List;

import dev.architectury.networking.NetworkManager;
import dev.rheava.program7.director.ProgramDirectorState;
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
 * known base. All of that is resolved server-side here and pushed to the
 * client as a {@link DatapadSnapshotPayload}; the client opens the screen
 * when it arrives.
 */
public class DatapadItem extends Item {
	/** Radar reach: everything within this many blocks of the player is plotted (~7 chunks). */
	private static final double RADAR_RANGE = 112.0;

	public DatapadItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(net.minecraft.world.World world, PlayerEntity user, Hand hand) {
		if (world.isClient || !(user instanceof ServerPlayerEntity serverPlayer)) {
			return TypedActionResult.success(user.getStackInHand(hand));
		}

		ServerWorld serverWorld = (ServerWorld) world;
		ProgramDirectorState state = ProgramDirectorState.get(serverWorld);

		List<DatapadSnapshotPayload.Contact> contacts = new ArrayList<>();
		List<ProgramDroneEntity> nearby = serverWorld.getEntitiesByClass(ProgramDroneEntity.class,
				user.getBoundingBox().expand(RADAR_RANGE), e -> true);
		for (ProgramDroneEntity drone : nearby) {
			double dx = drone.getX() - user.getX();
			double dz = drone.getZ() - user.getZ();
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
			double dx = basePos.getX() + 0.5 - user.getX();
			double dz = basePos.getZ() + 0.5 - user.getZ();
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
		NetworkManager.sendToPlayer(serverPlayer, new DatapadSnapshotPayload(header, contacts));

		return TypedActionResult.success(user.getStackInHand(hand));
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
