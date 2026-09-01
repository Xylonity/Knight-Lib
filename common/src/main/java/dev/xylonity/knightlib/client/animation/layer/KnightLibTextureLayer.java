package dev.xylonity.knightlib.client.animation.layer;

import dev.xylonity.knightlib.api.util.KnightLibColor;
import dev.xylonity.knightlib.client.texture.KnightLibAnimatedTextures;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Base class for a texture pass over the current animated model
 */
public abstract class KnightLibTextureLayer<T> extends KnightLibRenderLayer<T> {

    private final Function<T, ResourceLocation> texture;
    private final ToIntFunction<T> tint;

    protected KnightLibTextureLayer(Function<T, ResourceLocation> texture) {
        this(texture, constantTint(KnightLibColor.WHITE_ARGB));
    }

    protected KnightLibTextureLayer(Function<T, ResourceLocation> texture, int tint) {
        this(texture, constantTint(tint));
    }

    protected KnightLibTextureLayer(Function<T, ResourceLocation> texture, KnightLibColor tint) {
        this(texture, tint == null ? KnightLibColor.WHITE_ARGB : tint.toArgb());
    }

    protected KnightLibTextureLayer(Function<T, ResourceLocation> texture, ToIntFunction<T> tint) {
        this.texture = Objects.requireNonNull(texture, "texture");
        this.tint = Objects.requireNonNull(tint, "tint");
    }

    @Override
    public boolean shouldRender(KnightLibRenderLayerContext<T> context) {
        return !(context.target() instanceof Entity entity) || !entity.isInvisible();
    }

    @Override
    public final void render(KnightLibRenderLayerContext<T> context) {
        ResourceLocation resolvedTexture = getTexture(context);
        if (resolvedTexture == null) {
            return;
        }

        resolvedTexture = KnightLibAnimatedTextures.resolve(resolvedTexture);

        final int resolvedColor = context.multiplyColor(getColor(context));
        final RenderType renderType = Objects.requireNonNull(getRenderType(context, resolvedTexture, resolvedColor), "renderType");
        renderTexture(context, resolvedTexture, renderType, resolvedColor);
    }

    /**
     * Returns null to skip this texture pass for the current frame
     */
    @Nullable
    protected ResourceLocation getTexture(KnightLibRenderLayerContext<T> context) {
        return texture.apply(context.target());
    }

    /**
     * Layer packed ARGB multiplied by the owning renderer's color before drawing
     */
    protected int getColor(KnightLibRenderLayerContext<T> context) {
        return tint.applyAsInt(context.target());
    }

    protected abstract RenderType getRenderType(KnightLibRenderLayerContext<T> context, ResourceLocation texture, int resolvedColor);

    /**
     * Draws the resolved texture pass. Subclasses may add a preparatory pass before delegating here.
     */
    protected void renderTexture(KnightLibRenderLayerContext<T> context, ResourceLocation texture, RenderType renderType, int resolvedColor) {
        context.renderModel(renderType, getPackedLight(context), getPackedOverlay(context), resolvedColor);
    }

    protected int getPackedLight(KnightLibRenderLayerContext<T> context) {
        return context.packedLight();
    }

    protected int getPackedOverlay(KnightLibRenderLayerContext<T> context) {
        return context.packedOverlay();
    }

    /**
     * Resolves this layer's target tint against a packed renderer ARGB
     */
    public int resolveColor(T target, int baseColor) {
        return KnightLibColor.multiplyArgb(baseColor, tint.applyAsInt(target));
    }

    public KnightLibColor resolveColor(T target, KnightLibColor baseColor) {
        final int resolvedBase = baseColor == null ? KnightLibColor.WHITE_ARGB : baseColor.toArgb();
        return KnightLibColor.fromArgb(resolveColor(target, resolvedBase));
    }

    private static <T> ToIntFunction<T> constantTint(int tint) {
        return ignored -> tint;
    }

}
