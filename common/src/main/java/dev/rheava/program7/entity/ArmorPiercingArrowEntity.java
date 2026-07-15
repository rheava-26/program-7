package dev.rheava.program7.entity;

import java.util.HashSet;
import java.util.Set;

import dev.rheava.program7.registry.P7DamageTypes;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Items;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The armor-piercing crossbow bolt in flight — see {@link
 * dev.rheava.program7.item.ArmorPiercingBoltItem} for the ammo item this
 * spawns from. The design intent (see {@code docs/DESIGN.md}'s "Damage &
 * armor is typed" section and {@code docs/UNITS.md}'s "no build is walled
 * out" salvage-tree note): a deliberate, craft-and-carry specialist counter
 * — devastating against armored vehicles, unremarkable against everything
 * else.
 *
 * <p>Mechanically this is a thin {@link PersistentProjectileEntity} subclass
 * whose only real job is routing its hit damage through the {@code
 * program7:ap_bolt} datapack damage type (see {@link P7DamageTypes#AP_BOLT})
 * instead of vanilla's {@code minecraft:arrow}, so {@code
 * ProgramDroneEntity#classify} buckets it as {@link
 * ArmorProfile.DamageClass#PIERCING} — the mod's existing typed-armor math
 * (see {@link ArmorProfile#ARMORED_VEHICLE}/{@link ArmorProfile#HEAVY_HULL})
 * is what actually produces the "shreds armor, tickles anything unarmored"
 * profile; this class carries no armor-specific special-casing of its own.
 * {@code onEntityHit} is a from-scratch reimplementation (rather than a
 * {@code super} call) purely so a custom {@link DamageSource} can be built —
 * vanilla's own {@code onEntityHit} hardcodes {@code
 * getDamageSources().arrow(...)}, which isn't overridable piecemeal.
 */
public class ArmorPiercingArrowEntity extends PersistentProjectileEntity {
	/** Heavier tip than a plain arrow's 2.0 base — this is a purpose-built anti-armor round. */
	private static final double BASE_DAMAGE = 3.0;

	/** Local pierce-hit bookkeeping — {@code PersistentProjectileEntity}'s own tracking set isn't exposed to subclasses. */
	private final Set<Integer> piercedEntityIds = new HashSet<>();

	/** Entity-type registry constructor — the factory {@link P7Entities#ARMOR_PIERCING_BOLT} is built from. */
	public ArmorPiercingArrowEntity(EntityType<? extends ArmorPiercingArrowEntity> entityType, World world) {
		super(entityType, world);
	}

	/**
	 * The "fired from a crossbow" constructor — mirrors vanilla {@code
	 * ArrowEntity}'s {@code (World, LivingEntity, ItemStack, ItemStack)}
	 * shape, which {@link dev.rheava.program7.item.ArmorPiercingBoltItem#createArrow}
	 * calls into.
	 */
	public ArmorPiercingArrowEntity(World world, LivingEntity owner, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(P7Entities.ARMOR_PIERCING_BOLT.get(), owner, world, stack, shotFrom);
		this.setDamage(BASE_DAMAGE);
	}

	@Override
	protected ItemStack getDefaultItemStack() {
		return new ItemStack(P7Items.ARMOR_PIERCING_BOLT.get());
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		Entity target = entityHitResult.getEntity();
		if (!(this.getWorld() instanceof ServerWorld serverWorld) || this.piercedEntityIds.contains(target.getId())) {
			return;
		}

		Entity owner = this.getOwner();
		RegistryEntry<DamageType> apBoltType = serverWorld.getRegistryManager()
				.getWrapperOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(P7DamageTypes.AP_BOLT);
		// (type, source, attacker) — the projectile itself is the "source" (what
		// ProgramDroneEntity#classify inspects via source.getSource()), the
		// owner is the "attacker" credited for the kill, mirroring vanilla's
		// own DamageSources#arrow(PersistentProjectileEntity, Entity).
		DamageSource source = new DamageSource(apBoltType, this, owner);

		float speed = (float) this.getVelocity().length();
		float amount = MathHelper.ceil(speed * this.getDamage());
		if (this.isCritical()) {
			amount += this.random.nextInt((int) (amount / 2.0f) + 2);
		}
		amount = MathHelper.clamp(amount, 0.0f, Float.MAX_VALUE);

		boolean damaged = target.damage(source, amount);
		if (damaged) {
			this.piercedEntityIds.add(target.getId());
			if (target instanceof LivingEntity livingTarget) {
				this.knockback(livingTarget, source);
				if (this.isOnFire()) {
					livingTarget.setOnFireFor(5);
				}
			}
			this.playSound(this.getHitSound(), 1.0f, 1.2f);
		}

		if (this.piercedEntityIds.size() > this.getPierceLevel()) {
			this.discard();
		}
	}
}
