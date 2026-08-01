package dev.xylonity.knightlib.client.animation.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Base renderer for block entities animated through KnightLib
 */
public abstract class KnightLibBlockEntityRenderer<T extends BlockEntity & KnightLibAnimatable> implements BlockEntityRenderer<T> {

    private final List<KnightLibRenderLayer<T>> layers = new ArrayList<>();
    private final KnightLibModelSource.InstanceCache modelCache = new KnightLibModelSource.InstanceCache();

    protected KnightLibBlockEntityRenderer() {
        ;;
    }

    /**
     * Geometry file or method reference
     */
    protected abstract KnightLibModelSource defineModel(T blockEntity);

    /**
     * Animation file or method reference
     */
    protected KnightLibAnimationSource defineAnimations(T blockEntity) {
        return KnightLibAnimationSource.none();
    }

    /**
     * Packed ARGB multiplier, evaluated once immediately before drawing
     */
    protected int getRenderColor(T blockEntity, float partialTicks, int packedLight) {
        return KnightLibColor.WHITE_ARGB;
    }

    /**
     * Calculates scale and ARGB together once for the complete render pass
     */
    protected KnightLibRenderState getRenderState(T blockEntity, float partialTicks, int packedLight) {
        return KnightLibRenderState.of(getScale(blockEntity), getRenderColor(blockEntity, partialTicks, packedLight));
    }

    /**
     * Texture source for this block entity
     */
    public abstract ResourceLocation getTextureLocation(T blockEntity);

    protected RenderType getRenderType(T blockEntity, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    protected RenderType getRenderType(T blockEntity, ResourceLocation texture, int renderColor) {
        return KnightLibColor.fromArgb(renderColor).isTranslucent()
                ? RenderType.entityTranslucent(texture)
                : getRenderType(blockEntity, texture);
    }

    /**
     * Optional atlas sprite backing the returned texture (no need to override this at all)
     */
    protected TextureAtlasSprite getTextureSprite(T blockEntity, ResourceLocation texture) {
        return null;
    }

    protected float getScale(T blockEntity) {
        return 1f;
    }

    /**
     * Whether the model rotates with the block state's horizontal facing property
     */
    protected boolean rotateByBlockFacing() {
        return false;
    }

    protected final void addEmissiveLayer(Function<T, ResourceLocation> texture) {
        addRenderLayer(new KnightLibEmissiveLayer<>(texture));
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
    protected void setupPose(T blockEntity, KnightLibModel model, float partialTicks) {
        ;;
    }

    /**
     * Called for every bone after animation/pose evaluation and before rendering
     */
    protected void setupBone(T blockEntity, KnightLibModel model, String boneName, float partialTicks) {
        ;;
    }

    @Override
    public void render(T blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        final KnightLibModel model = modelCache.resolve(defineModel(blockEntity));
        final KnightLibAnimationSource animations = defineAnimations(blockEntity);

        // Level-less block entities (JEI previews and similar) still render at rest pose
        final double now = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() + partialTicks : partialTicks;
        KnightLibAnimator.animate(blockEntity.getAnimationHandler(), model, animations::get, now);

        setupPose(blockEntity, model, partialTicks);
        model.forEachBone(boneName -> setupBone(blockEntity, model, boneName, partialTicks));

        KnightLibRenderState renderState = getRenderState(blockEntity, partialTicks, packedLight);
        if (renderState == null) {
            renderState = KnightLibRenderState.DEFAULT;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);

        if (rotateByBlockFacing()) {
            final BlockState state = blockEntity.getBlockState();
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                final Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            }

        }

        final float scale = renderState.scale();
        if (scale != 1f) {
            poseStack.scale(scale, scale, scale);
        }

        model.setupRootTransform(poseStack, 0f, false);

        final ResourceLocation baseTexture = getTextureLocation(blockEntity);
        final TextureAtlasSprite baseSprite = getTextureSprite(blockEntity, baseTexture);
        actuallyRender(blockEntity, model, baseTexture, baseSprite, partialTicks, poseStack, buffers, packedLight, packedOverlay, renderState.renderColor());

        poseStack.popPose();
    }

    /**
     * Render hook that's derived from the main render call that can alter the actual packed ARGB
     */
    protected void actuallyRender(T blockEntity, KnightLibModel model, ResourceLocation baseTexture, TextureAtlasSprite baseSprite, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay, int renderColor) {
        final KnightLibColor color = KnightLibColor.fromArgb(renderColor);
        final ResourceLocation renderTexture = baseSprite == null ? KnightLibAnimatedTextures.resolve(baseTexture) : baseSprite.atlasLocation();

        VertexConsumer baseConsumer = buffers.getBuffer(getRenderType(blockEntity, renderTexture, renderColor));
        if (baseSprite != null) {
            baseConsumer = baseSprite.wrap(baseConsumer);
        }

        model.render(poseStack, baseConsumer, packedLight, packedOverlay, color.red(), color.green(), color.blue(), color.alpha());

        final double renderTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() + partialTicks : partialTicks;
        final KnightLibRenderLayerContext<T> context = new KnightLibRenderLayerContext<>(blockEntity, model, poseStack, buffers, packedLight, packedOverlay, renderColor, partialTicks, renderTime, null, false);
        for (final KnightLibRenderLayer<T> layer : layers) {
            layer.renderIsolated(context);
        }

    }

}