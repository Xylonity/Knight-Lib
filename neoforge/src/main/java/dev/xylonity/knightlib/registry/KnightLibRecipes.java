package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.recipe.GreatChaliceRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class KnightLibRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, KnightLib.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, KnightLib.MOD_ID);

    public static final Supplier<RecipeSerializer<GreatChaliceRecipe>> CHALICE_SERIALIZER;
    public static final Supplier<RecipeType<GreatChaliceRecipe>> CHALICE_TYPE;

    static {
        CHALICE_SERIALIZER = SERIALIZERS.register("great_chalice_interaction", () -> GreatChaliceRecipe.SERIALIZER);
        CHALICE_TYPE = TYPES.register("great_chalice_interaction", () -> GreatChaliceRecipe.RECIPE_TYPE);
    }

}
