package dev.xylonity.knightlib.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.recipe.input.GenericRecipeInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record GreatChaliceRecipe(ItemStack input, ItemStack output) implements Recipe<GenericRecipeInput> {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(KnightLib.MOD_ID, "great_chalice_interaction");

    public static final RecipeSerializer<GreatChaliceRecipe> SERIALIZER = new Serializer();
    public static final RecipeType<GreatChaliceRecipe> RECIPE_TYPE = new Type();

    @Override
    public boolean matches(GenericRecipeInput genericRecipeInput, @NotNull Level level) {
        return ItemStack.isSameItem(genericRecipeInput.getItem(0), input);
    }

    @Override
    public ItemStack assemble(GenericRecipeInput genericRecipeInput, HolderLookup.Provider provider) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return output.copy();
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

    public static final class Type implements RecipeType<GreatChaliceRecipe> {

        @Override
        public String toString() {
            return ID.toString();
        }

    }

    public static class Serializer implements RecipeSerializer<GreatChaliceRecipe> {
        public static final MapCodec<GreatChaliceRecipe> CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(
                        ItemStack.CODEC.fieldOf("ingredient").forGetter(GreatChaliceRecipe::input),
                        ItemStack.CODEC.fieldOf("result").forGetter(GreatChaliceRecipe::output)
                ).apply(i, GreatChaliceRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, GreatChaliceRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ItemStack.STREAM_CODEC, GreatChaliceRecipe::input,
                        ItemStack.STREAM_CODEC, GreatChaliceRecipe::output,
                        GreatChaliceRecipe::new
                );

        @Override
        public @NotNull MapCodec<GreatChaliceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GreatChaliceRecipe> streamCodec() {
            return STREAM_CODEC;
        }

    }

}
