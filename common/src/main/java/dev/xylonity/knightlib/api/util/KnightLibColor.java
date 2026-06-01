package dev.xylonity.knightlib.api.util;

import org.joml.Math;

/**
 * ARGB parser using bitwise shift operations
 */
public class KnightLibColor {

    public float alpha;
    public float red;
    public float green;
    public float blue;

    public KnightLibColor(float red, float green, float blue, float alpha) {
        this.alpha = alpha;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public static KnightLibColor parse(int color) {
        return new KnightLibColor(((color >> 16) & 0x00ff) / 255f, ((color >> 8) & 0x0000ff) / 255f, (color & 0x000000ff) / 255f, ((color >> 24) & 0xff) / 255f);
    }

    public int toInt() {
        int alpha = Math.clamp(Math.round(this.alpha * 255), 0, 255);
        int red = Math.clamp(Math.round(this.red * 255), 0, 255);
        int green = Math.clamp(Math.round(this.green * 255), 0, 255);
        int blue = Math.clamp(Math.round(this.blue * 255), 0, 255);

        return (alpha << 24) + (red << 16) + (green << 8) + blue;
    }

    public static int colorAlpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    public static int colorRed(int argb) {
        return (argb >> 16) & 0xFF;
    }

    public static int colorGreen(int argb) {
        return (argb >> 8) & 0xFF;
    }

    public static int colorBlue(int argb) {
        return argb & 0xFF;
    }

    /**
     * Returns the color with the alpha channel replaced
     */
    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /**
     * Returns the color with alpha multiplied by {@code factor} [0..1]
     */
    public static int multiplyAlpha(int argb, float factor) {
        final int alpha = (int) (colorAlpha(argb) * KnightLibMath.clamp01(factor));
        return withAlpha(argb, alpha);
    }

}
