package dev.xylonity.knightlib.api.animation;

/**
 * Determines how an animation layer combines with controllers evaluated before it
 */
public enum KnightLibAnimationBlendMode {

    /// Uses the animation JSON's {@code override_previous_animation} value
    AUTHORED(0),
    /// Always adds the animation's transform delta to the existing pose
    ADDITIVE(1),
    /// Always replaces the existing pose for every bone touched by the animation
    OVERRIDE(2);

    private final int id;

    KnightLibAnimationBlendMode(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static KnightLibAnimationBlendMode byId(int id) {
        return switch (id) {
            case 0 -> AUTHORED;
            case 1 -> ADDITIVE;
            case 2 -> OVERRIDE;
            default -> throw new IllegalArgumentException("[KnightLib] Unknown animation blend mode " + id);
        };

    }

}
