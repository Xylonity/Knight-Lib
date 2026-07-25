package dev.xylonity.knightlib.api.util;

import org.joml.Math;

/**
 * Mutable RGBA color parser with additional color utilities
 */
public class KnightLibColor {

    public static final int WHITE_ARGB = 0xffffffff;

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

    /**
     * Creates an opaque render color
     */
    public static KnightLibColor rgb(float red, float green, float blue) {
        return rgba(red, green, blue, 1f);
    }

    /**
     * Creates a render color with a certain alpha
     */
    public static KnightLibColor rgba(float red, float green, float blue, float alpha) {
        return new KnightLibColor(normalize(red), normalize(green), normalize(blue), normalize(alpha));
    }

    public static KnightLibColor fromArgb(int argb) {
        return parse(argb);
    }

    public float red() {
        return red;
    }

    public float green() {
        return green;
    }

    public float blue() {
        return blue;
    }

    public float alpha() {
        return alpha;
    }

    /**
     * Returns a mutable snapshot
     */
    public KnightLibColor normalized() {
        return rgba(red, green, blue, alpha);
    }

    /**
     * Returns a copy with its alpha replaced
     */
    public KnightLibColor withAlpha(float alpha) {
        return rgba(red, green, blue, alpha);
    }

    /**
     * Multiplies two RGBA tints
     */
    public KnightLibColor multiply(KnightLibColor other) {
        if (other == null) {
            return normalized();
        }
        return rgba(red * other.red, green * other.green, blue * other.blue, alpha * other.alpha);
    }

    /**
     * Multiplies two packed ARGB colors
     */
    public static int multiplyArgb(int baseArgb, int tintArgb) {
        return fromArgb(baseArgb).multiply(fromArgb(tintArgb)).toArgb();
    }

    public boolean isTranslucent() {
        return normalize(alpha) < 1f;
    }

    public int toInt() {
        final int alpha = Math.clamp(Math.round(this.alpha * 255), 0, 255);
        final int red = Math.clamp(Math.round(this.red * 255), 0, 255);
        final int green = Math.clamp(Math.round(this.green * 255), 0, 255);
        final int blue = Math.clamp(Math.round(this.blue * 255), 0, 255);

        return (alpha << 24) + (red << 16) + (green << 8) + blue;
    }

    /**
     * Packs a render color as ARGB
     */
    public int toArgb() {
        final int alpha = renderChannel(this.alpha);
        final int red = renderChannel(this.red);
        final int green = renderChannel(this.green);
        final int blue = renderChannel(this.blue);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
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
     * Returns the color with alpha multiplied by the given factor
     */
    public static int multiplyAlpha(int argb, float factor) {
        final int alpha = (int) (colorAlpha(argb) * KnightLibMath.clamp01(factor));
        return withAlpha(argb, alpha);
    }

    private static int renderChannel(float channel) {
        return java.lang.Math.round(normalize(channel) * 255f);
    }

    private static float normalize(float channel) {
        if (!Float.isFinite(channel)) {
            return 1f;
        }

        return java.lang.Math.max(0f, java.lang.Math.min(1f, channel));
    }

}
