package dev.rheava.program7.entity;

import dev.rheava.program7.director.SupplyNetwork;
import dev.rheava.program7.director.UpkeepProfile;
import dev.rheava.program7.entity.ai.BurstGunAttackGoal;
import dev.rheava.program7.entity.ai.HoverWanderGoal;
import dev.rheava.program7.entity.ai.InertialFlightMoveControl;
import dev.rheava.program7.entity.ai.InvestigateNoiseGoal;
import dev.rheava.program7.entity.ai.RetreatGoal;
import dev.rheava.program7.entity.ai.RoundClass;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Tier 3 wall: a car-sized armored gun flyer built around a machine gun that
 * hoses targets with bursts instead of single shots. It's still a flier by
 * the faction's rules — rotor quick-kills and the Knockback/Punch scramble
 * both still land on it exactly like any other airframe — this thing is
 * big, not invincible. What actually protects it is mass: its
 * {@link InertialFlightMoveControl} spools up and banks slowly, so every
 * maneuver telegraphs and surviving it is about reading those turns, not
 * simply out-tanking the gun.
 */
public class HeavyAttackDroneEntity extends ProgramDroneEntity {
	public HeavyAttackDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new InertialFlightMoveControl(this, 20, true, 4.0f);
		this.experiencePoints = 20;
	}

	public static DefaultAttributeContainer.Builder createHeavyAttackDroneAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5)
				// +50% acquisition range so this tier engages a bit sooner;
				// gun range (below) stays tight — this is not a standoff unit.
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 72.0)
				.add(EntityAttributes.GENERIC_ARMOR, 10.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void initGoals() {
		// Priority 0: even a heavy limps off when it's nearly dead rather than
		// dying in place (see fleeHealthFraction) — only fires while retreating.
		this.goalSelector.add(0, new RetreatGoal(this));
		// Damage 3.0->10.0/shot (gun-feel pass), MEDIUM caliber — a bigger gun
		// flyer than the light drones, one rung below the gunship's HEAVY belly gun.
		this.goalSelector.add(1, new BurstGunAttackGoal(this, 1.0, 24.0, 4, 50, 10.0f, RoundClass.MEDIUM));
		this.goalSelector.add(2, new InvestigateNoiseGoal(this));
		this.goalSelector.add(3, new HoverWanderGoal(this));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f));

		this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false, null));
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation navigation = new BirdNavigation(this, world);
		navigation.setCanPathThroughDoors(false);
		navigation.setCanSwim(false);
		navigation.setCanEnterOpenDoors(true);
		return navigation;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return P7Sounds.HELI_ROTOR_LOOP.get();
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 45;
	}

	@Override
	protected float getSoundVolume() {
		return 1.1f;
	}

	@Override
	protected ArmorProfile armorProfile() {
		return ArmorProfile.ARMORED_VEHICLE;
	}

	@Override
	public boolean isRangedAttacker() {
		return true;
	}

	@Override
	protected float fleeHealthFraction() {
		// A heavy holds the line far longer than a light strafer — it only
		// breaks contact once it's nearly wrecked, buying a tense "it's
		// limping away, finish it" beat instead of a fight to the death.
		return 0.15f;
	}

	@Nullable
	@Override
	protected UpkeepProfile upkeepProfile() {
		// Tier 3 fuel upkeep (SUPPLY_LINES_SPEC.md §2): 90s full-power
		// endurance off-depot — short leash, hard brownout.
		return new UpkeepProfile(SupplyNetwork.SUPPLY_FUEL, 2, 9);
	}
}
