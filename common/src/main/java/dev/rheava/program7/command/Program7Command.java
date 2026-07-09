package dev.rheava.program7.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.rheava.program7.director.DroneToken;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.director.RiskAssessment;
import dev.rheava.program7.director.ScanRecord;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Operator/debug command surface for the Program Director.
 *
 * <pre>
 * /program7 status        — global threat, scan count, and your intel file
 * /program7 assess        — run a live risk assessment on yourself
 * /program7 threat &lt;0-100&gt; — force the global threat level
 * /program7 land [distance] — force the drop pod down now (testing)
 * /program7 fleet          — list virtualized drone tokens (read-only, debug)
 * </pre>
 */
public final class Program7Command {
	public static void register() {
		CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("program7")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.literal("status").executes(Program7Command::status))
						.then(CommandManager.literal("assess").executes(Program7Command::assessSelf))
						.then(CommandManager.literal("threat")
								.then(CommandManager.argument("value", IntegerArgumentType.integer(0, 100))
										.executes(Program7Command::setThreat)))
						.then(CommandManager.literal("land")
								.executes(context -> land(context, 120))
								.then(CommandManager.argument("distance", IntegerArgumentType.integer(32, 2000))
										.executes(context -> land(context,
												IntegerArgumentType.getInteger(context, "distance")))))
						.then(CommandManager.literal("fleet").executes(Program7Command::fleet))));
	}

	private static int fleet(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ProgramDirectorState state = ProgramDirectorState.get(source.getWorld());
		java.util.List<DroneToken> tokens = state.getVirtualFleet().getTokens();
		source.sendFeedback(() -> Text.literal("[Program 7] Virtual fleet — " + tokens.size() + " token(s)"), false);
		for (DroneToken token : tokens) {
			source.sendFeedback(() -> Text.literal("[Program 7]  - " + token.getEntityTypeId()
					+ " @ (" + Math.round(token.getX()) + ", " + Math.round(token.getY())
					+ ", " + Math.round(token.getZ()) + ")"), false);
		}
		return tokens.size();
	}

	private static int land(CommandContext<ServerCommandSource> context, int distance) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		ProgramDirectorState state = ProgramDirectorState.get(context.getSource().getWorld());
		state.deployPod(context.getSource().getWorld(), player,
				(int) (distance * 0.8), (int) (distance * 1.2));
		context.getSource().sendFeedback(() -> Text.literal("[Program 7] Insertion authorized, ~"
				+ distance + " blocks out."), true);
		return 1;
	}

	private static int status(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ProgramDirectorState state = ProgramDirectorState.get(source.getWorld());
		source.sendFeedback(() -> Text.literal("[Program 7] Global threat: " + state.getGlobalThreat()
				+ "/" + ProgramDirectorState.MAX_THREAT
				+ " — scans completed: " + state.getScansCompleted()), false);
		if (state.isPodDeployed()) {
			StringBuilder stockpile = new StringBuilder("[Program 7] Stockpile —");
			state.getResources().entrySet().stream()
					.sorted(java.util.Map.Entry.comparingByKey())
					.forEach(entry -> stockpile.append(' ').append(entry.getKey())
							.append(": ").append(entry.getValue()).append(','));
			stockpile.setLength(stockpile.length() - 1);
			source.sendFeedback(() -> Text.literal(stockpile.toString()), false);
			if (state.getProbeCorePos() != null) {
				source.sendFeedback(() -> Text.literal("[Program 7] Probe core at "
						+ state.getProbeCorePos().toShortString()), false);
			}
		} else {
			source.sendFeedback(() -> Text.literal("[Program 7] No pod on the ground yet."), false);
		}

		ServerPlayerEntity player = source.getPlayer();
		if (player != null) {
			ProgramDirectorState.PlayerIntel intel = state.getIntel(player.getUuid());
			if (intel == null) {
				source.sendFeedback(() -> Text.literal("[Program 7] No intel file on you. Yet."), false);
			} else {
				source.sendFeedback(() -> Text.literal("[Program 7] Your file — risk tier " + intel.riskTier
						+ ", weapon profile: " + intel.weaponProfile
						+ ", elytra: " + intel.elytra
						+ ", deaths on record: " + intel.deaths), false);
			}
		}
		return state.getGlobalThreat();
	}

	private static int assessSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		ScanRecord record = RiskAssessment.assess(player);
		context.getSource().sendFeedback(() -> Text.literal("[Program 7] Live assessment — risk tier "
				+ record.riskTier()
				+ ", weapon profile: " + record.weaponProfile()
				+ ", elytra: " + record.elytra()
				+ ", deaths: " + record.deaths()), false);
		return record.riskTier();
	}

	private static int setThreat(CommandContext<ServerCommandSource> context) {
		int value = IntegerArgumentType.getInteger(context, "value");
		ProgramDirectorState.get(context.getSource().getWorld()).setGlobalThreat(value);
		context.getSource().sendFeedback(() -> Text.literal("[Program 7] Global threat set to " + value), true);
		return value;
	}

	private Program7Command() {
	}
}
