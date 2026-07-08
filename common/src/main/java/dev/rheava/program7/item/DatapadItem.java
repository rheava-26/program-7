package dev.rheava.program7.item;

import java.util.List;

import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.entity.ProgramDroneEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * The Datapad — the player's window into the Program's own instrumentation.
 * A right-click prints a short, in-fiction intel readout to chat: the
 * Program's current global threat reading, how many hostile units are
 * loitering nearby (and where), and a bearing to the nearest known base, if
 * one has ever been scouted.
 *
 * <p>Deliberately simple for v1: no custom HUD, no networking — everything
 * resolves server-side inside {@link #use}.
 */
public class DatapadItem extends Item {
	/** How far out to count loitering Program hardware. */
	private static final double SCAN_RANGE = 64.0;
	private static final String[] COMPASS_POINTS = {
			"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
			"S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

	public DatapadItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(net.minecraft.world.World world, PlayerEntity user, Hand hand) {
		if (world.isClient) {
			return TypedActionResult.success(user.getStackInHand(hand));
		}

		ServerWorld serverWorld = (ServerWorld) world;
		ProgramDirectorState state = ProgramDirectorState.get(serverWorld);

		List<ProgramDroneEntity> nearby = serverWorld.getEntitiesByClass(ProgramDroneEntity.class,
				user.getBoundingBox().expand(SCAN_RANGE), e -> true);

		user.sendMessage(Text.literal("▚ PROGRAM DATAPAD ▚").formatted(Formatting.AQUA, Formatting.BOLD), false);
		user.sendMessage(Text.literal("Global threat reading: " + state.getGlobalThreat() + "/"
				+ ProgramDirectorState.MAX_THREAT).formatted(Formatting.GRAY), false);

		if (nearby.isEmpty()) {
			user.sendMessage(Text.literal("No hostile signatures within " + (int) SCAN_RANGE + "m.")
					.formatted(Formatting.DARK_GRAY), false);
		} else {
			ProgramDroneEntity nearest = null;
			double nearestDistSq = Double.MAX_VALUE;
			for (ProgramDroneEntity drone : nearby) {
				double distSq = drone.squaredDistanceTo(user);
				if (distSq < nearestDistSq) {
					nearestDistSq = distSq;
					nearest = drone;
				}
			}
			String bearing = nearest != null
					? bearingTo(user.getX(), user.getZ(), nearest.getX(), nearest.getZ())
					: "?";
			user.sendMessage(Text.literal(nearby.size() + " hostile signature(s) within " + (int) SCAN_RANGE
					+ "m — nearest bearing " + bearing).formatted(Formatting.RED), false);
		}

		// coreSites isn't exposed beyond the probe core itself; the live core
		// (if any) is the closest thing to a "nearest known base" the Director
		// currently surfaces.
		BlockPos basePos = state.getProbeCorePos();
		if (basePos != null) {
			double distance = Math.sqrt(user.getBlockPos().getSquaredDistance(basePos));
			String bearing = bearingTo(user.getX(), user.getZ(), basePos.getX() + 0.5, basePos.getZ() + 0.5);
			user.sendMessage(Text.literal("Nearest known base: bearing " + bearing + ", ~"
					+ (int) distance + "m").formatted(Formatting.GOLD), false);
		} else {
			user.sendMessage(Text.literal("No base located.").formatted(Formatting.DARK_GRAY), false);
		}

		return TypedActionResult.success(user.getStackInHand(hand));
	}

	/** 16-point compass bearing from (fromX, fromZ) to (toX, toZ). */
	private static String bearingTo(double fromX, double fromZ, double toX, double toZ) {
		double dx = toX - fromX;
		double dz = toZ - fromZ;
		// atan2(dx, -dz): 0 = north (-Z), 90 = east (+X), matching Minecraft's axes.
		double angle = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dx, -dz)));
		if (angle < 0) {
			angle += 360.0;
		}
		int index = (int) Math.round(angle / 22.5) % 16;
		return COMPASS_POINTS[index];
	}
}
