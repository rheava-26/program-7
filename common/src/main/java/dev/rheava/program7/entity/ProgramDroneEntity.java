package dev.rheava.program7.entity;

import java.util.ArrayList;
import java.util.List;

import dev.rheava.program7.block.DroneWreckBlock;
import dev.rheava.program7.entity.ArmorProfile.DamageClass;
import dev.rheava.program7.registry.P7Sounds;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all Program hardware. Encodes the faction-wide combat feel:
 *
 * <ul>
 *   <li><b>Fragile airframes:</b> swords and axes shred small fliers —
 *       usually a one-hit kill.</li>
 *   <li><b>Knockback scrambles:</b> Knockback/Punch-enchanted hits throw
 *       fliers off course, killing their control for a moment; hitting
 *       terrain while scrambled is a crash.</li>
 *   <li><b>Wrecks, not item sprays:</b> destroyed units crash into a
 *       lootable wreck block holding their salvage (loot table rolls plus
 *       anything the unit was carrying).</li>
 *   <li>Machines: poison-immune, never despawn, no fall damage.</li>
 * </ul>
 */
public abstract class ProgramDroneEntity extends PathAwareEntity {
	/**
	 * Aim high to clip the rotors: a hit landing in this top slice of the
	 * hitbox is treated as a disabling shot to the rotor plane, not a graze.
	 */
	private static final double ROTOR_HIT_SLICE_HEIGHT = 0.25;

	private int scrambledTicks = 0;
	private int retreatTicks = 0;
	@Nullable
	private LivingEntity retreatFrom;

