package dev.rheava.program7.entity;

import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * The mortar's shell: a slow arcing lob, not a straight-line shot. It is not
 * a living thing — {@link ThrownEntity} rather than {@code ProgramDroneEntity}
 * fits since a shell has no health, no AI, and no wreck; it either flies or
 * it has already gone off. It smokes the whole way up, whistles the whole
 * way down (the incoming warning is the point), and detonates on whatever it
 * touches first.
 */
public class MortarShellEntity extends ThrownEntity {
	/** Heavier arc than a snowball/egg: this thing is meant to come down hard. */
	private static final double GRAVITY = 0.06;
	private static final float EXPLOSION_POWER = 1.8f;

	public MortarShellEntity(EntityType<? extends MortarShellEntity> entityType, World world) {
		super(entityType, world);
	}

	public MortarShellEntity(World world, LivingEntity owner) {
		super(P7Entities.MORTAR_SHELL.get(), owner, world);
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
					1, 0.0, 0.0, 0.0, 0.0);
			// Whistling only starts once it's arced over and is coming down —
			// that falling whistle is the player's warning to move.
			if (this.getVelocity().y < 0.0 && this.age % 10 == 0) {
				this.playSound(P7Sounds.MORTAR_WHISTLE.get(), 1.2f, 1.0f);
			}
		}
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		if (this.getWorld() instanceof ServerWorld world) {
			this.playSound(P7Sounds.MORTAR_IMPACT.get(), 1.0f, 1.0f);
			world.createExplosion(this, this.getX(), this.getY(), this.getZ(),
					EXPLOSION_POWER, World.ExplosionSourceType.NONE);
			this.discard();
		}
	}
}
