package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.RetreatGoal;
import dev.rheava.program7.entity.ai.SpotTargetGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * The Tier 2 ground scout: a fast, unarmed runabout that finds you, shadows
 * you at a standoff distance, and paints you for whatever else the Program
 * has nearby. It never fights — a hit is its cue to bolt, not to return
 * fire.
 */
public class ScoutCarEntity extends ProgramDroneEntity {
	/** How long it keeps running after taking a hit. */
	private static final int FLEE_TICKS = 100;

	public ScoutCarEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 5;
	}

	public static DefaultAttributeContainer.Builder createScoutCarAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0);
	}

	@Override
	protected boolean isFlier() {
		return false;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new SpotTargetGoal(this));
		this.goalSelector.add(2, new RetreatGoal(this));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.9));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
		this.goalSelector.add(5, new LookAroundGoal(this));

		// No target selectors: the scout never fights. SpotTargetGoal finds
		// players on its own and hands the fight off to nearby armed drones.
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		boolean hurt = super.damage(source, amount);
		if (hurt && !this.getWorld().isClient && source.getAttacker() instanceof LivingEntity attacker) {
			// Unarmed: getting hit is the cue to run, not to fight back.
			this.beginRetreat(attacker, FLEE_TICKS);
		}
		return hurt;
	}
}
