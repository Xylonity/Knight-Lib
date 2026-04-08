package dev.xylonity.knightlib.compat.jei;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.recipe.ChaliceFillingRecipe;
import dev.xylonity.knightlib.common.recipe.GreatChaliceRecipe;
import dev.xylonity.knightlib.compat.jei.category.ChaliceFillingRecipeCategory;
import dev.xylonity.knightlib.compat.jei.category.GreatChaliceRecipeCategory;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public final class KnightLibPlugin implements IModPlugin {

    private static final ResourceLocation UID = KnightLib.of("jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GreatChaliceRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ChaliceFillingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(GreatChaliceRecipeCategory.TYPE, List.of(new GreatChaliceRecipe()));
        registration.addRecipes(ChaliceFillingRecipeCategory.TYPE, List.of(
                new ChaliceFillingRecipe(new ItemStack(KnightLibItems.SMALL_ESSENCE.get()), new ItemStack(KnightLibBlocks.GREAT_CHALICE.get())),
                new ChaliceFillingRecipe(new ItemStack(KnightLibItems.GREAT_ESSENCE.get()), new ItemStack(KnightLibBlocks.GREAT_CHALICE.get()))
        ));}

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(KnightLibBlocks.GREAT_CHALICE.get()), GreatChaliceRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(KnightLibBlocks.GREAT_CHALICE.get()), ChaliceFillingRecipeCategory.TYPE);
    }

}
