package dev.xylonity.knightlib.client.animation.layer.impl;

import dev.xylonity.knightlib.api.util.KnightLibColor;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayerContext;
import dev.xylonity.knightlib.client.animation.layer.KnightLibTextureLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Render layer for a trivial texture pass over the current animated model
 */
public class KnightLibOverlayLayer<T> extends KnightLibTextureLayer<T> {

    public KnightLibOverlayLayer(Function<T, ResourceLocation> texture) {
        super(texture);
    }

    public KnightLibOverlayLayer(Function<T, ResourceLocation> texture, int tint) {
        super(texture, tint);
    }

    public KnightLibOverlayLayer(Function<T, ResourceLocation> texture, ToIntFunction<T> tint) {
        super(texture, tint);
    }

    public KnightLibOverlayLayer(Function<T, ResourceLocation> texture, KnightLibColor tint) {
        super(texture, tint);
    }

    @Override
    protected RenderType getRenderType(KnightLibRenderLayerContext<T> context, ResourceLocation texture, int resolvedColor) {
        return KnightLibColor.fromArgb(resolvedColor).isTranslucent()
                ? RenderType.entityTranslucent(texture)
                : RenderType.entityCutoutNoCull(texture);
    }

}