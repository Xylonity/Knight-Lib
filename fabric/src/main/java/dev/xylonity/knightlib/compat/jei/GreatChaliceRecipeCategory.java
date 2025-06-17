package dev.xylonity.knightlib.compat.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.api.IGreatChaliceInteractable;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import dev.xylonity.knightlib.common.recipe.GreatChaliceRecipe;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class GreatChaliceRecipeCategory implements IRecipeCategory<GreatChaliceRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(KnightLib.MOD_ID, "great_chalice_interaction");
    public static final RecipeType<GreatChaliceRecipe> TYPE = new RecipeType<>(UID, GreatChaliceRecipe.class);

    public static final ResourceLocation SHADOW = new ResourceLocation(KnightLib.MOD_ID, "textures/gui/shadow.png");

    private final IDrawable icon;

    private GreatChaliceBlockEntity cachedBlockEntity;
    private long lastUpdateTime = 0;

    public GreatChaliceRecipeCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(KnightLibBlocks.GREAT_CHALICE.get()));
    }

    @Override
    public @NotNull RecipeType<GreatChaliceRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.knightlib.great_chalice_grail_interaction.title");
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GreatChaliceRecipe rec, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 5).addItemStack(rec.input);

        builder.addSlot(RecipeIngredientRole.OUTPUT, 130, 42).addItemStack(rec.output);
    }

    private GreatChaliceBlockEntity getOrCreateBlockEntity() {
        if (cachedBlockEntity == null) {
            cachedBlockEntity = new GreatChaliceBlockEntity(BlockPos.ZERO, KnightLibBlocks.GREAT_CHALICE.get().defaultBlockState());
            cachedBlockEntity.setCharges(IGreatChaliceInteractable.MAX_CHARGES);
        }

        return cachedBlockEntity;
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
        }

        if (currentTime - lastUpdateTime >= 50) {
            lastUpdateTime = currentTime;
        }
    }

    @Override
    public void draw(@NotNull GreatChaliceRecipe recipe, @NotNull IRecipeSlotsView slots, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        RenderSystem.setShaderTexture(0, SHADOW);
        // chalice shadow
        guiGraphics.blit(SHADOW, 20, 55, 0, 0, 39, 17);
        // arrow down
        guiGraphics.blit(SHADOW, 32, 10, 46, 3, 33, 22);
        // arrow right
        guiGraphics.blit(SHADOW, 85, 45, 81, 6, 39, 12);
        // item bg input
        guiGraphics.blit(SHADOW, 129, 41, 120, 0, 19, 19);
        // item bg output
        guiGraphics.blit(SHADOW, 9, 4, 120, 0, 19, 19);

        updateAnimation();

        GreatChaliceBlockEntity be = getOrCreateBlockEntity();

        @SuppressWarnings("unchecked")
        GeoBlockRenderer<GreatChaliceBlockEntity> renderer = (GeoBlockRenderer<GreatChaliceBlockEntity>) Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);

        if (renderer == null) return;

        PoseStack pose = guiGraphics.pose();
        MultiBufferSource.BufferSource buffer = guiGraphics.bufferSource();

        pose.pushPose();
        pose.translate(59, 55, 0);
        pose.scale(24f, 24f, 24f);
        pose.mulPose(Axis.XP.rotationDegrees(-25f));
        pose.mulPose(Axis.YP.rotationDegrees(45f));
        pose.mulPose(Axis.ZP.rotationDegrees(180f));

        Matrix3f normalMat = pose.last().normal();

        Vector3f up = new Vector3f(-1, 10, -1);
        Vector3f front = new Vector3f(-1, 3, -1);

        normalMat.transform(up).normalize();
        normalMat.transform(front).normalize();

        RenderSystem.setupGui3DDiffuseLighting(up, front);

        try {
            float partialTicks = (float)((System.currentTimeMillis() - lastUpdateTime) / 50.0);

            renderer.render(be, partialTicks, pose, buffer, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
        } catch (Exception e) {
            renderer.render(be, Minecraft.getInstance().getFrameTime(), pose, buffer, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
        }

        pose.popPose();
        buffer.endBatch();
    }

}