package dev.xylonity.knightlib.common.recipe.input;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xylonity.knightlib.KnightLib;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record ChaliceFillingRecipeInput(ItemStack input) implements RecipeInput {
    @Override
    public @NotNull ItemStack getItem(int i) {
        return input;
    }

    @Override
    public int size() {
        return 1;
    }

}