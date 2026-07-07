package dev.rheava.program7.entity;

import dev.rheava.program7.director.ProgramDirectorState;
import dev.rheava.program7.registry.P7Blocks;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The pod itself, mid-descent: a screaming column of smoke and flame that
 * ends in a classic Minecraft explosion and a placed {@code probe_core}.
 *
 * <p>Per the VFX direction: vanilla particle ingredients (campfire smoke,
 * flame, explosion emitters) in a semi-realistic composition. The roar is
 * played loud enough to carry hundreds of blocks.
 */
public class DropPodEntity extends Entity {
	public DropPodEntity(EntityType<? extends DropPodEntity> type, World world) {
		super(type, world);
		this.noClip = false;
	}

	@Override
	public void tick() {
		super.tick();

		if (this.getWorld().isClient) {
			// Re-entry trail: a fat smoke column with a burning tail.
			for (int i = 0; i < 4; i++) {
				this.getWorld().addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
						this.getParticleX(1.2), this.getY() + 1.5 + this.random.nextDouble() * 1.5,
						this.getParticleZ(1.2), 0.0, 0.06, 0.0);
			}
			this.getWorld().addParticle(ParticleTypes.FLAME,
					this.getParticleX(0.6), this.getY() + 1.2, this.getParticleZ(0.6),
					0.0, 0.02, 0.0);
			this.getWorld().addParticle(ParticleTypes.LAVA,
					this.getX(), this.getY() + 1.0, this.getZ(), 0.0, 0.0, 0.0);
			return;
		}

		Vec3d velocity = this.getVelocity();
		this.setVelocity(velocity.x * 0.98, Math.max(velocity.y - 0.10, -1.8), velocity.z * 0.98);
		this.move(MovementType.SELF, this.getVelocity());

		if (this.age % 15 == 0) {
			this.getWorld().playSound(null, this.getBlockPos(), P7Sounds.DROP_POD_DESCENT,
					SoundCategory.HOSTILE, 8.0f, 0.8f + this.random.nextFloat() * 0.3f);
		}

		if (this.isOnGround() || this.verticalCollision) {
			this.impact((ServerWorld) this.getWorld());
		}
	}

	private void impact(ServerWorld world) {
		BlockPos pos = this.getBlockPos();

		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 2.0, 1.0, 2.0, 0.0);
		world.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
				pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 60, 3.0, 2.0, 3.0, 0.02);
		// Loud enough to carry: volume extends audible range far past the render distance.
		world.playSound(null, pos, P7Sounds.DROP_POD_IMPACT, SoundCategory.HOSTILE, 16.0f, 0.9f);

		ProgramDirectorState.deployProbeAt(world, pos);
		this.discard();
	}

	/** Recon + extraction complement that fans out from a fresh landing. */
	public static void spawnLandingComplement(ServerWorld world, BlockPos pos) {
		for (int i = 0; i < 2; i++) {
			SurveyorDroneEntity surveyor = P7Entities.SURVEYOR_DRONE.create(world);
			if (surveyor != null) {
				surveyor.refreshPositionAndAngles(
						pos.getX() + 0.5 + world.getRandom().nextInt(9) - 4,
						pos.getY() + 3.0 + world.getRandom().nextInt(3),
						pos.getZ() + 0.5 + world.getRandom().nextInt(9) - 4,
						world.getRandom().nextFloat() * 360.0f, 0.0f);
				world.spawnEntity(surveyor);
			}
		}
		for (int i = 0; i < 2; i++) {
			HarvesterDroneEntity harvester = P7Entities.HARVESTER_DRONE.create(world);
			if (harvester != null) {
				harvester.refreshPositionAndAngles(
						pos.getX() + 0.5 + world.getRandom().nextInt(7) - 3,
						pos.getY() + 1.0,
						pos.getZ() + 0.5 + world.getRandom().nextInt(7) - 3,
						world.getRandom().nextFloat() * 360.0f, 0.0f);
				world.spawnEntity(harvester);
			}
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}
}
