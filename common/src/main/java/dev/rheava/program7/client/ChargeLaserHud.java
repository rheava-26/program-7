package dev.rheava.program7.client;

import dev.rheava.program7.item.ChargeLaserItem;
import dev.rheava.program7.item.ChargeLaserState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

/**
 * A small heat bar over the hotbar, shown only while the charge laser is the
 * held item. Purely client-side: the laser's heat lives on the held stack's
 * {@code ChargeLaserState} data component, which already syncs to the client
 * as ordinary item data (same as {@code Damage}/durability), so this just
 * reads whichever hand holds it each frame — no network payload needed,
 * unlike {@link InterferenceOverlay} which tracks server-authoritative state
 * with nothing backing it on the client.
 */
public final class ChargeLaserHud {
	private static final int BAR_WIDTH = 80;
	private static final int BAR_HEIGHT = 3;

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}
		ItemStack stack = heldLaser(client);
		if (stack == null) {
			return;
		}
		ChargeLaserState state = ChargeLaserItem.getState(stack);
		float heatFraction = MathHelper.clamp(state.heatTicks() / (float) ChargeLaserItem.HEAT_MAX, 0.0f, 1.0f);

		int width = context.getScaledWindowWidth();
		int height = context.getScaledWindowHeight();
		int x = width / 2 - BAR_WIDTH / 2;
		int y = height - 32 - 13;

		context.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0x80000000);
		int filled = Math.round(BAR_WIDTH * heatFraction);
		if (filled > 0) {
			context.fill(x, y, x + filled, y + BAR_HEIGHT, heatColor(heatFraction, state.overheated()));
		}
	}

	/** Green -> amber -> red as heat climbs; flashes bright red while latched overheated. */
	private static int heatColor(float fraction, boolean overheated) {
		if (overheated) {
			return 0xFFFF3030;
		}
		int r = (int) (80 + 175 * fraction);
		int g = (int) (200 - 160 * fraction);
		int b = 40;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static ItemStack heldLaser(MinecraftClient client) {
		ItemStack main = client.player.getStackInHand(Hand.MAIN_HAND);
		if (main.getItem() instanceof ChargeLaserItem) {
			return main;
		}
		ItemStack off = client.player.getStackInHand(Hand.OFF_HAND);
		if (off.getItem() instanceof ChargeLaserItem) {
			return off;
		}
		return null;
	}

	private ChargeLaserHud() {
	}
}
