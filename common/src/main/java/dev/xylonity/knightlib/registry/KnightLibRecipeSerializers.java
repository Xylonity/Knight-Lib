package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.common.recipe.GreatChaliceRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class KnightLibRecipeSerializers {

    public static final ResourceRegistry<RecipeSerializer<?>> RECIPE_SERIALIZERS = ResourceDispatcher.create(BuiltInRegistries.RECIPE_SERIALIZER, KnightLib.MOD_ID);

    public static final ResourceEntry<RecipeSerializer<GreatChaliceRecipe>> CHALICE_SERIALIZER = RECIPE_SERIALIZERS.register("great_chalice_interaction", () -> GreatChaliceRecipe.SERIALIZER);

}