	protected ProgramDroneEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
	}

	/** Fliers scramble and crash; ground units just get shoved around. */
	protected boolean isFlier() {
		return true;
	}

	/** Extra stacks (stolen cargo etc.) added to this unit's wreck. */
	protected List<ItemStack> getExtraWreckSalvage() {
		return List.of();
	}

	/**
	 * Per-damage-type resistance profile. Unarmored by default; armored
	 * units override this to give different weapon types a genuinely
	 * different fight against them instead of one flat reduction.
	 */
	protected ArmorProfile armorProfile() {
		return ArmorProfile.UNARMORED;
	}

	public boolean isScrambled() {
		return this.scrambledTicks > 0;
	}

	/**
	 * Shared "break contact" state used by {@link dev.rheava.program7.entity.ai.RetreatGoal}.
	 * Any drone that needs to bug out — after finishing a job, or just because
	 * it got hurt and it doesn't fight back — calls this instead of each unit
	 * rolling its own flee timer.
	 */
	public void beginRetreat(@Nullable LivingEntity threat, int ticks) {
		this.retreatFrom = threat;
		this.retreatTicks = ticks;
	}

	public boolean isRetreating() {
		return this.retreatTicks > 0;
	}

	@Nullable
	public LivingEntity getRetreatFrom() {
		return this.retreatFrom;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!this.getWorld().isClient) {
			LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
			ItemStack weapon = attacker != null ? attacker.getMainHandStack() : ItemStack.EMPTY;
			if (attacker != null) {
				if (this.isFlier()
						&& (weapon.getItem() instanceof SwordItem || weapon.getItem() instanceof AxeItem)) {
					// Blades shred airframes: small drones usually die in one hit.
					amount *= 2.5f;
				}
				int scramblePower = this.getEnchantLevel(weapon, Enchantments.KNOCKBACK)
						+ this.getEnchantLevel(weapon, Enchantments.PUNCH);
				if (scramblePower > 0 && this.isFlier()) {
					this.scramble(attacker, scramblePower);
				}
			}

			if (this.isFlier() && this.isRotorHit(source, attacker)) {
				// Rotor-plane hit: stacks multiplicatively on top of whatever
				// the blade bonus above already did, so a blade swung down
				// from above a small flier is an even cleaner kill.
				amount *= 1.5f;
				if (attacker != null) {
					this.scramble(attacker, 2);
				}
				this.spawnRotorHitEffects();
			}

			// Typed armor: layer the per-damage-class multiplier on top of
			// everything above, so armored units get a real, differently
			// textured fight depending on what's actually hitting them.
			amount *= this.armorProfile().multiplierFor(this.classify(source, weapon));
		}
		return super.damage(source, amount);
	}

	/**
	 * Buckets incoming damage into a {@link DamageClass} so
	 * {@link #armorProfile()} can apply a type-specific multiplier.
	 */
	private DamageClass classify(DamageSource source, ItemStack weapon) {
		if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
			return DamageClass.EXPLOSIVE;
		}
		if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
			if (source.getSource() instanceof TridentEntity) {
				return DamageClass.PIERCING;
			}
			if (source.getSource() instanceof ArrowEntity || source.getSource() instanceof SpectralArrowEntity
					|| source.getSource() instanceof PersistentProjectileEntity) {
				return DamageClass.HIGH_VELOCITY_IMPACT;
			}
			// Anything else riding the projectile tag — most gun-mod bullets
			// included — reads as conventional ballistic fire.
			return DamageClass.BALLISTIC;
		}
		if (source.getAttacker() instanceof LivingEntity) {
			int enchantedPower = this.getEnchantLevel(weapon, Enchantments.SHARPNESS)
					+ this.getEnchantLevel(weapon, Enchantments.SMITE)
					+ this.getEnchantLevel(weapon, Enchantments.BANE_OF_ARTHROPODS)
					+ this.getEnchantLevel(weapon, Enchantments.IMPALING);
			return enchantedPower > 0 ? DamageClass.ENCHANTED : DamageClass.MELEE;
		}
		return DamageClass.GENERIC;
	}

	/**
	 * A rotor-plane hit is one that lands in the top slice of the hitbox —
	 * either a melee attacker looking down into it, or a projectile that
	 * struck up there.
	 */
	private boolean isRotorHit(DamageSource source, @Nullable LivingEntity attacker) {
		double sliceMinY = this.getBoundingBox().maxY - ROTOR_HIT_SLICE_HEIGHT;
		if (attacker != null && attacker.getEyeY() >= sliceMinY) {
			return true;
		}
		if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
			Vec3d position = source.getPosition();
			if (position != null && position.y >= sliceMinY) {
				return true;
			}
		}
		return false;
	}

	private void spawnRotorHitEffects() {
		if (this.getWorld() instanceof ServerWorld serverWorld) {
			Box box = this.getBoundingBox();
			double x = (box.minX + box.maxX) / 2.0;
			double z = (box.minZ + box.maxZ) / 2.0;
			serverWorld.spawnParticles(ParticleTypes.CRIT, x, box.maxY, z, 8, 0.2, 0.05, 0.2, 0.02);
			serverWorld.spawnParticles(ParticleTypes.SMOKE, x, box.maxY, z, 4, 0.15, 0.05, 0.15, 0.01);
		}
		this.playSound(P7Sounds.DRONE_HURT.get(), 1.0f, 0.6f);
	}

	private int getEnchantLevel(ItemStack weapon, RegistryKey<Enchantment> key) {
		RegistryEntry<Enchantment> entry = this.getWorld().getRegistryManager()
				.getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(key);
		return EnchantmentHelper.getLevel(entry, weapon);
	}

	private void scramble(LivingEntity attacker, int power) {
		this.scrambledTicks = Math.max(this.scrambledTicks, 20 + 15 * power);
		Vec3d away = this.getPos().subtract(attacker.getPos());
		if (away.lengthSquared() < 1.0E-4) {
			away = new Vec3d(1.0, 0.0, 0.0);
		}
		away = away.normalize().multiply(0.5 * power);
		this.addVelocity(away.x, 0.25 + 0.1 * power, away.z);
		this.playSound(P7Sounds.DRONE_ALERT.get(), 0.8f, 1.7f);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (!this.getWorld().isClient) {
			if (this.scrambledTicks > 0) {
				this.scrambledTicks--;
				// Stabilizers are gone: no pathing, wild spin, drifting thrust.
				this.getNavigation().stop();
				this.setYaw(this.getYaw() + (this.random.nextFloat() - 0.5f) * 45.0f);
				this.addVelocity((this.random.nextDouble() - 0.5) * 0.12,
						(this.random.nextDouble() - 0.5) * 0.08,
						(this.random.nextDouble() - 0.5) * 0.12);
				if (this.horizontalCollision) {
					// Slammed into terrain while tumbling.
					this.damage(this.getDamageSources().flyIntoWall(), 5.0f);
					this.scrambledTicks = Math.min(this.scrambledTicks, 4);
				}
			}
			if (this.retreatTicks > 0) {
				this.retreatTicks--;
				if (this.retreatTicks == 0) {
					this.retreatFrom = null;
				}
			}
		}
	}

	@Override
	protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
		// Salvage goes into the wreck (see onDeath) instead of item sprays.
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		super.onDeath(damageSource);
		if (this.getWorld() instanceof ServerWorld world) {
			List<ItemStack> salvage = new ArrayList<>(this.getExtraWreckSalvage());
			LootTable lootTable = world.getServer().getReloadableRegistries()
					.getLootTable(this.getLootTable());
			LootContextParameterSet params = new LootContextParameterSet.Builder(world)
					.add(LootContextParameters.THIS_ENTITY, this)
					.add(LootContextParameters.ORIGIN, this.getPos())
					.add(LootContextParameters.DAMAGE_SOURCE, damageSource)
					.build(LootContextTypes.ENTITY);
			lootTable.generateLoot(params, salvage::add);
			DroneWreckBlock.placeWreck(world, this.getBlockPos(), salvage);
		}
	}

	@Override
	public boolean canHaveStatusEffect(StatusEffectInstance effect) {
		// Machines don't bleed — poison does nothing. Wither, per the design
		// doc, tears through drone plating just fine.
		if (effect.getEffectType() == StatusEffects.POISON) {
			return false;
		}
		return super.canHaveStatusEffect(effect);
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
	}

	@Override
	public void checkDespawn() {
		// Program hardware is deployed deliberately; it never just despawns.
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return P7Sounds.DRONE_AMBIENT.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return P7Sounds.DRONE_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return P7Sounds.DRONE_DEATH.get();
	}
}
