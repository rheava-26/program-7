package dev.rheava.program7.entity;

import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/**
 * One bomb out of a CAS aircraft's stick (see {@code GunshipEntity} /
 * {@code dev.rheava.program7.entity.ai.CasBombingGoal}). Released, not
 * launched — it inherits the dropping aircraft's own velocity at the moment
 * of release and falls hard from there, no ballistic solve needed: a
 * diving-pass release over the target does the aiming, not the munition.
 * Per the doc's "telegraphed by the approaching engine note and the visible
 * run-in — the one indirect attack you can see coming and sprint out of the
 * lane of," so this warhead is tuned closer to the howitzer's than the
 * mortar's — a real threat if you don't move.
 */
public class BombEntity extends AbstractShellEntity {
	/** Falls hard — a dropped bomb, not a lobbed shell riding a long arc. */
	private static final double GRAVITY = 0.09;
	private static final float EXPLOSION_POWER = 2.6f;

	public BombEntity(EntityType<? extends BombEntity> entityType, World world) {
		super(entityType, world);
	}

	public BombEntity(World world, LivingEntity owner) {
		super(P7Entities.BOMB.get(), owner, world);
	}

	@Override
	protected double shellGravity() {
		return GRAVITY;
	}

	@Override
	protected int whistleIntervalTicks() {
		// Short flight from a diving pass — the whistle needs to land at
		// least once or twice before impact, not just once at the top.
		return 5;
	}

	@Override
	protected float whistleVolume() {
		return 1.3f;
	}

	@Override
	protected float whistlePitch() {
		return 1.1f;
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
		return 1.5f;
	}

	@Override
	protected float impactPitch() {
		return 0.75f;
	}
}
