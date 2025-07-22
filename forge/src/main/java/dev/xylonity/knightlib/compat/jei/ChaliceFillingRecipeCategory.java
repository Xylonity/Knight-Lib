package dev.xylonity.knightlib.compat.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Vector3f;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.IGreatChaliceInteractable;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import dev.xylonity.knightlib.common.entity.projectile.GreatChaliceStartsetRing;
import dev.xylonity.knightlib.common.recipe.ChaliceFillingRecipe;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mod.azure.azurelib.renderer.GeoBlockRenderer;
import mod.azure.azurelib.renderer.GeoEntityRenderer;
import mod.azure.azurelib.rewrite.render.block.AzBlockEntityRenderer;
import mod.azure.azurelib.rewrite.render.entity.AzEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ChaliceFillingRecipeCategory implements IRecipeCategory<ChaliceFillingRecipe> {

    public static final ResourceLocation UID = new ResourceLocation(KnightLib.MOD_ID, "block_filling");
    public static final RecipeType<ChaliceFillingRecipe> TYPE = new RecipeType<>(UID, ChaliceFillingRecipe.class);
    public static final ResourceLocation SHADOW = new ResourceLocation(KnightLib.MOD_ID, "textures/gui/shadow.png");

    private GreatChaliceBlockEntity cachedBlockEntity;
    private GreatChaliceStartsetRing cachedEntity;
    private long lastUpdateTime = 0;
    private final IDrawable icon;

    private long lastEntityAppearTime = 0;
    private static final long ENTITY_VISIBLE_DURATION = 500;
    private static final long ENTITY_INTERVAL = 2000;
    private long lastEntityTickAdvanceTime = 0;

    private ChaliceFillingRecipe currentRecipe;
    private final IDrawable shadow, arrowDown, itemBg;
    private final IDrawable bg;

    public ChaliceFillingRecipeCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(KnightLibBlocks.GREAT_CHALICE.get()));
        this.bg = gui.createBlankDrawable(120, 80);

        this.shadow = gui.createDrawable(SHADOW, 0,   0, 39, 17);
        this.arrowDown = gui.createDrawable(SHADOW, 46,  3, 33, 22);
        this.itemBg = gui.createDrawable(SHADOW, 120, 0, 19, 19);
    }

    @Override
    public @NotNull RecipeType<ChaliceFillingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.knightlib.great_chalice_fill_interaction.title");
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return bg;
    }

    @Override
    public int getWidth() {
        return 120;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ChaliceFillingRecipe rec, @NotNull IFocusGroup focuses) {
        this.currentRecipe = rec;
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 5).addItemStack(rec.input());
    }

    private GreatChaliceBlockEntity getOrCreateBlockEntity() {
        if (cachedBlockEntity == null) {
            cachedBlockEntity = new GreatChaliceBlockEntity(BlockPos.ZERO, KnightLibBlocks.GREAT_CHALICE.get().defaultBlockState());
            cachedBlockEntity.setCharges(IGreatChaliceInteractable.MAX_CHARGES);
        }
        return cachedBlockEntity;
    }

    private GreatChaliceStartsetRing getOrCreateEntity() {
        if (cachedEntity == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                cachedEntity = new GreatChaliceStartsetRing(KnightLibEntities.GREAT_CHALICE_STARSET_RING.get(), mc.level);
                cachedEntity.setNoGravity(true);
            }
        }

        if (cachedEntity != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastEntityTickAdvanceTime >= 50) {
                cachedEntity.tickCount++;
                lastEntityTickAdvanceTime = currentTime;
            }
        }

        return cachedEntity;
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();

        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
        }

        if (currentTime - lastEntityAppearTime >= ENTITY_INTERVAL) {
            lastEntityAppearTime = currentTime;

            GreatChaliceStartsetRing entity = getOrCreateEntity();
            if (entity != null) {
                entity.tickCount = 0;
            }

            int amountToAdd = 1;
            if (currentRecipe.input().getItem() instanceof IGreatChaliceInteractable stack) {
                amountToAdd = stack.getChargesToApply();
            }

            GreatChaliceBlockEntity blockEntity = getOrCreateBlockEntity();
            int current = blockEntity.getCharges();
            int next = current + amountToAdd;
            if (next > IGreatChaliceInteractable.MAX_CHARGES) {
                next = 0;
            }
            blockEntity.setCharges(next);
        }

        lastUpdateTime = currentTime;
    }

    @Override
    public void draw(@NotNull ChaliceFillingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull PoseStack stack, double mouseX, double mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SHADOW);

        shadow.draw(stack, 21, 55);
        arrowDown.draw(stack, 33, 10);
        itemBg.draw(stack, 9, 4);

        updateAnimation();

        GreatChaliceBlockEntity blockEntity = getOrCreateBlockEntity();
        GreatChaliceStartsetRing entity = getOrCreateEntity();

        @SuppressWarnings("unchecked")
        GeoBlockRenderer<GreatChaliceBlockEntity> blockRenderer = (GeoBlockRenderer<GreatChaliceBlockEntity>) Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);

        @SuppressWarnings("unchecked")
        GeoEntityRenderer<GreatChaliceStartsetRing> entityRenderer = entity != null ? (GeoEntityRenderer<GreatChaliceStartsetRing>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity) : null;

        if (blockRenderer == null) return;

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        stack.pushPose();
        stack.translate(60, 55, 20);
        stack.scale(24f, 24f, 24f);
        stack.mulPose(Vector3f.XP.rotationDegrees(-25f));
        stack.mulPose(Vector3f.YP.rotationDegrees(45f));
        stack.mulPose(Vector3f.ZP.rotationDegrees(180f));

        Matrix3f normalMat = stack.last().normal();
        Vector3f up = new Vector3f(-1, 10, -1);
        Vector3f front = new Vector3f(-1,  3, -1);
        up.normalize();
        front.normalize();
        up.transform(normalMat);
        front.transform(normalMat);
        RenderSystem.setupGui3DDiffuseLighting(up, front);

        try {
            float partial = (System.currentTimeMillis() - lastUpdateTime) / 50f;
            blockRenderer.render(blockEntity, partial, stack, buffer, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
        } catch (Exception e) {
            blockRenderer.render(blockEntity, Minecraft.getInstance().getFrameTime(), stack, buffer, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
        }
        stack.popPose();

        if (entity != null && (System.currentTimeMillis() - lastEntityAppearTime) < ENTITY_VISIBLE_DURATION) {
            stack.pushPose();
            stack.translate(60, 66, 27.5);
            stack.scale(24f, 24f, 24f);
            stack.mulPose(Vector3f.XP.rotationDegrees(-25f));
            stack.mulPose(Vector3f.YP.rotationDegrees(45f));
            stack.mulPose(Vector3f.ZP.rotationDegrees(180f));

            entityRenderer.render(entity, 0f, Minecraft.getInstance().getFrameTime(), stack, buffer, LightTexture.pack(15, 15));
            stack.popPose();
        }

        buffer.endBatch();
    }


}