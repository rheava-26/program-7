package dev.rheava.program7.entity;

import dev.rheava.program7.Program7;
import dev.rheava.program7.audio.ProgramAcoustics;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * The howitzer's shell: the same "not a living thing, just flies and
 * detonates" shape as {@link MortarShellEntity} — {@link ThrownEntity} for
 * the same reasons (no health, no AI, no wreck) — but heavier and floatier
 * (a longer hang for the longer standoff range) with a much bigger warhead.
 *
 * <p>Unlike the mortar's shell, this one actually breaks cover on impact —
 * the artillery doc's "wall-breaker." Block destruction reuses the exact
 * config+gamerule-gated {@code World.ExplosionSourceType} pattern
 * {@link AttackDroneEntity} already uses for its own detonation: {@code MOB}
 * (which itself only affects blocks when the {@code mobGriefing} gamerule is
 * on) when {@link Program7#CONFIG}'s {@code terrainDestruction} switch is on,
 * {@code NONE} (damage-only, no block interaction at all) otherwise.
 */
public class HowitzerShellEntity extends ThrownEntity {
	/** Floatier than the mortar's 0.06 — a longer hang time for the longer standoff range. */
	private static final double GRAVITY = 0.045;
	/** Well above the mortar's 1.8f: this is the round meant to actually crack a wall. */
	private static final float EXPLOSION_POWER = 3.4f;

	public HowitzerShellEntity(EntityType<? extends HowitzerShellEntity> entityType, World world) {
		super(entityType, world);
	}

	public HowitzerShellEntity(World world, LivingEntity owner) {
		super(P7Entities.HOWITZER_SHELL.get(), owner, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		// No synced payload beyond position/velocity — nothing to track.
	}

	@Override
	protected double getGravity() {
		return GRAVITY;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(),
					2, 0.0, 0.0, 0.0, 0.0);
			// Whistling only starts once it's arced over and is coming down —
			// same warning shape as the mortar, just deeper/louder for the
			// bigger round.
			if (this.getVelocity().y < 0.0 && this.age % 10 == 0) {
				this.playSound(P7Sounds.MORTAR_WHISTLE.get(), 1.6f, 0.75f);
			}
		}
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		if (this.getWorld() instanceof ServerWorld world) {
			ProgramAcoustics.emit(world, this.getPos(), P7Sounds.MORTAR_IMPACT.get(),
					SoundCategory.HOSTILE, 1.6f, 0.7f);
			World.ExplosionSourceType sourceType = Program7.CONFIG.terrainDestruction
					? World.ExplosionSourceType.MOB
					: World.ExplosionSourceType.NONE;
			world.createExplosion(this, this.getX(), this.getY(), this.getZ(),
					EXPLOSION_POWER, sourceType);
			this.discard();
		}
	}
}
