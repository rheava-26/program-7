package dev.rheava.program7.entity;

import java.util.EnumSet;
import java.util.List;

import dev.rheava.program7.entity.ai.RepairAuraGoal;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Tier 3 support asset: a rolling power bank that crawls along after the
 * Program's forces to keep them patched up far from base. It never raises a
 * weapon — {@link RepairAuraGoal} and whatever escort is standing near it are
 * its whole defense. That's the point: kill the truck and the whole local
 * operation chokes. Every machine it was propping up browns out the instant
 * it goes down, so a siege gets a lot easier once this thing is scrap.
 */
public class BatteryCenterEntity extends ProgramDroneEntity {
	/** Everything but another battery center browns out when this one dies. */
	private static final double BROWNOUT_RANGE = 32.0;
	private static final int BROWNOUT_DURATION_TICKS = 300;

	public BatteryCenterEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 25;
	}

	public static DefaultAttributeContainer.Builder createBatteryCenterAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 90.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0)
				.add(EntityAttributes.GENERIC_ARMOR, 10.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected boolean isFlier() {
		// Ground vehicle: knockback shoves it around but never scrambles it.
		return false;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new RepairAuraGoal(this));
		this.goalSelector.add(2, new FollowCrowdGoal(this));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.6));
		this.goalSelector.add(4, new LookAroundGoal(this));

		// No targetSelector entries at all, not even RevengeGoal: this thing
		// never fights back, on purpose. Its guards are its defense.
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		super.onDeath(damageSource);
		if (this.getWorld() instanceof ServerWorld world) {
			Box box = this.getBoundingBox().expand(BROWNOUT_RANGE);
			List<ProgramDroneEntity> supported = world.getEntitiesByClass(ProgramDroneEntity.class, box,
					unit -> unit != this && !(unit instanceof BatteryCenterEntity));
			for (ProgramDroneEntity unit : supported) {
				// Brownout: the power bank propping these machines up just died with it.
				unit.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, BROWNOUT_DURATION_TICKS, 2));
				unit.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, BROWNOUT_DURATION_TICKS, 1));
				world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
						unit.getX(), unit.getY() + unit.getHeight() * 0.5, unit.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
			}
			// A big capacitor letting go, not just another drone dying.
			this.playSound(P7Sounds.DRONE_DEATH.get(), 1.5f, 0.5f);
		}
	}

	@Override
	public int getMinAmbientSoundDelay() {
		// It hums constantly — barely any gap between ambient sounds.
		return 60;
	}

	@Override
	protected float getSoundVolume() {
		return 0.7f;
	}

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.ARMORED_VEHICLE;
	}

	/**
	 * Keeps the battery center from getting left behind: whenever some other
	 * Program unit (not another battery center) has wandered off past
	 * comfortable repair-aura range but hasn't left the area outright, this
	 * plods after the nearest one. Simple by design — it doesn't try to
	 * herd the whole squad, just close the gap with whoever's closest.
	 */
	private static final class FollowCrowdGoal extends Goal {
		private static final double DETECT_RANGE = 40.0;
		private static final double CATCH_UP_RANGE = 12.0;

		private final BatteryCenterEntity battery;
		@Nullable
		private ProgramDroneEntity target;

		private FollowCrowdGoal(BatteryCenterEntity battery) {
			this.battery = battery;
			this.setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			this.target = this.findNearestStraggler();
			return this.target != null;
		}

		@Override
		public boolean shouldContinue() {
			if (this.target == null || !this.target.isAlive()) {
				return false;
			}
			double distanceSq = this.battery.squaredDistanceTo(this.target);
			return distanceSq > CATCH_UP_RANGE * CATCH_UP_RANGE && distanceSq < DETECT_RANGE * DETECT_RANGE;
		}

		@Override
		public void tick() {
			if (this.target == null) {
				return;
			}
			if (this.battery.getNavigation().isIdle()) {
				this.battery.getNavigation().startMovingTo(this.target, 1.0);
			}
		}

		@Override
		public void stop() {
			this.target = null;
			this.battery.getNavigation().stop();
		}

		@Nullable
		private ProgramDroneEntity findNearestStraggler() {
			Box box = this.battery.getBoundingBox().expand(DETECT_RANGE);
			List<ProgramDroneEntity> units = this.battery.getWorld().getEntitiesByClass(ProgramDroneEntity.class, box,
					unit -> unit != this.battery && !(unit instanceof BatteryCenterEntity));

			ProgramDroneEntity nearest = null;
			double nearestDistanceSq = Double.MAX_VALUE;
			for (ProgramDroneEntity unit : units) {
				double distanceSq = this.battery.squaredDistanceTo(unit);
				if (distanceSq > CATCH_UP_RANGE * CATCH_UP_RANGE && distanceSq < nearestDistanceSq) {
					nearest = unit;
					nearestDistanceSq = distanceSq;
				}
			}
			return nearest;
		}
	}
}
