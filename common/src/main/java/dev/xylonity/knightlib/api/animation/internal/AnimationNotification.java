package dev.xylonity.knightlib.api.animation.internal;

import dev.xylonity.knightlib.api.animation.KnightLibKeyframeEvent;

/**
 * A callback queued after evaluation, so user code cannot mutate an active iteration
 */
public record AnimationNotification(
        KnightLibKeyframeEvent keyframe,
        String controller,
        String animation
) {

    static AnimationNotification keyframe(String controller, String animation, dev.xylonity.knightlib.api.client.animation.KnightLibAnimation.KeyframeEvent event) {
        return new AnimationNotification(new KnightLibKeyframeEvent(controller, animation, event.type(), event.payload(), event.locator()), controller, animation);
    }

    static AnimationNotification finished(String controller, String animation) {
        return new AnimationNotification(null, controller, animation);
    }

    public boolean isKeyframe() {
        return keyframe != null;
    }

}