package dev.xylonity.knightlib.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.recipe.input.ChaliceFillingRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record ChaliceFillingRecipe(ItemStack input, ItemStack block) implements Recipe<ChaliceFillingRecipeInput> {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(KnightLib.MOD_ID, "block_filling");

    public static final RecipeSerializer<ChaliceFillingRecipe> SERIALIZER = new Serializer();
    public static final RecipeType<ChaliceFillingRecipe> RECIPE_TYPE = new Type();

    @Override
    public boolean matches(ChaliceFillingRecipeInput chaliceFillingRecipeInput, @NotNull Level level) {
        return ItemStack.isSameItem(chaliceFillingRecipeInput.getItem(0), input);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull ChaliceFillingRecipeInput chaliceFillingRecipeInput, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        return ItemStack.EMPTY;
    }

    public static ResourceLocation getID() {
        return ID;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return RECIPE_TYPE;
    }

    public static class Type implements RecipeType<ChaliceFillingRecipe> {

        @Override
        public String toString() {
            return ID.toString();
        }

    }

    public static class Serializer implements RecipeSerializer<ChaliceFillingRecipe> {
        public static final MapCodec<ChaliceFillingRecipe> CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ChaliceFillingRecipe::input),
                        ItemStack.CODEC.fieldOf("result").forGetter(ChaliceFillingRecipe::block)
                ).apply(i, ChaliceFillingRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ChaliceFillingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, ChaliceFillingRecipe::input,
                        ItemStack.STREAM_CODEC, ChaliceFillingRecipe::block,
                        ChaliceFillingRecipe::new
                );

        @Override
        public MapCodec<ChaliceFillingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ChaliceFillingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

    }

}