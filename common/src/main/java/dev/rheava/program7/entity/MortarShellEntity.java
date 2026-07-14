package dev.rheava.program7.entity;

import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/**
 * The mortar's shell: a slow arcing lob, not a straight-line shot. It smokes
 * the whole way up, whistles the whole way down (the incoming warning is the
 * point), and detonates on whatever it touches first — damage-only, no block
 * interaction, unlike the howitzer's wall-breaking round.
 */
public class MortarShellEntity extends AbstractShellEntity {
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
	protected double shellGravity() {
		return GRAVITY;
	}

	@Override
	protected float whistleVolume() {
		return 1.2f;
	}

	@Override
	protected float whistlePitch() {
		return 1.0f;
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
		return 1.0f;
	}

	@Override
	protected float impactPitch() {
		return 1.0f;
	}
}
