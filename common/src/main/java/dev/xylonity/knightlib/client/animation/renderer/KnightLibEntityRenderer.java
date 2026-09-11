package dev.xylonity.knightlib.client.animation.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.api.util.KnightLibColor;
import dev.xylonity.knightlib.client.animation.KnightLibAnimationSource;
import dev.xylonity.knightlib.client.animation.KnightLibAnimator;
import dev.xylonity.knightlib.client.animation.KnightLibKeyframeEvents;
import dev.xylonity.knightlib.client.animation.KnightLibModelSource;
import dev.xylonity.knightlib.client.animation.layer.impl.KnightLibEmissiveLayer;
import dev.xylonity.knightlib.client.animation.layer.impl.KnightLibOverlayLayer;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayer;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayerContext;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import dev.xylonity.knightlib.client.texture.KnightLibAnimatedTextures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Base renderer for entities animated through KnightLib.
 */
public abstract class KnightLibEntityRenderer<T extends Entity & KnightLibAnimatable> extends EntityRenderer<T> {

    private final List<KnightLibRenderLayer<T>> layers = new ArrayList<>();
    private final KnightLibModelSource.InstanceCache modelCache = new KnightLibModelSource.InstanceCache();

    protected KnightLibEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * Geometry file or method reference
     */
    protected abstract KnightLibModelSource defineModel(T entity);

    /**
     * Animation file or method reference
     */
    protected KnightLibAnimationSource defineAnimations(T entity) {
        return KnightLibAnimationSource.none();
    }

    /**
     * Packed ARGB multiplier, evaluated once immediately before the entity is drawn
     */
    protected int getRenderColor(T entity, float partialTicks, int packedLight) {
        return KnightLibColor.WHITE_ARGB;
    }

    /**
     * Calculates scale and ARGB together once for the complete render pass
     */
    protected KnightLibRenderState getRenderState(T entity, float partialTicks, int packedLight) {
        return KnightLibRenderState.of(getScale(entity), getRenderColor(entity, partialTicks, packedLight));
    }

    /**
     * Render type of the base texture pass
     */
    protected RenderType getRenderType(T entity, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    protected RenderType getRenderType(T entity, ResourceLocation texture, int renderColor) {
        return KnightLibColor.fromArgb(renderColor).isTranslucent()
                ? RenderType.entityTranslucent(texture)
                : getRenderType(entity, texture);
    }

    protected float getScale(T entity) {
        return 1f;
    }

    /**
     * Extra culling margin in blocks
     */
    protected double getCullingInflation(T entity) {
        return 0.5;
    }

    protected final void addEmissiveLayer(Function<T, ResourceLocation> texture) {
        addRenderLayer(new KnightLibEmissiveLayer<>(texture));
    }

    protected final void addEmissiveLayer(Function<T, ResourceLocation> texture, KnightLibEmissiveLayer.AnimatedTint<T> animatedTint) {
        addRenderLayer(new KnightLibEmissiveLayer<>(texture, animatedTint));
    }

    protected final void addOverlayLayer(Function<T, ResourceLocation> texture) {
        addRenderLayer(new KnightLibOverlayLayer<>(texture));
    }

    /**
     * Adds an arbitrary render layer
     */
    protected final void addRenderLayer(KnightLibRenderLayer<T> layer) {
        layers.add(Objects.requireNonNull(layer, "layer"));
    }

    /**
     * Called every frame after animations are applied and before rendering
     */
    protected void setupPose(T entity, KnightLibModel model, float partialTicks) {
        ;;
    }

    /**
     * Called for every bone after animation/pose evaluation and before rendering
     */
    protected void setupBone(T entity, KnightLibModel model, String boneName, float partialTicks) {
        ;;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        final KnightLibModel model = modelCache.resolve(defineModel(entity));
        final KnightLibAnimationSource animations = defineAnimations(entity);

        final double now = entity.level().getGameTime() + partialTicks;
        final KnightLibAnimator.DeferredEvents keyframeEvents = KnightLibAnimator.animateDeferred(entity.getAnimationHandler(), model, animations::get, now);

        setupPose(entity, model, partialTicks);
        model.forEachBone(boneName -> setupBone(entity, model, boneName, partialTicks));

        KnightLibRenderState renderState = getRenderState(entity, partialTicks, packedLight);
        if (renderState == null) {
            renderState = KnightLibRenderState.DEFAULT;
        }

        final float bodyYaw = entity instanceof LivingEntity living
                ? Mth.rotLerp(partialTicks, living.yBodyRotO, living.yBodyRot)
                : Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());

        poseStack.pushPose();
        float scale = renderState.scale();
        if (scale != 1f) {
            poseStack.scale(scale, scale, scale);
        }

        model.setupRootTransform(poseStack, bodyYaw, true);
        KnightLibKeyframeEvents.dispatch(entity.getAnimationHandler(), keyframeEvents, model, poseStack, false);

        final int packedOverlay = entity instanceof LivingEntity living
                ? LivingEntityRenderer.getOverlayCoords(living, 0f)
                : OverlayTexture.NO_OVERLAY;

        final ResourceLocation baseTexture = getTextureLocation(entity);
        actuallyRender(entity, model, baseTexture, partialTicks, poseStack, buffers, packedLight, packedOverlay, renderState.renderColor());

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffers, packedLight);
    }

    /**
     * Render hook that's derived from the main render call that can alter the actual packed ARGB
     */
    protected void actuallyRender(T entity, KnightLibModel model, ResourceLocation baseTexture, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay, int renderColor) {
        final KnightLibColor color = KnightLibColor.fromArgb(renderColor);
        final ResourceLocation renderTexture = KnightLibAnimatedTextures.resolve(baseTexture);
        model.render(poseStack, buffers.getBuffer(getRenderType(entity, renderTexture, renderColor)), packedLight, packedOverlay, color.red(), color.green(), color.blue(), color.alpha());

        final double renderTime = entity.level().getGameTime() + partialTicks;
        final KnightLibRenderLayerContext<T> context = new KnightLibRenderLayerContext<>(
                entity, model, poseStack, buffers, packedLight, packedOverlay, renderColor,
                partialTicks, renderTime, null, false
        );
        for (final KnightLibRenderLayer<T> layer : layers) {
            layer.renderIsolated(context);
        }

    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        if (!entity.shouldRender(cameraX, cameraY, cameraZ)) {
            return false;
        }
        if (super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ)) {
            return true;
        }

        final double inflation = Math.max(0, getCullingInflation(entity));
        final AABB box = entity.getBoundingBoxForCulling().inflate(inflation);
        return !box.hasNaN() && frustum.isVisible(box);
    }

}
