package dev.xylonity.knightlib.api.animation;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Client sided notification emitted when playback crosses a sound, particle or timeline keyframe in Blockbench.
 * {@link #locatorPosition()} is the locator's position when one could be resolved during a render pass, or {@code null} otherwise.
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
        String locator,
        @Nullable Vec3 locatorPosition
) {

    public KnightLibKeyframeEvent(String controller, String animation, Type type, String payload, String locator) {
        this(controller, animation, type, payload, locator, null);
    }

    public KnightLibKeyframeEvent withLocatorPosition(@Nullable Vec3 position) {
        return new KnightLibKeyframeEvent(controller, animation, type, payload, locator, position);
    }

    public enum Type {
        SOUND,
        PARTICLE,
        TIMELINE
    }

}