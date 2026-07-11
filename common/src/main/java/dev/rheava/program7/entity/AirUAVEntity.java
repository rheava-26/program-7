package dev.rheava.program7.entity;

import dev.rheava.program7.entity.ai.CircleLoiterGoal;
import dev.rheava.program7.entity.ai.ExploreGoal;
import dev.rheava.program7.entity.ai.FixedWingMoveControl;
import dev.rheava.program7.entity.ai.InvestigateDisturbanceGoal;
import dev.rheava.program7.entity.ai.UAVSpotGoal;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Air UAV: a fixed-wing spotter slung off a {@link
 * dev.rheava.program7.block.LaunchCatapultBlock}. It never fights — it just
 * circles its launch site at altitude, watches for a player, and paints them
 * for every armed drone in earshot. Shoot it down and the base upstairs goes
 * blind until the catapult can afford to sling another one up.
 */
public class AirUAVEntity extends ProgramDroneEntity {
	@Nullable
	private BlockPos homePos = null;

	public AirUAVEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new FixedWingMoveControl(this, 10, true);
		this.experiencePoints = 6;
	}

	public static DefaultAttributeContainer.Builder createAirUAVAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4)
				.add(EntityAttributes.GENERIC_FLYING_SPEED, 1.1)
				// Raised for symmetric "if you can see it, it can see you"
				// LOS-gated perception (InvestigateDisturbanceGoal). 160 is the
				// practical ceiling for this attribute; true whole-region
				// (~600 block) symmetry needs entity simulation distance / a
				// future virtualization layer — a known limit, not fixed here.
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 160.0)
				// A plane shouldn't get shoved off course by a stray hit.
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
	}

	/** Fixed-wing airframe: never drains its battery, since it can't land to recharge (no LandAndChargeGoal). */
	@Override
	protected float chargeDrainPerTick() {
		return 0.0f;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new UAVSpotGoal(this));
		// No LandAndChargeGoal: this is a fixed-wing airframe (FixedWingMoveControl
		// never stops thrusting and has a wide turn radius), so it physically
		// can't converge on and settle onto a ground point — a land-to-charge
		// goal would just lock it into an eternal tight orbit. It's exempted from
		// battery drain entirely (chargeDrainPerTick -> 0 below).
		this.goalSelector.add(2, new InvestigateDisturbanceGoal(this));
		// Priority 3: "prioritize exploring above all else" (see #3) — a fresh
		// UAV fans out on long legs looking for structures/caves/villages
		// instead of immediately settling into its home loiter circle. Still
		// below the disturbance check above it, so a moving contact nearby
		// interrupts a long leg rather than getting ignored.
		this.goalSelector.add(3, new ExploreGoal(this));
		this.goalSelector.add(4, new CircleLoiterGoal(this, 28.0, 20.0, 1.0));
		this.goalSelector.add(5, new LookAroundGoal(this));

		// No target selectors: unarmed spotter. UAVSpotGoal hands contacts off
		// to whatever armed hardware is already in range.
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation navigation = new BirdNavigation(this, world);
		navigation.setCanPathThroughDoors(false);
		navigation.setCanSwim(false);
		navigation.setCanEnterOpenDoors(true);
		return navigation;
	}

	@Nullable
	public BlockPos getHomePos() {
		return this.homePos;
	}

	public void setHomePos(@Nullable BlockPos homePos) {
		this.homePos = homePos;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		// Airspeed floor is now owned by FixedWingMoveControl itself; nothing
		// left to do here beyond the base drone bookkeeping above.
	}

	@Override
	protected boolean isFixedWing() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return P7Sounds.PLANE_ENGINE_LOOP.get();
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 50;
	}

	@Override
	protected float getSoundVolume() {
		// Bumped for the "distant menace" pass (see #6): a plane droning
		// overhead should be audible well before it's visible.
		return 1.3f;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (this.homePos != null) {
			nbt.putIntArray("HomePos", new int[] {
					this.homePos.getX(), this.homePos.getY(), this.homePos.getZ()});
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("HomePos")) {
			int[] pos = nbt.getIntArray("HomePos");
			if (pos.length == 3) {
				this.homePos = new BlockPos(pos[0], pos[1], pos[2]);
			}
		}
	}
}
