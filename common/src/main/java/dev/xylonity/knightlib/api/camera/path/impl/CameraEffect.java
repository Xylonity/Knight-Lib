package dev.xylonity.knightlib.api.camera.path.impl;

/**
 * Screen effect that can be keyframed along a CameraPath timeline through {@link EffectKeyframe} windows
 */
public enum CameraEffect {

    /**
     * Black overlay
     */
    FADE(0),
    /**
     * Blur
     */
    BLUR(1),
    /**
     * Horizontal top and bottom black bars
     */
    LETTERBOX(2);

    private final int id;

    CameraEffect(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static CameraEffect byId(int id) {
        for (final CameraEffect effect : values()) {
            if (effect.id == id) {
                return effect;
            }

        }

        return FADE;
    }

}