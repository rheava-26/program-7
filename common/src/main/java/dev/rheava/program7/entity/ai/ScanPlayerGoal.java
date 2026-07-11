package dev.rheava.program7.entity.ai;

import java.util.EnumSet;

import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.director.RiskAssessment;
import dev.rheava.program7.director.ScanRecord;
import dev.rheava.program7.entity.SurveyorDroneEntity;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import org.jetbrains.annotations.Nullable;

/**
 * Approach the nearest player, hold at standoff range, and sweep them with an
 * escalating scan. The beeping accelerates as the scan completes — that is the
 * player's window to break line of sight or shoot the drone down before their
 * threat profile reaches the Director.
 */
public class ScanPlayerGoal extends Goal {
	private static final double DETECTION_RANGE = 24.0;
	private static final double HOLD_DISTANCE = 5.0;
	private static final double SCAN_PROGRESS_RANGE = 10.0;
	private static final int SCAN_DURATION = 70;
	private static final int SCAN_COOLDOWN = 400;

	private final SurveyorDroneEntity drone;
	@Nullable
	private PlayerEntity target;
	private int scanTicks;

	public ScanPlayerGoal(SurveyorDroneEntity drone) {
		this.drone = drone;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		if (!this.drone.isScanReady() || this.drone.isRetreating() || this.drone.isScrambled()) {
			return false;
		}
		PlayerEntity player = this.drone.getWorld().getClosestPlayer(this.drone, DETECTION_RANGE);
		if (player == null || player.isSpectator() || player.isCreative() || !this.drone.canSee(player)) {
			return false;
		}
		this.target = player;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return this.target != null
				&& this.target.isAlive()
				&& !this.target.isSpectator()
				&& !this.drone.isRetreating()
				&& this.drone.squaredDistanceTo(this.target) < 32.0 * 32.0
				&& this.scanTicks < SCAN_DURATION;
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		this.scanTicks = 0;
		if (this.target != null && this.drone.getWorld() instanceof ServerWorld world
				&& SpottedAlertCoordinator.tryAnnounceSpotted(world, this.target)) {
			// First spotter on this player within the cooldown window: full shrill cue.
			this.drone.playSound(P7Sounds.DRONE_ALERT.get(), 1.0f, 1.0f);
		} else {
			// Already announced very recently by something else — just layer the hum.
			this.drone.playSound(P7Sounds.DRONE_INTERFERENCE.get(), 0.4f, 1.2f);
		}
	}

	@Override
	public void tick() {
		if (this.target == null) {
			return;
		}
		this.drone.getLookControl().lookAt(this.target, 30.0f, 30.0f);

		double distanceSq = this.drone.squaredDistanceTo(this.target);
		if (distanceSq > HOLD_DISTANCE * HOLD_DISTANCE) {
			this.drone.getNavigation().startMovingTo(
					this.target.getX(), this.target.getEyeY() + 2.0, this.target.getZ(), 1.2);
		} else {
			this.drone.getNavigation().stop();
		}

		if (distanceSq <= SCAN_PROGRESS_RANGE * SCAN_PROGRESS_RANGE && this.drone.canSee(this.target)) {
			this.scanTicks++;

			int beepInterval = Math.max(4, 16 - this.scanTicks / 5);
			if (this.scanTicks % beepInterval == 0) {
				float pitch = 0.8f + (this.scanTicks / (float) SCAN_DURATION) * 0.8f;
				this.drone.playSound(P7Sounds.DRONE_SCAN_BEEP.get(), 0.8f, pitch);
			}

			if (this.drone.getWorld() instanceof ServerWorld serverWorld) {
				serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
						this.target.getX(), this.target.getBodyY(0.5), this.target.getZ(),
						2, 0.3, 0.6, 0.3, 0.0);
			}

			if (this.scanTicks >= SCAN_DURATION) {
				this.completeScan();
			}
		}
	}

	private void completeScan() {
		if (this.target instanceof ServerPlayerEntity player) {
			ScanRecord record = RiskAssessment.assess(player);
			ProgramDirectorState.get(player.getServerWorld()).recordScan(player, record);

			// Psionic interference: the drone burns its findings back up the
			// link and the player's senses catch the edge of the transmission.
			// Show, don't tell: no action-bar text, just a sensory spike — the
			// player should FEEL that they've been made, not read it.
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, false, false));
			// The interference hum always layers in, completed scan or not —
			// it's the ambient "you're being watched" texture, not the shrill
			// one-shot cue, so it never needs gating.
			player.getServerWorld().playSound(null, player.getBlockPos(),
					P7Sounds.DRONE_INTERFERENCE.get(), SoundCategory.HOSTILE, 1.0f, 1.0f);
			// The sting + "more are coming" horn are the shrill "you've been
			// made" beat — only the first scan to complete on an already-spotted
			// player within the cooldown window gets to play these; a second
			// surveyor finishing its scan a moment later doesn't restack them.
			if (SpottedAlertCoordinator.tryAnnounceSpotted(player.getServerWorld(), player)) {
				player.getServerWorld().playSound(null, player.getBlockPos(),
						P7Sounds.SCAN_STING.get(), SoundCategory.HOSTILE, 1.4f, 1.0f);
				// A beat later, in-fiction: the sound of drones now inbound on the
				// player's marked position.
				player.getServerWorld().playSound(null, player.getBlockPos(),
						P7Sounds.DRONES_INBOUND.get(), SoundCategory.HOSTILE, 1.0f, 1.0f);
			}
		}
		this.drone.setScanCooldown(SCAN_COOLDOWN);
		this.drone.beginRetreat(this.target, 160);
	}

	@Override
	public void stop() {
		this.target = null;
		this.scanTicks = 0;
		this.drone.getNavigation().stop();
	}
}
