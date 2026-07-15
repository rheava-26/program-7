package dev.rheava.program7.entity;

import java.util.HashSet;
import java.util.Set;

import dev.rheava.program7.registry.P7DamageTypes;
import dev.rheava.program7.registry.P7Entities;
import dev.rheava.program7.registry.P7Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ProjectileDeflection;
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
 * ProgramDroneEntity#classify} buckets it as its own {@link
 * ArmorProfile.DamageClass#ARMOR_PIERCING} — deliberately not the trident's
 * {@link ArmorProfile.DamageClass#PIERCING} bucket, so tuning this bolt can
 * never stealth-buff a vanilla trident. The mod's existing typed-armor math
 * (see {@link ArmorProfile#ARMORED_VEHICLE}/{@link ArmorProfile#HEAVY_HULL})
 * is what actually produces the "armor barely matters" profile; this class
 * carries no armor-specific special-casing of its own. {@code onEntityHit}
 * is a from-scratch reimplementation (rather than a {@code super} call)
 * purely so a custom {@link DamageSource} can be built — vanilla's own
 * {@code onEntityHit} hardcodes {@code getDamageSources().arrow(...)}, which
 * isn't overridable piecemeal — but it otherwise mirrors vanilla {@code
 * PersistentProjectileEntity#onEntityHit} step for step: enchantment damage
 * scaling (Power/Impaling etc., via {@link EnchantmentHelper#getDamage}) and
 * the failed-damage deflect/discard path (shield block, i-frames, an
 * invulnerable target) both come straight from there.
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
		double damage = this.getDamage();
		// Power/Impaling/etc.: vanilla PersistentProjectileEntity#onEntityHit
		// re-derives its damage through EnchantmentHelper#getDamage, keyed off
		// the bow/crossbow that fired it, before scaling by velocity. Skipping
		// this is what silently strips enchantments off a bow-fired bolt — the
		// item sits in minecraft:tags/item/arrows, so a bow will happily load
		// it too, un-enchanted.
		ItemStack weaponStack = this.getWeaponStack();
		if (weaponStack != null) {
			damage = EnchantmentHelper.getDamage(serverWorld, weaponStack, target, source, (float) damage);
		}
		float amount = MathHelper.ceil(speed * damage);
		if (this.isCritical()) {
			amount += this.random.nextInt((int) (amount / 2.0f) + 2);
		}
		amount = MathHelper.clamp(amount, 0.0f, Float.MAX_VALUE);

		// Set (and remember any prior) fire ticks before the damage call, same
		// as vanilla — so a failed hit below can put the target's fire state
		// back exactly as it found it.
		int preHitFireTicks = target.getFireTicks();
		if (this.isOnFire()) {
			target.setOnFireFor(5);
		}

		boolean damaged = target.damage(source, amount);
		if (damaged) {
			// Only a successful hit counts as a pierce and gets the on-hit
			// effects below — see the failed-damage branch for why.
			this.piercedEntityIds.add(target.getId());
			if (target instanceof LivingEntity livingTarget) {
				this.knockback(livingTarget, source);
			}
			this.playSound(this.getHitSound(), 1.0f, 1.2f);
			if (this.piercedEntityIds.size() > this.getPierceLevel()) {
				this.discard();
			}
		} else {
			// Mirrors vanilla's failed-damage branch (shield block, i-frames, an
			// invulnerable target): undo the fire we just set, deflect/slow the
			// bolt instead of letting it phase through and re-collide with the
			// same target every subsequent tick, and drop/discard it once it's
			// essentially stopped. Not recording a pierce here is deliberate —
			// a bounced hit didn't actually get through the target, so it
			// shouldn't burn down the pierce budget or replay on-hit effects.
			target.setFireTicks(preHitFireTicks);
			this.deflect(ProjectileDeflection.SIMPLE, target, this.getOwner(), false);
			this.setVelocity(this.getVelocity().multiply(0.2));
			if (this.getVelocity().lengthSquared() < 1.0E-7) {
				if (this.pickupType == PersistentProjectileEntity.PickupPermission.ALLOWED) {
					this.dropStack(this.asItemStack(), 0.1f);
				}
				this.discard();
			}
		}
	}
}
