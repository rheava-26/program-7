package dev.rheava.program7.entity;

import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/**
 * The howitzer's shell: heavier and floatier than {@link MortarShellEntity}
 * (a longer hang for the longer standoff range), with a much bigger warhead.
 *
 * <p>Unlike the mortar's shell, this one actually breaks cover on impact —
 * the artillery doc's "wall-breaker." Block destruction reuses the exact
 * config+gamerule-gated {@code World.ExplosionSourceType} pattern
 * {@link AttackDroneEntity} already uses for its own detonation.
 */
public class HowitzerShellEntity extends AbstractShellEntity {
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
	protected double shellGravity() {
		return GRAVITY;
	}

	@Override
	protected int smokeParticleCount() {
		return 2;
	}

	@Override
	protected float whistleVolume() {
		return 1.6f;
	}

	@Override
	protected float whistlePitch() {
		return 0.75f;
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
	protected SoundEvent impactSound() {
		return P7Sounds.MORTAR_IMPACT.get();
	}

	@Override
	protected float impactVolume() {
		return 1.6f;
	}

	@Override
	protected float impactPitch() {
		return 0.7f;
	}
}
