package dev.xylonity.knightlib.client.animation.renderer;

import dev.xylonity.knightlib.api.util.KnightLibColor;

/**
 * Immutable wrapper of properties calculated once for a complete model render.
 */
public record KnightLibRenderState(
        float scale,
        int renderColor
) {

    public static final KnightLibRenderState DEFAULT = new KnightLibRenderState(1f, KnightLibColor.WHITE_ARGB);

    public KnightLibRenderState {
        scale = Float.isFinite(scale) ? scale : 1f;
    }

    public KnightLibRenderState(float scale, KnightLibColor color) {
        this(scale, color == null ? KnightLibColor.WHITE_ARGB : color.toArgb());
    }

    public static KnightLibRenderState of(float scale, KnightLibColor color) {
        return new KnightLibRenderState(scale, color);
    }

    /**
     * Creates a state from the same packed ARGB format accepted by the renderer hooks
     */
    public static KnightLibRenderState of(float scale, int renderColor) {
        return new KnightLibRenderState(scale, renderColor);
    }

    /**
     * Returns a mutable color snapshot without exposing this state's stored value to mutation
     */
    public KnightLibColor color() {
        return KnightLibColor.fromArgb(renderColor);
    }

    public KnightLibRenderState withRenderColor(int renderColor) {
        return of(scale, renderColor);
    }

    public static KnightLibRenderState rgba(float scale, float red, float green, float blue, float alpha) {
        return new KnightLibRenderState(scale, KnightLibColor.rgba(red, green, blue, alpha));
    }

}
