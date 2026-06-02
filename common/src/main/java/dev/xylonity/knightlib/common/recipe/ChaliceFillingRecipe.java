package dev.xylonity.knightlib.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xylonity.knightlib.KnightLib;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record ChaliceFillingRecipe(ItemStack input, ItemStack block) implements Recipe<RecipeInput> {

    private static final ResourceLocation ID = KnightLib.of("block_filling");

    public static final RecipeSerializer<ChaliceFillingRecipe> SERIALIZER = new Serializer();
    public static final RecipeType<ChaliceFillingRecipe> RECIPE_TYPE = new Type();

    @Override
    public boolean matches(RecipeInput inv, @NotNull Level lvl) {
        return ItemStack.isSameItem(inv.getItem(0), input);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput inv, HolderLookup.@NotNull Provider reg) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider reg) {
        return ItemStack.EMPTY;
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

        private static final MapCodec<ChaliceFillingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ItemStack.STRICT_CODEC.fieldOf("input").forGetter(ChaliceFillingRecipe::input),
                ItemStack.STRICT_CODEC.fieldOf("block").forGetter(ChaliceFillingRecipe::block)
        ).apply(inst, ChaliceFillingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ChaliceFillingRecipe> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ChaliceFillingRecipe::input,
                ItemStack.STREAM_CODEC, ChaliceFillingRecipe::block,
                ChaliceFillingRecipe::new
        );

        @Override
        public @NotNull MapCodec<ChaliceFillingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ChaliceFillingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

}
