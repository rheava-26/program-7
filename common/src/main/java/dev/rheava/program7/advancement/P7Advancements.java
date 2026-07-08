package dev.rheava.program7.advancement;

import dev.rheava.program7.Program7;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Small helper around the grant-from-code pattern already used for the
 * {@code seven_days} advancement (see {@link
 * dev.rheava.program7.director.ProgramDirectorState#tick}): every
 * gameplay-triggered advancement in this mod uses a
 * {@code minecraft:impossible} criterion named {@code "unlocked"} and is
 * granted directly from code instead of a datapack-satisfiable trigger.
 */
public final class P7Advancements {
	private P7Advancements() {
	}

	/** Grant the {@code "unlocked"} criterion of {@code program7:<id>} to a player, if it exists. */
	public static void grant(ServerPlayerEntity player, String id) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		AdvancementEntry adv = server.getAdvancementLoader().get(Program7.id(id));
		if (adv != null) {
			player.getAdvancementTracker().grantCriterion(adv, "unlocked");
		}
	}
}
