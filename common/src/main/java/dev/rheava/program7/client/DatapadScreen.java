package dev.rheava.program7.client;

import java.util.ArrayList;
import java.util.List;

import dev.architectury.networking.NetworkManager;
import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.network.DatapadRefreshPayload;
import dev.rheava.program7.network.DatapadSnapshotPayload;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/**
 * The Datapad v2 radar screen — a chunk-grid sweep of what the Program's
 * instrumentation can see, opened when the player reads the datapad and fed a
 * frozen {@link DatapadSnapshotPayload} snapshot by the server.
 *
 * <p>Left: a status rail (posture, heat, off-screen fleet estimate, escalation
 * tier, nearest-base bearing). Right: a north-up chunk grid centred on the
 * player. Every contact is a blip coloured by what it knows about <em>you</em>
 * — white/grey = hasn't noticed you, warming through amber to red = tracking
 * and engaging. A slow sweep line rakes the grid with a soft beep, the tension
 * device: between beeps a whole cluster of contacts can bloom into view at
 * once. Hover a blip for the datapad's read on it.
 *
 * <p>Deliberately built from plain HUD fills and the vanilla font — no
 * textures — so it stays crisp, blocky and cheap, and drops straight into any
 * resource pack.
 */
public final class DatapadScreen extends Screen {
	// Palette.
	private static final int PANEL_BG = 0xF00A0F16;
	private static final int PANEL_EDGE = 0xFF27E2D3;
	private static final int RAIL_DIVIDER = 0x4027E2D3;
	private static final int GRID_LINE = 0x2227E2D3;
	private static final int GRID_AXIS = 0x4427E2D3;
	private static final int SWEEP_COLOR = 0x9027E2D3;
	private static final int PLAYER_COLOR = 0xFF35E066;
	private static final int LABEL = 0xFF8CA0AE;
	private static final int VALUE = 0xFFD7E6F0;
	private static final int TITLE = 0xFF27E2D3;

	/** Blip colour by alert-ramp ordinal (0 UNAWARE .. 4 ENGAGING) — the "does it know about you" axis. */
	private static final int[] ALERT_COLORS = {
			0xFFBFC7D0, // UNAWARE  — cold white
			0xFFE8D24A, // SUSPICIOUS — amber
			0xFFE8892E, // SEARCHING — orange
			0xFFE83838, // TRACKING — red
			0xFFFF2020, // ENGAGING — hot red
	};
	/** A tracking-chip blip's own colour axis — deliberately outside the alert palette so it reads as "your intel," not a contact. */
	private static final int TRACKED_LIVE_COLOR = 0xFFFFD23F;
	private static final int TRACKED_LIVE_RING = 0xFFFFFFFF;
	/** Signal-lost tracked marker: dim, no bright ring — a last-known fix, not a live read. */
	private static final int TRACKED_LOST_COLOR = 0xFF5A6470;
	private static final String[] ALERT_LABELS = {
			"Unaware", "Suspicious", "Searching", "Tracking", "Engaging"};
	private static final String[] GUESS_LABELS = {
			"Unidentified", "Armed unit", "Recon unit", "Logistics unit"};
	private static final String[] COMPASS = {
			"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
			"S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

	/** Must match DatapadItem.RADAR_RANGE — the block reach the snapshot was gathered over. */
	private static final float RADAR_RANGE = 112.0f;
	private static final int PANEL_W = 306;
	private static final int PANEL_H = 190;
	private static final int RADAR_SIDE = 156;
	private static final long SWEEP_PERIOD_MS = 3000L;
	private static final long BEEP_INTERVAL_MS = 1500L;
	/** How often the open screen polls the server for a fresh read (client ticks). */
	private static final int REFRESH_INTERVAL_TICKS = 10;

	private DatapadSnapshotPayload snapshot;
	private long lastBeepMs;
	private int refreshCooldown = REFRESH_INTERVAL_TICKS;

	private DatapadScreen(DatapadSnapshotPayload snapshot) {
		super(Text.translatable("item.program7.datapad"));
		this.snapshot = snapshot;
		this.lastBeepMs = Util.getMeasuringTimeMs();
	}

	/**
	 * Open the datapad screen with a fresh snapshot — or, if it's already open,
	 * update it in place so a live-refresh reply doesn't tear down and rebuild
	 * the screen (which would reset the sweep and eat the player's hover).
	 */
	public static void open(DatapadSnapshotPayload snapshot) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen instanceof DatapadScreen existing) {
			existing.snapshot = snapshot;
		} else {
			client.setScreen(new DatapadScreen(snapshot));
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/**
	 * Vanilla {@link Screen#renderBackground(DrawContext, int, int, float)}
	 * gaussian-blurs and darkens whatever's behind the screen as of 1.20.3+.
	 * The datapad is a live HUD overlay the player keeps glancing through
	 * while still moving around, not a paused menu, so it should stay crisp
	 * — draw a single light translucent darkening ourselves instead of
	 * calling {@code super}, which would blur the world behind the panel.
	 */
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicker) {
		context.fill(0, 0, this.width, this.height, 0x66000000);
	}

