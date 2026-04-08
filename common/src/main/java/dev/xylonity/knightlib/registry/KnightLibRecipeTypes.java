package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.common.recipe.GreatChaliceRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;

public final class KnightLibRecipeTypes {

    public static final ResourceRegistry<RecipeType<?>> RECIPE_TYPES = ResourceDispatcher.create(BuiltInRegistries.RECIPE_TYPE, KnightLib.MOD_ID);

    public static final ResourceEntry<RecipeType<GreatChaliceRecipe>> CHALICE_TYPE = RECIPE_TYPES.register("great_chalice_interaction", () -> GreatChaliceRecipe.RECIPE_TYPE);

}
