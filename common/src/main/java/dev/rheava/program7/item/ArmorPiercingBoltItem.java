package dev.rheava.program7.item;

import dev.rheava.program7.entity.ArmorPiercingArrowEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The armor-piercing crossbow bolt — ammunition, not a stand-alone weapon.
 * Extending {@link ArrowItem} (rather than a plain {@link
 * net.minecraft.item.Item}) is what makes a vanilla crossbow willing to load
 * and fire it: {@code RangedWeaponItem}'s ammo predicate is tag-based (see
 * {@code data/minecraft/tags/item/arrows.json}, which lists this item), and
 * {@link #createArrow} is the hook a bow/crossbow calls into to actually
 * spawn the flying entity.
 *
 * <p>See {@link ArmorPiercingArrowEntity} for the anti-armor routing itself
 * — this class has no combat logic of its own.
 */
public class ArmorPiercingBoltItem extends ArrowItem {
	public ArmorPiercingBoltItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter,
			@Nullable ItemStack shotFrom) {
		return new ArmorPiercingArrowEntity(world, shooter, stack.copyWithCount(1), shotFrom);
	}
}
