package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.KnightLibForge;
import dev.xylonity.knightlib.common.recipe.GreatChaliceRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class KnightLibRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, KnightLib.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, KnightLib.MOD_ID);

    public static final RegistryObject<RecipeSerializer<GreatChaliceRecipe>> CHALICE_SERIALIZER;
    public static final RegistryObject<RecipeType<GreatChaliceRecipe>> CHALICE_TYPE;

    static {
        CHALICE_SERIALIZER = SERIALIZERS.register("great_chalice_interaction", () -> GreatChaliceRecipe.SERIALIZER);
        CHALICE_TYPE = TYPES.register("great_chalice_interaction", () -> GreatChaliceRecipe.RECIPE_TYPE);
    }

}
