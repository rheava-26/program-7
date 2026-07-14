package dev.rheava.program7.entity;

import dev.rheava.program7.Program7;
import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * Shared bones for every indirect-fire munition — the artillery doc's §8
 * "shells themselves generalize {@code MortarShellEntity} into a small
 * family (arc/spread/payload parameters) rather than a new class per type."
 * {@link MortarShellEntity} and {@link HowitzerShellEntity} were the first
 * two, hand-duplicated; this factors out what they actually shared (the
 * smoke trail, the falling whistle, and the impact-sound-then-explosion
 * sequence) so every artillery family added after them — rocket, missile,
 * naval, bomb — is a subclass with a handful of tuned numbers, not a
 * copy-pasted class.
 *
 * <p>Like its two originals, this is not a living thing: {@link ThrownEntity}
 * fits because a shell has no health, no AI, and no wreck — it either flies
 * or it has already gone off. {@link ThrownEntity} itself already bakes in
 * the 1%/tick horizontal drag every subclass inherits (see
 * {@code dev.rheava.program7.entity.ai.BallisticSolver}, which mirrors that
 * exact constant for the ballistic solve); this base only owns gravity and
 * presentation.
 */
public abstract class AbstractShellEntity extends ThrownEntity {
	protected AbstractShellEntity(EntityType<? extends AbstractShellEntity> entityType, World world) {
		super(entityType, world);
	}

	protected AbstractShellEntity(EntityType<? extends AbstractShellEntity> entityType, LivingEntity owner,
			World world) {
		super(entityType, owner, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		// No synced payload beyond position/velocity — nothing to track.
	}

	@Override
	protected final double getGravity() {
		return this.shellGravity();
	}

	/** How hard this munition falls — the mortar's floaty 0.06, the howitzer's floatier 0.045, a rocket's flatter/faster arc, etc. */
	protected abstract double shellGravity();

	@Override
	public void tick() {
		super.tick();
		if (this.getWorld() instanceof ServerWorld world) {
			this.spawnFlightParticles(world);
			// Whistling only starts once it's arced over and is coming down —
			// the falling whistle is the player's warning to move.
			if (this.getVelocity().y < 0.0 && this.age % this.whistleIntervalTicks() == 0) {
				this.playSound(this.whistleSound(), this.whistleVolume(), this.whistlePitch());
			}
		}
	}

	/** The smoke trail while airborne; overridable for a bigger/smaller/differently-colored puff per family. */
	protected void spawnFlightParticles(ServerWorld world) {
		world.spawnParticles(this.smokeParticleType(), this.getX(), this.getY(), this.getZ(),
				this.smokeParticleCount(), 0.0, 0.0, 0.0, 0.0);
	}

	protected ParticleEffect smokeParticleType() {
		return ParticleTypes.SMOKE;
	}

	protected int smokeParticleCount() {
		return 1;
	}

	protected int whistleIntervalTicks() {
		return 10;
	}

	protected SoundEvent whistleSound() {
		return P7Sounds.MORTAR_WHISTLE.get();
	}

	protected abstract float whistleVolume();

	protected abstract float whistlePitch();

	@Override
	protected final void onCollision(HitResult hitResult) {
		if (this.getWorld() instanceof ServerWorld world) {
			this.onImpact(world, hitResult);
		}
	}

	/**
	 * The impact sequence: report + explosion, then discard. Subclasses that
	 * need something extra on top (a guided missile's proximity fizzle, a
	 * bomb stick's staggered detonations) override this rather than
	 * {@code onCollision} directly, since the world-instanceof guard stays
	 * shared here.
	 */
	protected void onImpact(ServerWorld world, HitResult hitResult) {
		ProgramAcoustics.emit(world, this.getPos(), this.impactSound(), SoundCategory.HOSTILE,
				this.impactVolume(), this.impactPitch());
		World.ExplosionSourceType sourceType = this.usesTerrainDestruction() && Program7.CONFIG.terrainDestruction
				? World.ExplosionSourceType.MOB
				: World.ExplosionSourceType.NONE;
		world.createExplosion(this, this.getX(), this.getY(), this.getZ(), this.explosionPower(), sourceType);
		this.discard();
	}

	/** The warhead's blast radius/power handed to {@code World#createExplosion}. */
	protected abstract float explosionPower();

	/**
	 * Whether this munition is heavy enough to actually chew block terrain
	 * (gated by {@code mobGriefing} and the {@code terrainDestruction}
	 * config switch either way) — the mortar's light shell stays damage-only,
	 * the howitzer's "wall-breaker" round does not. {@code false} by default.
	 */
	protected boolean usesTerrainDestruction() {
		return false;
	}

	protected abstract SoundEvent impactSound();

	protected abstract float impactVolume();

	protected abstract float impactPitch();
}
