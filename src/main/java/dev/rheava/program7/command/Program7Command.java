package dev.rheava.program7.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.director.RiskAssessment;
import dev.rheava.program7.director.ScanRecord;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
 * </pre>
 */
public final class Program7Command {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("program7")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.literal("status").executes(Program7Command::status))
						.then(CommandManager.literal("assess").executes(Program7Command::assessSelf))
						.then(CommandManager.literal("threat")
								.then(CommandManager.argument("value", IntegerArgumentType.integer(0, 100))
										.executes(Program7Command::setThreat)))));
	}

	private static int status(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ProgramDirectorState state = ProgramDirectorState.get(source.getWorld());
		source.sendFeedback(() -> Text.literal("[Program 7] Global threat: " + state.getGlobalThreat()
				+ "/" + ProgramDirectorState.MAX_THREAT
				+ " — scans completed: " + state.getScansCompleted()), false);

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
