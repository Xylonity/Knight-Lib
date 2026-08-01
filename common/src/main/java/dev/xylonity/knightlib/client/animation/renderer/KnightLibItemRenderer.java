package dev.xylonity.knightlib.client.animation.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.item.KnightLibAnimatedItem;
import dev.xylonity.knightlib.api.item.KnightLibRenderedItem;
import dev.xylonity.knightlib.api.util.KnightLibColor;
import dev.xylonity.knightlib.client.animation.KnightLibAnimationSource;
import dev.xylonity.knightlib.client.animation.KnightLibAnimator;
import dev.xylonity.knightlib.client.animation.KnightLibModelSource;
import dev.xylonity.knightlib.client.animation.layer.impl.KnightLibEmissiveLayer;
import dev.xylonity.knightlib.client.animation.layer.impl.KnightLibOverlayLayer;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayer;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayerContext;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import dev.xylonity.knightlib.client.texture.KnightLibAnimatedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Base item renderer (BEWLR) for items rendered with a KnightLib model
 */
public abstract class KnightLibItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final List<KnightLibRenderLayer<ItemStack>> layers = new ArrayList<>();
    private final KnightLibModelSource.InstanceCache modelCache = new KnightLibModelSource.InstanceCache();

    protected KnightLibItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /**
     * Geometry file or method reference
     */
    protected abstract KnightLibModelSource defineModel(ItemStack stack);

    /**
     * Animation file or method reference
     */
    protected KnightLibAnimationSource defineAnimations(ItemStack stack) {
        return KnightLibAnimationSource.none();
    }

    /**
     * Packed ARGB multiplier, evaluated once immediately before drawing
     */
    protected int getRenderColor(ItemStack stack, ItemDisplayContext displayContext, float partialTicks, int packedLight) {
        return KnightLibColor.WHITE_ARGB;
    }

    /**
     * Calculates scale and ARGB together once for the complete render pass
     */
    protected KnightLibRenderState getRenderState(ItemStack stack, ItemDisplayContext displayContext, float partialTicks, int packedLight) {
        return KnightLibRenderState.of(getScale(stack), getRenderColor(stack, displayContext, partialTicks, packedLight));
    }

    /**
     * Texture source for this stack
     */
    public abstract ResourceLocation getTextureLocation(ItemStack stack);

    protected RenderType getRenderType(ItemStack stack, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    protected RenderType getRenderType(ItemStack stack, ResourceLocation texture, int renderColor) {
        return KnightLibColor.fromArgb(renderColor).isTranslucent()
                ? RenderType.entityTranslucent(texture)
                : getRenderType(stack, texture);
    }

    /**
     * Optional atlas sprite backing the returned texture (no need to override this at all)
     */
    protected TextureAtlasSprite getTextureSprite(ItemStack stack, ResourceLocation texture) {
        return null;
    }

    protected float getScale(ItemStack stack) {
        return 1f;
    }

    /**
     * Vertical offset of the model inside the unit cube
     */
    protected float getVerticalOffset(ItemStack stack) {
        return 0.51f;
    }

    /**
     * Name of a looping animation driven by the game clock, or null for the rest pose (convenience)
     */
    protected String getAmbientAnimation(ItemStack stack) {
        return null;
    }

    protected float getAmbientAnimationSpeed(ItemStack stack) {
        return 1f;
    }

    protected final void addEmissiveLayer(Function<ItemStack, ResourceLocation> texture) {
        addRenderLayer(new KnightLibEmissiveLayer<>(texture));
    }

    protected final void addOverlayLayer(Function<ItemStack, ResourceLocation> texture) {
        addRenderLayer(new KnightLibOverlayLayer<>(texture));
    }

    /**
     * Adds an arbitrary render layer
     */
    protected final void addRenderLayer(KnightLibRenderLayer<ItemStack> layer) {
        layers.add(Objects.requireNonNull(layer, "layer"));
    }

    /**
     * Called every frame after animations are applied and before rendering
     */
    protected void setupPose(ItemStack stack, KnightLibModel model, float partialTicks) {
        ;;
    }

    /**
     * Called every frame after animations are applied and before rendering
     */
    protected void setupPose(ItemStack stack, ItemDisplayContext displayContext, KnightLibModel model, float partialTicks) {
        setupPose(stack, model, partialTicks);
    }

    /**
     * Called for every bone after animation/pose evaluation and before rendering
     */
    protected void setupBone(ItemStack stack, ItemDisplayContext displayContext, KnightLibModel model, String boneName, float partialTicks) {
        ;;
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext displayContext, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int packedLight, int packedOverlay) {
        final KnightLibModel model = modelCache.resolve(defineModel(stack));

        final Minecraft minecraft = Minecraft.getInstance();
        final float partialTicks = minecraft.getFrameTime();
        final double now = minecraft.level != null ? minecraft.level.getGameTime() + partialTicks : partialTicks;

        final KnightLibAnimationSource animations = defineAnimations(stack);
        KnightLibAnimationHandler handler = null;
        if (stack.getItem() instanceof KnightLibAnimatedItem item) {
            handler = item.getAnimationHandler(stack, minecraft.level);
            handler.tick();
        }

        if (handler != null && !handler.controllers().isEmpty()) {
            KnightLibAnimator.animate(handler, model, animations::get, now);
        }
        else {
            final String ambient = getAmbientAnimation(stack);
            final KnightLibAnimation animation = ambient == null ? null : animations.get(ambient);
            if (animation != null) {
                KnightLibAnimator.applyAmbient(model, animation, now, getAmbientAnimationSpeed(stack));
            }
            else {
                model.resetPose();
            }

        }

        setupPose(stack, displayContext, model, partialTicks);
        model.forEachBone(boneName -> setupBone(stack, displayContext, model, boneName, partialTicks));
        KnightLibRenderState renderState = getRenderState(stack, displayContext, partialTicks, packedLight);
        if (renderState == null) {
            renderState = KnightLibRenderState.DEFAULT;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, getVerticalOffset(stack), 0.5);

        final float scale = renderState.scale();
        if (scale != 1f) {
            poseStack.scale(scale, scale, scale);
        }

        model.setupRootTransform(poseStack, 0f, false);

        final ResourceLocation baseTexture = getTextureLocation(stack);
        final TextureAtlasSprite baseSprite = getTextureSprite(stack, baseTexture);
        actuallyRender(stack, displayContext, model, baseTexture, baseSprite, partialTicks, poseStack, buffers, packedLight, packedOverlay, renderState.renderColor());

        poseStack.popPose();
    }

    /**
     * Render hook that's derived from the main render call that can alter the actual packed ARGB
     */
    protected void actuallyRender(ItemStack stack, ItemDisplayContext displayContext, KnightLibModel model, ResourceLocation baseTexture, TextureAtlasSprite baseSprite, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay, int renderColor) {
        final KnightLibColor color = KnightLibColor.fromArgb(renderColor);
        final ResourceLocation renderTexture = baseSprite == null ? KnightLibAnimatedTextures.resolve(baseTexture) : baseSprite.atlasLocation();
        final RenderType baseRenderType = getRenderType(stack, renderTexture, renderColor);
        VertexConsumer baseConsumer = ItemRenderer.getFoilBufferDirect(buffers, baseRenderType, false, stack.hasFoil());
        if (baseSprite != null) {
            baseConsumer = baseSprite.wrap(baseConsumer);
        }

        model.render(poseStack, baseConsumer, packedLight, packedOverlay, color.red(), color.green(), color.blue(), color.alpha());

        final Minecraft minecraft = Minecraft.getInstance();
        final double renderTime = minecraft.level != null ? minecraft.level.getGameTime() + partialTicks : partialTicks;
        final KnightLibRenderLayerContext<ItemStack> context = new KnightLibRenderLayerContext<>(
                stack, model, poseStack, buffers, packedLight, packedOverlay, renderColor,
                partialTicks, renderTime, displayContext, false
        );
        for (final KnightLibRenderLayer<ItemStack> layer : layers) {
            layer.renderIsolated(context);
        }

    }

}