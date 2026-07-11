package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.DeployDronesGoal;
import dev.rheava.program7.entity.ai.GunAttackGoal;
import dev.rheava.program7.entity.ai.RoundClass;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/**
 * Tier 3 wall: an armored fighting vehicle taller than the player, built to
 * haul a light-drone fireteam forward per the unit bible. It holds its own
 * in a straight gunfight, but the moment it has a target locked it also
 * pops its drone bay ({@link DeployDronesGoal}), rolling ground drones out
 * to screen it while it keeps firing from behind them.
 */
public class IFVEntity extends ProgramDroneEntity {
	public IFVEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 25;
	}

	public static DefaultAttributeContainer.Builder createIFVAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 70.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.37)
				// +50% acquisition range so this tier engages a bit sooner;
				// gun range (below) stays tight — this is not a standoff unit.
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 60.0)
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.5)
				.add(EntityAttributes.GENERIC_ARMOR, 14.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected boolean isFlier() {
		return false;
	}

	@Override
	protected void initGoals() {
		// fireInterval 15->7: brings the main gun to ~2.9 rounds/sec so a
		// straight gunfight with it actually feels sustained (see #1).
		// Damage 4.0->11.0 (gun-feel pass), MEDIUM caliber autocannon — the
		// differentiated-rounds pass groups this with the gunboat's deck gun.
		this.goalSelector.add(1, new GunAttackGoal(this, 1.0, 18.0, 7, 11.0f, RoundClass.MEDIUM));
		this.goalSelector.add(2, new DeployDronesGoal(this));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.7));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		this.goalSelector.add(5, new LookAroundGoal(this));

		// Players first, then whatever else is hostile: this is a war
		// machine answering a real threat, not perimeter pest control.
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		this.targetSelector.add(3, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, null));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return P7Sounds.TANK_TRACKS_LOOP.get();
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 45;
	}

	@Override
	protected float getSoundVolume() {
		return 1.0f;
	}

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.ARMORED_VEHICLE;
	}
}
