package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.recipe.GreatChaliceRecipe;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class KnightLibRecipes {

    public static void init() { ;; }

    public static final RecipeSerializer<GreatChaliceRecipe> CHALICE_SERIALIZER;
    public static final RecipeType<GreatChaliceRecipe> CHALICE_TYPE;

    static {
        CHALICE_SERIALIZER = Registry.register(Registry.RECIPE_SERIALIZER, new ResourceLocation(KnightLib.MOD_ID, "great_chalice_interaction"), GreatChaliceRecipe.SERIALIZER);
        CHALICE_TYPE = Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(KnightLib.MOD_ID, "great_chalice_interaction"), GreatChaliceRecipe.RECIPE_TYPE);
    }

}
