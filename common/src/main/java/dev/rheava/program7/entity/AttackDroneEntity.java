package dev.rheava.program7.entity;

import dev.rheava.program7.Program7;
import dev.rheava.program7.entity.ai.ChaseAndDetonateGoal;
import dev.rheava.program7.entity.ai.HoverWanderGoal;
import dev.rheava.program7.entity.ai.InvestigateNoiseGoal;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/**
 * Tier 1 response unit: a small, fast, expendable flyer that chases its
 * target down and detonates — creeper logic with rotors. Once the fuse is
 * lit it is committed; the accelerating beep is the only warning. Shooting
 * it down before it arms leaves a wreck with its warhead inside; letting it
 * detonate leaves nothing.
 */
public class AttackDroneEntity extends ProgramDroneEntity {
	private static final int FUSE_TICKS = 30;

	private boolean armed = false;
	private int fuseTicks = 0;

	public AttackDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new FlightMoveControl(this, 20, true);
		this.experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createAttackDroneAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 1.2)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new ChaseAndDetonateGoal(this));
		this.goalSelector.add(2, new InvestigateNoiseGoal(this));
		this.goalSelector.add(3, new HoverWanderGoal(this));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f));

		this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation navigation = new BirdNavigation(this, world);
		navigation.setCanPathThroughDoors(false);
		navigation.setCanSwim(false);
		navigation.setCanEnterOpenDoors(true);
		return navigation;
	}

	public void arm() {
		if (!this.armed) {
			this.armed = true;
			this.playSound(P7Sounds.ATTACK_DRONE_FUSE.get(), 1.0f, 1.0f);
		}
	}

	public boolean isArmed() {
		return this.armed;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (!this.getWorld().isClient && this.armed) {
			this.fuseTicks++;
			if (this.fuseTicks % 4 == 0) {
				this.playSound(P7Sounds.DRONE_SCAN_BEEP.get(), 1.0f,
						1.2f + (this.fuseTicks / (float) FUSE_TICKS) * 0.8f);
			}
			if (this.fuseTicks >= FUSE_TICKS) {
				this.explode();
			}
		}
	}

	private void explode() {
		// Discarded, not killed: a detonation consumes the drone, no wreck.
		this.discard();
		World.ExplosionSourceType sourceType = Program7.CONFIG.terrainDestruction
				? World.ExplosionSourceType.MOB
				: World.ExplosionSourceType.NONE;
		this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), 2.0f, sourceType);
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 40;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Armed", this.armed);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.armed = nbt.getBoolean("Armed");
	}
}
