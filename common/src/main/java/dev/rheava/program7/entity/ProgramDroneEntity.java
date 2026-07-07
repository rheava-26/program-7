package dev.rheava.program7.entity;

import java.util.ArrayList;
import java.util.List;

import dev.rheava.program7.block.DroneWreckBlock;
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
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
	private int scrambledTicks = 0;

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

	public boolean isScrambled() {
		return this.scrambledTicks > 0;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (!this.getWorld().isClient && source.getAttacker() instanceof LivingEntity attacker) {
			ItemStack weapon = attacker.getMainHandStack();
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
		return super.damage(source, amount);
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
		if (!this.getWorld().isClient && this.scrambledTicks > 0) {
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