	@Override
	public void tick() {
		// Poll the server for a live read a couple of times a second, so the
		// radar stays current while it's open — contacts drift, alerts warm.
		if (--this.refreshCooldown <= 0) {
			this.refreshCooldown = REFRESH_INTERVAL_TICKS;
			NetworkManager.sendToServer(DatapadRefreshPayload.INSTANCE);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);

		int px = (this.width - PANEL_W) / 2;
		int py = (this.height - PANEL_H) / 2;

		// Panel.
		context.fill(px, py, px + PANEL_W, py + PANEL_H, PANEL_BG);
		context.drawBorder(px, py, PANEL_W, PANEL_H, PANEL_EDGE);

		// Title bar.
		context.drawTextWithShadow(this.textRenderer,
				Text.literal("▮ PROGRAM DATAPAD ▮").formatted(Formatting.BOLD),
				px + 10, py + 8, TITLE);
		context.drawHorizontalLine(px + 8, px + PANEL_W - 9, py + 20, RAIL_DIVIDER);

		int radarX = px + PANEL_W - RADAR_SIDE - 10;
		int radarY = py + 26;
		drawRail(context, px + 10, py + 28, radarX - 6);
		drawRadar(context, radarX, radarY, mouseX, mouseY);

		context.drawTextWithShadow(this.textRenderer,
				Text.literal("[esc] / [right-click] disconnect").formatted(Formatting.DARK_GRAY),
				px + 10, py + PANEL_H - 13, 0xFF56636E);

		beep();
		super.render(context, mouseX, mouseY, delta);
	}

