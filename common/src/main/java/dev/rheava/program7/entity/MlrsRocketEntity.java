package dev.rheava.program7.entity;

import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/**
 * One rocket out of an MLRS ripple (see {@link MlrsLauncherEntity} /
 * {@code dev.rheava.program7.entity.ai.MlrsAttackGoal}). Lighter-warhead and
 * faster/flatter than the howitzer's shell — the doc's "cheap, inaccurate
 * end of the munition axis: area denial, not precision" — a single rocket
 * does modest damage; the ripple of several landing together is the point,
 * not any one round's power. Unlike the howitzer's wall-breaker, it doesn't
 * chew terrain — suppression and saturation, not demolition.
 */
public class MlrsRocketEntity extends AbstractShellEntity {
	/** Flatter/faster than the howitzer's floaty arc — a rocket motor, not a lobbed shell. */
	private static final double GRAVITY = 0.05;
	private static final float EXPLOSION_POWER = 1.6f;

	public MlrsRocketEntity(EntityType<? extends MlrsRocketEntity> entityType, World world) {
		super(entityType, world);
	}

	public MlrsRocketEntity(World world, LivingEntity owner) {
		super(P7Entities.MLRS_ROCKET.get(), owner, world);
	}

	@Override
	protected double shellGravity() {
		return GRAVITY;
	}

	@Override
	protected ParticleEffect smokeParticleType() {
		// A brighter exhaust trail than a lobbed shell's smoke puff — reads
		// distinctly as a rocket motor burning, not an arcing shell.
		return ParticleTypes.SMALL_FLAME;
	}

	@Override
	protected int smokeParticleCount() {
		return 2;
	}

	@Override
	protected int whistleIntervalTicks() {
		// Rockets are faster and shorter-lived in the air than a lobbed
		// shell — a tighter whistle cadence so the cue still lands more than
		// once or twice before impact.
		return 6;
	}

	@Override
	protected float whistleVolume() {
		return 1.1f;
	}

	@Override
	protected float whistlePitch() {
		return 1.3f;
	}

	@Override
	protected float explosionPower() {
		return EXPLOSION_POWER;
	}

	@Override
	protected SoundEvent impactSound() {
		return P7Sounds.MORTAR_IMPACT.get();
	}

	@Override
	protected float impactVolume() {
		return 1.1f;
	}

	@Override
	protected float impactPitch() {
		return 1.2f;
	}
}
