package dev.rheava.program7.recipe;

import dev.rheava.program7.item.ChargeLaserItem;
import dev.rheava.program7.item.ChargeLaserState;
import dev.rheava.program7.registry.P7DataComponents;
import dev.rheava.program7.registry.P7Items;
import dev.rheava.program7.registry.P7Recipes;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/**
 * Crafting-grid repair for the charge laser's amethyst lens: the laser plus
 * one or more amethyst shards, nothing else, in any arrangement. Each shard
 * knocks a fixed chunk off {@link ChargeLaserState#lensWear}, so a full
 * repair from empty takes {@link #SHARDS_FOR_FULL_REPAIR} shards. The output
 * is a copy of the input laser stack with only {@code lensWear} changed —
 * battery charge and heat carry through untouched, same as combining two
 * damaged tools with vanilla's repair-item recipe carries enchantments
 * through.
 */
public class LensRepairRecipe extends SpecialCraftingRecipe {
	/** Shards needed to fully repair a completely spent lens. */
	private static final int SHARDS_FOR_FULL_REPAIR = 8;
	private static final int REPAIR_PER_SHARD = ChargeLaserItem.LENS_MAX_TICKS / SHARDS_FOR_FULL_REPAIR;

	public LensRepairRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean fits(int width, int height) {
		// Needs room for the laser plus at least one amethyst shard.
		return width * height >= 2;
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		int laserCount = 0;
		int shardCount = 0;
		for (ItemStack stack : input.getStacks()) {
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.isOf(P7Items.CHARGE_LASER.get())) {
				laserCount++;
			} else if (stack.isOf(Items.AMETHYST_SHARD)) {
				shardCount++;
			} else {
				return false;
			}
		}
		return laserCount == 1 && shardCount > 0;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registriesLookup) {
		ItemStack laser = ItemStack.EMPTY;
		int shardCount = 0;
		for (ItemStack stack : input.getStacks()) {
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.isOf(P7Items.CHARGE_LASER.get())) {
				laser = stack;
			} else {
				shardCount++;
			}
		}
		if (laser.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack result = laser.copy();
		ChargeLaserState state = ChargeLaserItem.getState(result);
		int repaired = Math.max(0, state.lensWear() - shardCount * REPAIR_PER_SHARD);
		result.set(P7DataComponents.CHARGE_LASER_STATE, state.withLensWear(repaired));
		return result;
	}

	@Override
	public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
		return ItemStack.EMPTY;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return P7Recipes.LENS_REPAIR.get();
	}
}
