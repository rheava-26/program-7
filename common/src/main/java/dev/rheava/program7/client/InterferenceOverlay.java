package dev.rheava.program7.client;

import dev.rheava.program7.Program7;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/**
 * The psionic interference HUD: a violet hue-shift creeping in from the
 * screen edges whenever Program hardware is within ~3 chunks. A faint
 * flicker keeps it alive; intensity (fed by the server) sets how far it
 * reaches and how hard it presses. With the directionality config enabled,
 * the edge facing the strongest threat glows hotter.
 *
 * <p>Deliberately built from plain HUD fills — no shaders — so it composes
 * with anything and costs nothing.
 */
public final class InterferenceOverlay {
	// Psionic violet.
	private static final int RED = 140;
	private static final int GREEN = 30;
	private static final int BLUE = 220;
	private static final int SIDE_STRIPS = 12;

	private static float target = 0.0f;
	private static float current = 0.0f;
	private static float threatYaw = Float.NaN;
	private static long lastPacketMs = 0L;

	// Audio focus: once the interference presses in past this, vanilla music
	// drops out so the whir of nearby drones and the crack of distant gunfire
	// push to the front of the mix — the Program's sound *becomes* the score.
	private static final float MUSIC_DUCK_THRESHOLD = 0.10f;
	private static boolean musicDucked = false;
	private static int musicReStopTicks = 0;

	public static void onPacket(float intensity, float yaw) {
		target = MathHelper.clamp(intensity, 0.0f, 1.0f);
		threatYaw = yaw;
		lastPacketMs = Util.getMeasuringTimeMs();
	}

	public static void clientTick(MinecraftClient client) {
		// If the server goes quiet (dimension change, drones gone), fade out.
		if (Util.getMeasuringTimeMs() - lastPacketMs > 3000L) {
			target = 0.0f;
		}
		current += (target - current) * 0.08f;
		if (current < 0.003f) {
			current = 0.0f;
		}

		// Music duck: while the interference presses in, silence vanilla music so
		// the Program's own sound — drone whir, distant gunfire — carries the
		// scene. Re-issued every ~30 ticks rather than every tick so a track that
		// tries to start back up is caught without stuttering the audio.
		if (Program7.CONFIG.interferenceMusicDuck && current > MUSIC_DUCK_THRESHOLD && client.player != null) {
			if (!musicDucked || --musicReStopTicks <= 0) {
				client.getMusicTracker().stop();
				musicReStopTicks = 30;
			}
			musicDucked = true;
		} else {
			musicDucked = false;
		}

		// The audio half of the warning: bursts of static, denser as it gets worse.
		if (current > 0.12f && client.player != null && client.world != null
				&& client.world.random.nextFloat() < current * 0.04f) {
			client.getSoundManager().play(PositionedSoundInstance.master(
					P7Sounds.DRONE_INTERFERENCE.get(),
					0.6f + client.world.random.nextFloat() * 0.8f,
					0.1f + current * 0.3f));
		}
	}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		if (current <= 0.004f || !Program7.CONFIG.interferenceOverlay) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}

		int width = context.getScaledWindowWidth();
		int height = context.getScaledWindowHeight();

		long time = Util.getMeasuringTimeMs();
		float flicker = 0.82f + 0.18f * MathHelper.sin(time / 90.0f) * MathHelper.sin(time / 37.0f);
		float strength = current * flicker;

		float topBias = 1.0f;
		float bottomBias = 1.0f;
		float leftBias = 1.0f;
		float rightBias = 1.0f;
		if (Program7.CONFIG.interferenceDirectionality && !Float.isNaN(threatYaw)) {
			float relative = MathHelper.wrapDegrees(threatYaw - client.player.getYaw());
			topBias = edgeBias(relative, 0.0f);      // threat ahead
			rightBias = edgeBias(relative, 90.0f);   // threat to the right
			bottomBias = edgeBias(relative, 180.0f); // threat behind
			leftBias = edgeBias(relative, -90.0f);   // threat to the left
		}

		int maxAlpha = (int) (strength * 150.0f);
		int verticalReach = (int) (height * (0.10f + 0.16f * strength));
		int horizontalReach = (int) (width * (0.07f + 0.12f * strength));

		context.fillGradient(0, 0, width, verticalReach,
				color((int) (maxAlpha * topBias)), color(0));
		context.fillGradient(0, height - verticalReach, width, height,
				color(0), color((int) (maxAlpha * bottomBias)));

		// fillGradient only runs vertically, so the side fades are stepped strips.
		int stripWidth = Math.max(1, horizontalReach / SIDE_STRIPS);
		for (int i = 0; i < SIDE_STRIPS; i++) {
			float fade = 1.0f - i / (float) SIDE_STRIPS;
			int alpha = (int) (maxAlpha * fade * fade);
			context.fill(i * stripWidth, 0, (i + 1) * stripWidth, height,
					color((int) (alpha * leftBias)));
			context.fill(width - (i + 1) * stripWidth, 0, width - i * stripWidth, height,
					color((int) (alpha * rightBias)));
		}
	}

	/** 0.5..1.5 multiplier, peaking when the threat sits on this edge's heading. */
	private static float edgeBias(float relativeYaw, float edgeCenter) {
		float aligned = MathHelper.cos((relativeYaw - edgeCenter) * ((float) Math.PI / 180.0f));
		return 0.5f + Math.max(0.0f, aligned);
	}

	private static int color(int alpha) {
		return (MathHelper.clamp(alpha, 0, 255) << 24) | (RED << 16) | (GREEN << 8) | BLUE;
	}

	private InterferenceOverlay() {
	}
}
