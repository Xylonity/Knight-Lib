package dev.xylonity.knightlib.client.animation.layer.impl;

import dev.xylonity.knightlib.api.util.KnightLibColor;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayerContext;
import dev.xylonity.knightlib.client.animation.layer.KnightLibTextureLayer;
import dev.xylonity.knightlib.client.KnightLibRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Render layer for a trivial fullbright texture pass over the current animated model.
 * Depth pass must be enabled for opaque layers.
 */
public class KnightLibEmissiveLayer<T> extends KnightLibTextureLayer<T> {

    private final boolean writeDepth;
    private final AnimatedTint<T> animatedTint;

    @FunctionalInterface
    public interface AnimatedTint<T> {
        int applyAsInt(T target, double renderTime);
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture) {
        this(texture, true);
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, boolean writeDepth) {
        super(texture);
        this.writeDepth = writeDepth;
        this.animatedTint = null;
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, int tint) {
        this(texture, tint, true);
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, int tint, boolean writeDepth) {
        super(texture, tint);
        this.writeDepth = writeDepth;
        this.animatedTint = null;
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, ToIntFunction<T> tint) {
        this(texture, tint, true);
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, ToIntFunction<T> tint, boolean writeDepth) {
        super(texture, tint);
        this.writeDepth = writeDepth;
        this.animatedTint = null;
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, KnightLibColor tint) {
        this(texture, tint, true);
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, KnightLibColor tint, boolean writeDepth) {
        super(texture, tint);
        this.writeDepth = writeDepth;
        this.animatedTint = null;
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, AnimatedTint<T> animatedTint) {
        this(texture, animatedTint, true);
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, AnimatedTint<T> animatedTint, boolean writeDepth) {
        super(texture);
        this.writeDepth = writeDepth;
        this.animatedTint = Objects.requireNonNull(animatedTint, "animatedTint");
    }

    @Override
    protected int getColor(KnightLibRenderLayerContext<T> context) {
        if (animatedTint != null) {
            return animatedTint.applyAsInt(context.target(), context.renderTime());
        }

        return super.getColor(context);
    }

    @Override
    protected RenderType getRenderType(KnightLibRenderLayerContext<T> context, ResourceLocation texture, int resolvedColor) {
        return KnightLibRenderTypes.entityEmissive(texture, false);
    }

    @Override
    protected void renderTexture(KnightLibRenderLayerContext<T> context, ResourceLocation texture, RenderType renderType, int resolvedColor) {
        if (writeDepth) {
            context.renderModel(KnightLibRenderTypes.entityEmissiveDepthMask(texture), getPackedLight(context), getPackedOverlay(context), resolvedColor);
        }

        super.renderTexture(context, texture, renderType, resolvedColor);
    }

    @Override
    protected int getPackedLight(KnightLibRenderLayerContext<T> context) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    protected int getPackedOverlay(KnightLibRenderLayerContext<T> context) {
        return OverlayTexture.NO_OVERLAY;
    }

}