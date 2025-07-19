package dev.xylonity.knightlib.compat.jei;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.recipe.ChaliceFillingRecipe;
import dev.xylonity.knightlib.common.recipe.GreatChaliceRecipe;
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

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(KnightLib.MOD_ID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new GreatChaliceRecipeCategory(reg.getJeiHelpers().getGuiHelper()));
        reg.addRecipeCategories(new ChaliceFillingRecipeCategory(reg.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        reg.addRecipes(GreatChaliceRecipeCategory.TYPE, List.of(new GreatChaliceRecipe(
                new ItemStack(KnightLibItems.EMPTY_GRAIL.get()),
                new ItemStack(KnightLibItems.FILLED_GRAIL.get()))
        ));
        reg.addRecipes(ChaliceFillingRecipeCategory.TYPE, List.of(
                new ChaliceFillingRecipe(new ItemStack(KnightLibItems.SMALL_ESSENCE.get()), new ItemStack(KnightLibBlocks.GREAT_CHALICE.get())),
                new ChaliceFillingRecipe(new ItemStack(KnightLibItems.GREAT_ESSENCE.get()), new ItemStack(KnightLibBlocks.GREAT_CHALICE.get()))
        ));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(KnightLibBlocks.GREAT_CHALICE.get()), GreatChaliceRecipeCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(KnightLibBlocks.GREAT_CHALICE.get()), ChaliceFillingRecipeCategory.TYPE);
    }

}
