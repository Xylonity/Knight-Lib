package dev.xylonity.knightlib.common.recipe;

import com.google.gson.JsonObject;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record ChaliceFillingRecipe(ItemStack input, ItemStack block) implements Recipe<SimpleContainer> {

    private static final ResourceLocation ID = new ResourceLocation(KnightLib.MOD_ID, "block_filling");

    public static final RecipeSerializer<ChaliceFillingRecipe> SERIALIZER = new Serializer();
    public static final RecipeType<ChaliceFillingRecipe> RECIPE_TYPE = new Type();

    @Override
    public boolean matches(SimpleContainer inv, @NotNull Level lvl) {
        return ItemStack.isSame(inv.getItem(0), input);
    }

    @Override
    public ItemStack assemble(SimpleContainer container) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public @NotNull ResourceLocation getId() {
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
        @Override
        public @NotNull ChaliceFillingRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            return new ChaliceFillingRecipe(new ItemStack(KnightLibItems.SMALL_ESSENCE.get()), new ItemStack(KnightLibBlocks.GREAT_CHALICE.get()));
        }

        @Override
        public ChaliceFillingRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buf) {
            ItemStack input = buf.readItem();
            ItemStack block = buf.readItem();
            return new ChaliceFillingRecipe(input, block);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull ChaliceFillingRecipe rec) {
            buf.writeItem(rec.input);
            buf.writeItem(rec.block);
        }
    }

}