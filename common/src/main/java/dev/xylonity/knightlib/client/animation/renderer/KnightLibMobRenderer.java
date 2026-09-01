package dev.xylonity.knightlib.client.animation.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.api.util.KnightLibColor;
import dev.xylonity.knightlib.client.animation.KnightLibAnimationSource;
import dev.xylonity.knightlib.client.animation.KnightLibModelSource;
import dev.xylonity.knightlib.client.animation.layer.impl.KnightLibEmissiveLayer;
import dev.xylonity.knightlib.client.animation.layer.impl.KnightLibOverlayLayer;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayer;
import dev.xylonity.knightlib.client.animation.model.KnightLibLivingModel;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import dev.xylonity.knightlib.client.texture.KnightLibAnimatedTextures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.Objects;
import java.util.function.Function;

/**
 * Base renderer for mob entities animated through KnightLib.
 */
public abstract class KnightLibMobRenderer<T extends Mob & KnightLibAnimatable> extends MobRenderer<T, KnightLibLivingModel<T>> {

    private KnightLibRenderState activeRenderState = KnightLibRenderState.DEFAULT;

    protected KnightLibMobRenderer(EntityRendererProvider.Context context, float shadowRadius) {
        super(context, new KnightLibLivingModel<>(), shadowRadius);
        this.model.configure(this::defineModel, this::defineAnimations, this::setupModelPose);
        this.model.configureAttachments(this::getHeadBone, this::getArmBone);
        this.model.configureRenderColor((entity, partialTick) -> activeRenderState.color());
    }

    protected KnightLibMobRenderer(EntityRendererProvider.Context context) {
        this(context, 0.5f);
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
    protected int getRenderColor(T entity, float partialTick, int packedLight) {
        return KnightLibColor.WHITE_ARGB;
    }

    /**
     * Calculates scale and ARGB together once for the complete render pass
     */
    protected KnightLibRenderState getRenderState(T entity, float partialTick, int packedLight) {
        return KnightLibRenderState.of(getScale(entity), getRenderColor(entity, partialTick, packedLight));
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

    protected String getHeadBone(T entity) {
        return "head";
    }

    protected String getArmBone(T entity, HumanoidArm arm) {
        return arm == HumanoidArm.LEFT ? "left_arm" : "right_arm";
    }

    protected void setupPose(T entity, KnightLibModel model, float partialTick) {
        ;;
    }

    /**
     * Called every frame after animations are applied and before rendering
     */
    protected void setupPose(T entity, KnightLibModel model, float limbSwing, float limbSwingAmount, float partialTick, float netHeadYaw, float headPitch) {
        setupPose(entity, model, partialTick);
    }

    /**
     * Called for every bone after animation/pose evaluation and before rendering
     */
    protected void setupBone(T entity, KnightLibModel model, String boneName, float partialTick) {
        ;;
    }

    /**
     * Called immediately before the complete vanilla living-entity render pass
     */
    protected void beforeRender(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        ;;
    }

    /**
     * Called after vanilla has rendered the entity and all its layers
     */
    protected void afterRender(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        ;;
    }

    private void setupModelPose(T entity, KnightLibModel model, float limbSwing, float limbSwingAmount, float partialTick, float netHeadYaw, float headPitch) {
        setupPose(entity, model, limbSwing, limbSwingAmount, partialTick, netHeadYaw, headPitch);
        model.forEachBone(boneName -> setupBone(entity, model, boneName, partialTick));
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
        addLayer(new KnightLibLivingRenderLayer<>(this, Objects.requireNonNull(layer, "layer")));
    }

    /**
     * Prefer the before/after hooks when possible
     */
    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        final KnightLibRenderState previousState = activeRenderState;
        final KnightLibRenderState renderState = getRenderState(entity, partialTick, packedLight);
        activeRenderState = renderState == null ? KnightLibRenderState.DEFAULT : renderState;
        try {
            actuallyRender(entity, entityYaw, partialTick, poseStack, buffers, packedLight, activeRenderState.renderColor());
        }
        finally {
            activeRenderState = previousState;
        }

    }

    /**
     * Executes the complete vanilla mob render pipeline
     */
    protected void actuallyRender(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int renderColor) {
        final KnightLibRenderState previousState = activeRenderState;
        activeRenderState = previousState.withRenderColor(renderColor);
        try {
            beforeRender(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
            super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
            afterRender(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        }
        finally {
            activeRenderState = previousState;
        }

    }

    @Override
    protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        final ResourceLocation texture = KnightLibAnimatedTextures.resolve(getTextureLocation(entity));
        if (translucent) {
            return RenderType.itemEntityTranslucentCull(texture);
        }
        if (bodyVisible) {
            return getRenderType(entity, texture, activeRenderState.renderColor());
        }
        if (glowing) {
            return RenderType.outline(texture);
        }

        return null;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        super.scale(entity, poseStack, partialTick);
        final float scale = activeRenderState.scale();
        if (scale != 1f) {
            poseStack.scale(scale, scale, scale);
        }

    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        if (super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ)) {
            return true;
        }

        final AABB box = entity.getBoundingBoxForCulling().inflate(Math.max(0.0, getCullingInflation(entity)));
        return !box.hasNaN() && frustum.isVisible(box);
    }

}
