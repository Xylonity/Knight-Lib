package dev.xylonity.knightlib.api.animation;

/**
 * Client sided notification emitted when playback crosses a sound, particle or timeline keyframe in Blockbench.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/keyframe/event/SoundKeyframeEvent.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/keyframe/event/ParticleKeyframeEvent.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/keyframe/event/CustomInstructionKeyframeEvent.java
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
