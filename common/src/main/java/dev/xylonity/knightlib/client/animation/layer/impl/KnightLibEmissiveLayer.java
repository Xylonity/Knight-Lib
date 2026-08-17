package dev.xylonity.knightlib.client.animation.layer.impl;

import dev.xylonity.knightlib.api.util.KnightLibColor;
import dev.xylonity.knightlib.client.animation.layer.KnightLibRenderLayerContext;
import dev.xylonity.knightlib.client.animation.layer.KnightLibTextureLayer;
import dev.xylonity.knightlib.client.KnightLibRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Render layer for a trivial fullbright texture pass over the current animated model.
 * Depth pass must be enabled for opaque layers.
 */
public class KnightLibEmissiveLayer<T> extends KnightLibTextureLayer<T> {

    private final boolean writeDepth;

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture) {
        this(texture, true);
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, boolean writeDepth) {
        super(texture);
        this.writeDepth = writeDepth;
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, int tint) {
        this(texture, tint, true);
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, int tint, boolean writeDepth) {
        super(texture, tint);
        this.writeDepth = writeDepth;
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, ToIntFunction<T> tint) {
        this(texture, tint, true);
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, ToIntFunction<T> tint, boolean writeDepth) {
        super(texture, tint);
        this.writeDepth = writeDepth;
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, KnightLibColor tint) {
        this(texture, tint, true);
    }

    public KnightLibEmissiveLayer(Function<T, ResourceLocation> texture, KnightLibColor tint, boolean writeDepth) {
        super(texture, tint);
        this.writeDepth = writeDepth;
    }

    @Override
    protected RenderType getRenderType(KnightLibRenderLayerContext<T> context, ResourceLocation texture, int resolvedColor) {
        return KnightLibRenderTypes.entityEmissive(texture, writeDepth);
    }

    @Override
    protected int getPackedLight(KnightLibRenderLayerContext<T> context) {
        return LightTexture.FULL_BRIGHT;
    }

}