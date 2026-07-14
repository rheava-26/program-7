package dev.rheava.program7.entity;

import java.util.UUID;

import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The doc's "same framework, different travel path and cost": a missile
 * flies a near-flat guided path (almost no gravity, unlike the mortar/
 * howitzer's arc) and, if it was launched with a live line-of-sight
 * designation, steers toward its target's actual position every tick within
 * a limited turn rate — the mechanism that "beats the range accuracy floor"
 * every other indirect weapon is stuck under (see
 * {@code ARTILLERY_AND_INDIRECT_FIRE.md} §3). Losing the observer at launch
 * time still matters exactly like it does for every other tube: a missile
 * fired without one flies dumb to the last-known aim point instead of
 * homing on the live entity — see {@code MissileAttackGoal}, which only
 * hands over a guidance target when the firing mission was spotted.
 */
public class GuidedMissileEntity extends AbstractShellEntity {
	/** Near-flat flight: this is a guided path, not a lobbed arc. */
	private static final double GRAVITY = 0.01;
	private static final float EXPLOSION_POWER = 3.0f;
	/** Fraction of the heading gap closed toward the target each tick — a limited turn rate, not an instant snap-to. */
	private static final double TURN_RATE = 0.06;
	private static final double MIN_SPEED = 1.4;

	@Nullable
	private UUID guidanceTargetId;

	public GuidedMissileEntity(EntityType<? extends GuidedMissileEntity> entityType, World world) {
		super(entityType, world);
	}

	public GuidedMissileEntity(World world, LivingEntity owner) {
		super(P7Entities.GUIDED_MISSILE.get(), owner, world);
	}

	/** Set right after spawning by the firing goal, when (and only when) the mission that launched it was currently spotted by an observer. */
	public void setGuidanceTarget(@Nullable UUID targetId) {
		this.guidanceTargetId = targetId;
	}

	@Override
	protected double shellGravity() {
		return GRAVITY;
	}

	@Override
	public void tick() {
		if (this.guidanceTargetId != null && this.getWorld() instanceof ServerWorld world) {
			Entity target = world.getEntity(this.guidanceTargetId);
			if (target instanceof LivingEntity living && living.isAlive()) {
				this.steerToward(living.getBoundingBox().getCenter());
			} else {
				// Target gone (dead, logged off, unloaded) — the missile
				// keeps flying its last heading rather than snapping anywhere.
				this.guidanceTargetId = null;
			}
		}
		super.tick();
	}

	/** Bends the current velocity's direction toward {@code targetPos} by {@link #TURN_RATE} of the remaining gap, holding speed roughly constant. */
	private void steerToward(Vec3d targetPos) {
		Vec3d toTarget = targetPos.subtract(this.getPos());
		if (toTarget.lengthSquared() < 1.0E-4) {
			return;
		}
		Vec3d desiredDir = toTarget.normalize();
		Vec3d currentVelocity = this.getVelocity();
		double speed = Math.max(currentVelocity.length(), MIN_SPEED);
		Vec3d currentDir = currentVelocity.lengthSquared() > 1.0E-4 ? currentVelocity.normalize() : desiredDir;
		Vec3d blended = currentDir.add(desiredDir.subtract(currentDir).multiply(TURN_RATE));
		if (blended.lengthSquared() < 1.0E-4) {
			return;
		}
		this.setVelocity(blended.normalize().multiply(speed));
	}

	@Override
	protected ParticleEffect smokeParticleType() {
		return ParticleTypes.SMALL_FLAME;
	}

	@Override
	protected int smokeParticleCount() {
		return 2;
	}

	@Override
	protected int whistleIntervalTicks() {
		return 8;
	}

	@Override
	protected float whistleVolume() {
		return 1.4f;
	}

	@Override
	protected float whistlePitch() {
		return 1.5f;
	}

	@Override
	protected float explosionPower() {
		return EXPLOSION_POWER;
	}

	@Override
	protected boolean usesTerrainDestruction() {
		return true;
	}

	@Override
	protected float saturationWeight() {
		// Expensive and precise — when it's aimed at fortification it hits hard.
		return 2.0f;
	}

	@Override
	protected SoundEvent impactSound() {
		return P7Sounds.MORTAR_IMPACT.get();
	}

	@Override
	protected float impactVolume() {
		return 1.7f;
	}

	@Override
	protected float impactPitch() {
		return 0.8f;
	}
}