	/** Right-click also disconnects, in addition to vanilla's default escape-closes-screen handling. */
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 1) {
			this.close();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void drawRail(DrawContext context, int x, int y, int railRight) {
		DatapadSnapshotPayload.Header h = this.snapshot.header();
		int line = y;

		context.drawTextWithShadow(this.textRenderer, Text.literal("POSTURE"), x, line, LABEL);
		context.drawTextWithShadow(this.textRenderer,
				Text.literal(h.posture()).formatted(postureColor(h.posture())), x, line + 10, VALUE);
		line += 26;

		// Heat bar.
		context.drawTextWithShadow(this.textRenderer,
				Text.literal("HEAT  " + h.heat() + "/" + ProgramDirectorState.MAX_THREAT), x, line, LABEL);
		int barY = line + 11;
		int barW = railRight - x;
		context.fill(x, barY, x + barW, barY + 4, 0xFF1B2530);
		float frac = MathHelper.clamp(h.heat() / (float) ProgramDirectorState.MAX_THREAT, 0f, 1f);
		int fillW = (int) (barW * frac);
		context.fill(x, barY, x + fillW, barY + 4, heatColor(frac));
		line += 26;

		context.drawTextWithShadow(this.textRenderer, Text.literal("FLEET (est.)"), x, line, LABEL);
		context.drawTextWithShadow(this.textRenderer,
				Text.literal(h.fleetEstimate() + " off-grid  ·  " + this.snapshot.contacts().size() + " in range"),
				x, line + 10, VALUE);
		line += 26;

		context.drawTextWithShadow(this.textRenderer, Text.literal("ESCALATION"), x, line, LABEL);
		context.drawTextWithShadow(this.textRenderer,
				Text.literal("Tier " + h.tier()).formatted(h.tier() >= 3 ? Formatting.RED
						: h.tier() == 2 ? Formatting.GOLD : Formatting.GREEN),
				x, line + 10, VALUE);
		line += 26;

		context.drawTextWithShadow(this.textRenderer, Text.literal("NEAREST BASE"), x, line, LABEL);
		String baseText = h.baseDistance() < 0 || Float.isNaN(h.baseYaw())
				? "— not located"
				: compass(h.baseYaw()) + "  ~" + h.baseDistance() + "m";
		context.drawTextWithShadow(this.textRenderer, Text.literal(baseText), x, line + 10, VALUE);
		line += 26;

		// Inbound-artillery telegraph (§5/§9): only shown when a shell is in the
		// air toward the player, and blinked so it reads as an alarm, not a stat.
		DatapadSnapshotPayload.Incoming inc = this.snapshot.incoming();
		if (inc.present()) {
			boolean blink = (Util.getMeasuringTimeMs() / 400L) % 2L == 0L;
			context.drawTextWithShadow(this.textRenderer,
					Text.literal("⚠ INCOMING FIRE").formatted(Formatting.BOLD)
							.formatted(blink ? Formatting.RED : Formatting.GOLD),
					x, line, blink ? 0xFFFF2020 : 0xFFE8A32E);
			context.drawTextWithShadow(this.textRenderer,
					Text.literal("from " + compass(inc.bearing()) + "  ·  ~" + inc.etaSeconds() + "s"),
					x, line + 10, VALUE);
		}
	}

	private void drawRadar(DrawContext context, int rx, int ry, int mouseX, int mouseY) {
		int side = RADAR_SIDE;
		int cx = rx + side / 2;
		int cy = ry + side / 2;
		float scale = (side / 2f) / RADAR_RANGE;

		// Scope backdrop + frame.
		context.fill(rx, ry, rx + side, ry + side, 0xFF060B11);
		context.drawBorder(rx, ry, side, side, GRID_AXIS);

		// Chunk gridlines (every 16 blocks).
		float step = 16f * scale;
		for (float d = step; d < side / 2f; d += step) {
			int off = Math.round(d);
			context.drawVerticalLine(cx - off, ry, ry + side - 1, GRID_LINE);
			context.drawVerticalLine(cx + off, ry, ry + side - 1, GRID_LINE);
			context.drawHorizontalLine(rx, rx + side - 1, cy - off, GRID_LINE);
			context.drawHorizontalLine(rx, rx + side - 1, cy + off, GRID_LINE);
		}
		// Centre axes + N marker.
		context.drawVerticalLine(cx, ry, ry + side - 1, GRID_AXIS);
		context.drawHorizontalLine(rx, rx + side - 1, cy, GRID_AXIS);
		context.drawTextWithShadow(this.textRenderer, Text.literal("N"), cx - 2, ry + 2, 0xFF6FA8C0);

		// Sweep line.
		float sweepDeg = (Util.getMeasuringTimeMs() % SWEEP_PERIOD_MS) / (float) SWEEP_PERIOD_MS * 360f;
		double rad = Math.toRadians(sweepDeg);
		double sx = Math.sin(rad);
		double sz = -Math.cos(rad);
		for (int r = 0; r < side / 2; r++) {
			int lx = cx + (int) Math.round(sx * r);
			int ly = cy + (int) Math.round(sz * r);
			context.fill(lx, ly, lx + 1, ly + 1, SWEEP_COLOR);
		}

		// Contacts.
		DatapadSnapshotPayload.Contact hovered = null;
		int hx = 0;
		int hy = 0;
		for (DatapadSnapshotPayload.Contact c : this.snapshot.contacts()) {
			int bx = cx + Math.round(c.relX() * scale);
			int by = cy + Math.round(c.relZ() * scale);
			bx = MathHelper.clamp(bx, rx + 1, rx + side - 3);
			by = MathHelper.clamp(by, ry + 1, ry + side - 3);
			int color = ALERT_COLORS[MathHelper.clamp(c.alert(), 0, ALERT_COLORS.length - 1)];
			context.fill(bx, by, bx + 3, by + 3, color);
			if (mouseX >= bx - 2 && mouseX <= bx + 5 && mouseY >= by - 2 && mouseY <= by + 5) {
				hovered = c;
				hx = bx;
				hy = by;
			}
		}

		// Tracked blips — the player's own tracking-chip intel, exact and not
		// range/fog-limited like a Contact above (see DatapadItem#trackedFor),
		// so they're drawn as a distinct bright diamond-with-ring "pin" instead
		// of a plain dot, and can land clamped to the scope's edge if the mark
		// is out of the radar's normal range. A lost signal (see
		// DatapadSnapshotPayload.Tracked#live) still plots its last-known fix,
		// just greyed and without the bright ring, mirroring the tracking
		// chip's own "signal lost" read instead of vanishing outright.
		DatapadSnapshotPayload.Tracked hoveredTracked = null;
		int thx = 0;
		int thy = 0;
		for (DatapadSnapshotPayload.Tracked t : this.snapshot.tracked()) {
			int bx = cx + Math.round(t.relX() * scale);
			int by = cy + Math.round(t.relZ() * scale);
			bx = MathHelper.clamp(bx, rx + 2, rx + side - 4);
			by = MathHelper.clamp(by, ry + 2, ry + side - 4);
			int color = t.live() ? TRACKED_LIVE_COLOR : TRACKED_LOST_COLOR;

			// Diamond pin, bigger than a plain contact dot.
			context.fill(bx, by - 3, bx + 1, by + 4, color);
			context.fill(bx - 1, by - 2, bx + 2, by + 3, color);
			context.fill(bx - 2, by - 1, bx + 3, by + 2, color);
			if (t.live()) {
				context.drawBorder(bx - 3, by - 4, 7, 7, TRACKED_LIVE_RING);
			}

			if (mouseX >= bx - 3 && mouseX <= bx + 4 && mouseY >= by - 4 && mouseY <= by + 5) {
				hoveredTracked = t;
				thx = bx;
				thy = by;
			}
		}

		// Player, always on top, dead centre.
		context.fill(cx - 2, cy - 2, cx + 2, cy + 2, PLAYER_COLOR);

		// Tracked-blip tooltip wins over a regular contact's if both are
		// hovered — it's drawn on top, so it should win the hover too.
		if (hoveredTracked != null) {
			context.drawTooltip(this.textRenderer, trackedTooltip(hoveredTracked), thx, thy);
		} else if (hovered != null) {
			context.drawTooltip(this.textRenderer, contactTooltip(hovered), hx, hy);
		}
	}

	private static List<Text> contactTooltip(DatapadSnapshotPayload.Contact c) {
		List<Text> lines = new ArrayList<>();
		lines.add(Text.literal("Contact").formatted(Formatting.AQUA));
		lines.add(Text.literal("Likely: " + GUESS_LABELS[MathHelper.clamp(c.guess(), 0, GUESS_LABELS.length - 1)])
				.formatted(Formatting.GRAY));
		int alert = MathHelper.clamp(c.alert(), 0, ALERT_LABELS.length - 1);
		lines.add(Text.literal("Alert: " + ALERT_LABELS[alert]).formatted(alert >= 3 ? Formatting.RED
				: alert >= 1 ? Formatting.GOLD : Formatting.GRAY));
		String sees = alert >= 3 ? "yes" : alert == 2 ? "closing in" : alert == 1 ? "not yet" : "no";
		lines.add(Text.literal("Has your position: " + sees).formatted(Formatting.DARK_GRAY));
		int dist = Math.round(MathHelper.sqrt(c.relX() * c.relX() + c.relZ() * c.relZ()));
		lines.add(Text.literal("Range: ~" + dist + "m").formatted(Formatting.DARK_GRAY));
		return lines;
	}

	private static List<Text> trackedTooltip(DatapadSnapshotPayload.Tracked t) {
		List<Text> lines = new ArrayList<>();
		lines.add(Text.literal("Tracked target").formatted(Formatting.GOLD));
		lines.add(Text.literal(t.live() ? "Signal: live" : "Signal: lost (last known)")
				.formatted(t.live() ? Formatting.GREEN : Formatting.GRAY));
		int dist = Math.round(MathHelper.sqrt(t.relX() * t.relX() + t.relZ() * t.relZ()));
		lines.add(Text.literal("Range: ~" + dist + "m").formatted(Formatting.DARK_GRAY));
		return lines;
	}

	private void beep() {
		long now = Util.getMeasuringTimeMs();
		if (now - this.lastBeepMs >= BEEP_INTERVAL_MS) {
			this.lastBeepMs = now;
			MinecraftClient.getInstance().getSoundManager().play(
					PositionedSoundInstance.master(P7Sounds.DRONE_SCAN_BEEP.get(), 1.4f, 0.35f));
		}
	}

	private static String compass(float yaw) {
		float a = MathHelper.wrapDegrees(yaw);
		if (a < 0) {
			a += 360f;
		}
		return COMPASS[Math.round(a / 22.5f) % 16];
	}

	private static Formatting postureColor(String posture) {
		return switch (posture) {
			case "HUNTING" -> Formatting.RED;
			case "ACTIVE" -> Formatting.GOLD;
			case "WATCHFUL" -> Formatting.YELLOW;
			case "DORMANT" -> Formatting.AQUA;
			case "NEUTRAL" -> Formatting.GREEN;
			default -> Formatting.WHITE;
		};
	}

	private static int heatColor(float frac) {
		if (frac >= 0.6f) {
			return 0xFFE83838;
		}
		if (frac >= 0.25f) {
			return 0xFFE8A32E;
		}
		return 0xFF35C0E0;
	}
}
