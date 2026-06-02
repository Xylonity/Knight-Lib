package dev.xylonity.knightlib.compat.jei.category;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.interop.IGreatChaliceInteractable;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ChaliceFillingRecipeCategory implements IRecipeCategory<ChaliceFillingRecipe> {

    public static final ResourceLocation UID = KnightLib.of("block_filling");
    public static final RecipeType<ChaliceFillingRecipe> TYPE = new RecipeType<>(UID, ChaliceFillingRecipe.class);
    public static final ResourceLocation SHADOW = KnightLib.of("textures/gui/shadow.png");

    private GreatChaliceBlockEntity cachedBlockEntity;
    private GreatChaliceStartsetRing cachedEntity;
    private long lastUpdateTime = 0;
    private final IDrawable icon;

    private long lastEntityAppearTime = 0;
    private static final long ENTITY_VISIBLE_DURATION = 500;
    private static final long ENTITY_INTERVAL = 2000;
    private long lastEntityTickAdvanceTime = 0;

    private ChaliceFillingRecipe currentRecipe;

    public ChaliceFillingRecipeCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(KnightLibBlocks.GREAT_CHALICE.get()));
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
    public void draw(@NotNull ChaliceFillingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        RenderSystem.setShaderTexture(0, SHADOW);
        guiGraphics.blit(SHADOW, 21, 55, 0, 0, 39, 17);
        // arrow down
        guiGraphics.blit(SHADOW, 33, 10, 46, 3, 33, 22);
        // item bg input
        guiGraphics.blit(SHADOW, 9, 4, 120, 0, 19, 19);

        updateAnimation();

        GreatChaliceBlockEntity blockEntity = getOrCreateBlockEntity();
        GreatChaliceStartsetRing entity = getOrCreateEntity();

        GeoBlockRenderer<GreatChaliceBlockEntity> blockRenderer = (GeoBlockRenderer<GreatChaliceBlockEntity>) Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);
        GeoEntityRenderer<GreatChaliceStartsetRing> entityRenderer = null;
        if (entity != null) {
            EntityRenderer<?> rawRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if (rawRenderer instanceof GeoEntityRenderer<?>) {
                entityRenderer = (GeoEntityRenderer<GreatChaliceStartsetRing>) rawRenderer;
            }

        }

        if (blockRenderer == null) {
            return;
        }

        PoseStack pose = guiGraphics.pose();
        MultiBufferSource.BufferSource buffer = guiGraphics.bufferSource();

        // chalice
        pose.pushPose();
        pose.translate(60, 55, 20);
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
            blockRenderer.render(blockEntity, partialTicks, pose, buffer, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
        } catch (Exception e) {
            blockRenderer.render(blockEntity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), pose, buffer, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
        }

        pose.popPose();

        // entity
        if (entity != null && (System.currentTimeMillis() - lastEntityAppearTime < ENTITY_VISIBLE_DURATION) && entityRenderer != null) {
            pose.pushPose();
            pose.translate(60, 66, 27.5);
            pose.scale(24f, 24f, 24f);
            pose.mulPose(Axis.XP.rotationDegrees(-25f));
            pose.mulPose(Axis.YP.rotationDegrees(45f));
            pose.mulPose(Axis.ZP.rotationDegrees(180f));

            //RenderSystem.setupGui3DDiffuseLighting(up, front);

            entityRenderer.render(entity, 0, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), pose, buffer, LightTexture.pack(15, 15));

            pose.popPose();
        }

        buffer.endBatch();
    }

}