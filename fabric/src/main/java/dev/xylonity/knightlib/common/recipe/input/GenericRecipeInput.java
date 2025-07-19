package dev.xylonity.knightlib.common.recipe.input;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record GenericRecipeInput(ItemStack input) implements net.minecraft.world.item.crafting.RecipeInput {
    @Override
    public @NotNull ItemStack getItem(int i) {
        return input;
    }

    @Override
    public int size() {
        return 1;
    }

}