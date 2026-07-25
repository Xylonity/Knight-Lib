package dev.xylonity.knightlib.api.animation;

/**
 * Client sided notification emitted when an animation crosses an authored non-transform keyframe
 */
public record KnightLibKeyframeEvent(
        String controller,
        String animation,
        Type type,
        String payload,
        String locator
) {

    public enum Type {
        SOUND,
        PARTICLE,
        TIMELINE
    }

}
