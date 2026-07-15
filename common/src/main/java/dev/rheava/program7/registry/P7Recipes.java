package dev.rheava.program7.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rheava.program7.Program7;
import dev.rheava.program7.recipe.LensRepairRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.RegistryKeys;

/**
 * Custom recipe serializers. Currently just the lens-repair special recipe
 * (see {@link LensRepairRecipe}) — everything else the mod crafts is a
 * plain data-driven {@code minecraft:crafting_shaped} JSON recipe, same as
 * vanilla's own {@code crafting_special_*} recipes (banner duplication,
 * tipped arrows, book cloning, ...) sit alongside data-driven ones.
 */
public final class P7Recipes {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
			DeferredRegister.create(Program7.MOD_ID, RegistryKeys.RECIPE_SERIALIZER);

	public static final RegistrySupplier<RecipeSerializer<?>> LENS_REPAIR = RECIPE_SERIALIZERS.register(
			"lens_repair", () -> new SpecialRecipeSerializer<>(LensRepairRecipe::new));

	public static void register() {
		RECIPE_SERIALIZERS.register();
	}

	private P7Recipes() {
	}
}
