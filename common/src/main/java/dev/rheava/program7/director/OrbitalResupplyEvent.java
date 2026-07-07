package dev.rheava.program7.director;

import java.util.Map;

import dev.architectury.networking.NetworkManager;
import dev.rheava.program7.network.InterferencePayload;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * The orbital resupply: a periodic capsule from orbit that tops up the
 * Director's ledger at an established base. Per the design bible it is
 * LOUD, BRIGHT, and spikes the psionic interference — the player should
 * always know it happened and roughly where, even from a distance. Bases
 * left standing compound their resupplies; razing the core before it fires
 * again is the counterplay.
 */
public final class OrbitalResupplyEvent {
	/** Half a starter stockpile — enough to matter, not enough to double the pod. */
	private static final Map<String, Integer> RESUPPLY_STOCKPILE = Map.of(
			Resources.IRON, 32, Resources.COPPER, 24, Resources.REDSTONE, 16,
			Resources.COAL, 32, Resources.GUNPOWDER, 16);

	/** Players this close feel the psionic spike, regardless of line of sight. */
	private static final double INTERFERENCE_RANGE = 128.0;
	private static final float INTERFERENCE_INTENSITY = 1.0f;

	public static void runAt(ServerWorld world, BlockPos core) {
		playFx(world, core);

		ProgramDirectorState state = ProgramDirectorState.get(world);
		RESUPPLY_STOCKPILE.forEach(state::addResource);

		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.getBlockPos().isWithinDistance(core, INTERFERENCE_RANGE)) {
				double dx = core.getX() - player.getX();
				double dz = core.getZ() - player.getZ();
				float threatYaw = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
				NetworkManager.sendToPlayer(player, new InterferencePayload(INTERFERENCE_INTENSITY, threatYaw));
			}
		}
	}

	private static void playFx(ServerWorld world, BlockPos core) {
		world.playSound(null, core, P7Sounds.ORBITAL_RESUPPLY.get(), SoundCategory.AMBIENT, 4.0f, 0.8f);

		double x = core.getX() + 0.5;
		double z = core.getZ() + 0.5;
		// A bright vertical column marking the descent path, a few particles per y-level.
		for (int y = 2; y <= 40; y += 2) {
			world.spawnParticles(ParticleTypes.END_ROD, x, core.getY() + y, z, 3, 0.3, 0.3, 0.3, 0.01);
		}
		// A flash-like burst up top where the capsule "arrives".
		world.spawnParticles(ParticleTypes.FIREWORK, x, core.getY() + 30, z, 20, 1.0, 1.0, 1.0, 0.05);
		// Lingering smoke at the core itself, echoing the drop pod's impact FX.
		world.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, core.getY() + 1.0, z, 40, 1.5, 1.0, 1.5, 0.01);
	}

	private OrbitalResupplyEvent() {
	}
}
