package dev.xylonity.knightlib.api.camera.path.impl;

import net.minecraft.util.Mth;

/**
 * Intensity shape of an {@link EffectKeyframe} window over its tick range
 */
public enum EffectMode {
    IN(0),
    OUT(1),
    FULL(2),
    HOLD(3);

    private final int id;

    EffectMode(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    /**
     * Intensity at a normalized progress through the window
     */
    public float intensityAt(float progress) {
        final float clampedProgress = Mth.clamp(progress, 0f, 1f);
        return switch (this) {
            case IN -> 0.5f + 0.5f * Mth.cos((float) Math.PI * clampedProgress);
            case OUT -> 0.5f - 0.5f * Mth.cos((float) Math.PI * clampedProgress);
            case FULL -> Mth.sin((float) Math.PI * clampedProgress);
            case HOLD -> 1f;
        };

    }

    public static EffectMode byId(int id) {
        for (final EffectMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }

        }

        return FULL;
    }

}
