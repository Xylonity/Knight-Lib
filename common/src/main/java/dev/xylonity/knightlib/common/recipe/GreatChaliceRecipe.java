package dev.xylonity.knightlib.common.recipe;

import com.mojang.serialization.MapCodec;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.registry.KnightLibItems;
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

public final class GreatChaliceRecipe implements Recipe<RecipeInput> {

    private static final ResourceLocation ID = KnightLib.of("great_chalice_interaction");

    public static final RecipeSerializer<GreatChaliceRecipe> SERIALIZER = new Serializer();
    public static final RecipeType<GreatChaliceRecipe> RECIPE_TYPE = new Type();

    public final ItemStack input = new ItemStack(KnightLibItems.EMPTY_GRAIL.get());
    public final ItemStack output = new ItemStack(KnightLibItems.FILLED_GRAIL.get());

    @Override
    public boolean matches(RecipeInput inv, @NotNull Level lvl) {
        return ItemStack.isSameItem(inv.getItem(0), input);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput inv, HolderLookup.@NotNull Provider reg) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider reg) {
        return output.copy();
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

    public static final class Serializer implements RecipeSerializer<GreatChaliceRecipe> {

        private static final MapCodec<GreatChaliceRecipe> CODEC = MapCodec.unit(GreatChaliceRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, GreatChaliceRecipe> STREAM_CODEC = StreamCodec.unit(new GreatChaliceRecipe());

        @Override
        public @NotNull MapCodec<GreatChaliceRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, GreatChaliceRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

}
