package dev.xylonity.knightlib.api.animation.internal;

import dev.xylonity.knightlib.api.animation.KnightLibAnim;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;

import java.util.List;
import java.util.function.Function;

/**
 * Event history is independent of the geometry and should survive a resource change
 */
final class AnimationEventHistory {

    private static final int MAX_EVENTS = 128;

    private double previous;

    void start(double elapsed, boolean snapshot) {
        previous = snapshot ? elapsed : -1.0E-4;
    }

    void collect(String controller, List<KnightLibAnim.Step> steps, Function<String, KnightLibAnimation> resolver, double elapsed, List<AnimationNotification> output) {
        if (elapsed <= previous) {
            // Render time can move backwards for another render pass, so I'd rather never replay callbacks
            return;
        }

        final double from = elapsed - previous > 40 ? elapsed - 2 : previous;
        double stepStart = 0;
        int fired = 0;
        for (final KnightLibAnim.Step step : steps) {
            final KnightLibAnimation animation = resolver.apply(step.animation());
            if (animation == null || elapsed < stepStart) {
                break;
            }

            final double length = animation.lengthTicks();
            if (step.mode() == KnightLibAnim.PlaybackMode.LOOP) {
                if (animation.events().isEmpty()) {
                    break;
                }

                final long first = Math.max(0L, (long) Math.floor(Math.max(from - stepStart, 0.0) / length));
                final long last = Math.max(first, (long) Math.floor((elapsed - stepStart) / length));
                final long end = Math.min(last, first > Long.MAX_VALUE - MAX_EVENTS ? Long.MAX_VALUE : first + MAX_EVENTS);
                for (long cycle = first; cycle <= end && fired < MAX_EVENTS; cycle++) {
                    for (final KnightLibAnimation.KeyframeEvent event : animation.events()) {
                        final double time = stepStart + cycle * length + event.tick();
                        if (event.tick() <= length && time > from && time <= elapsed && fired < MAX_EVENTS) {
                            output.add(AnimationNotification.keyframe(controller, animation.name(), event));
                            fired++;
                        }

                    }

                    if (cycle == Long.MAX_VALUE) {
                        break;
                    }

                }

                break;
            }

            final double stepEnd = stepStart + length;
            for (final KnightLibAnimation.KeyframeEvent event : animation.events()) {
                final double time = stepStart + event.tick();
                if (time > from && time <= Math.min(elapsed, stepEnd) && fired < MAX_EVENTS) {
                    output.add(AnimationNotification.keyframe(controller, animation.name(), event));
                    fired++;
                }

            }

            if (step.mode() == KnightLibAnim.PlaybackMode.HOLD_ON_LAST_FRAME) {
                break;
            }

            if (stepEnd > from && stepEnd <= elapsed) {
                output.add(AnimationNotification.finished(controller, animation.name()));
            }

            stepStart = stepEnd;
        }

        previous = elapsed;
    }

}
