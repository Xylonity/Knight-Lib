package dev.xylonity.knightlib.api.animation.internal;

import dev.xylonity.knightlib.api.animation.KnightLibAnim;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The boring but important thing that decides which step of an animation command is active right now
 */
public final class KnightLibAnimationSequence {

    /**
     * Resolves an animation-relative elapsed tick to its current step and sample position
     */
    public static Playback sample(List<KnightLibAnim.Step> steps, Function<String, KnightLibAnimation> resolver, double elapsedTicks) {
        Objects.requireNonNull(steps, "steps");
        Objects.requireNonNull(resolver, "resolver");
        if (steps.isEmpty()) {
            return Playback.empty();
        }

        double elapsed = Math.max(0.0, elapsedTicks);
        double stepStart = 0.0;
        for (int index = 0; index < steps.size(); index++) {
            final KnightLibAnim.Step step = steps.get(index);
            final KnightLibAnimation animation = resolver.apply(step.animation());
            final double rawTick = Math.max(0.0, elapsed - stepStart);
            if (animation == null) {
                return new Playback(index, step, null, stepStart, rawTick, 0f, Double.POSITIVE_INFINITY, false);
            }

            final float length = animation.lengthTicks();
            switch (step.mode()) {
                case ONCE -> {
                    final double stepEnd = stepStart + length;
                    if (elapsed < stepEnd) {
                        return new Playback(index, step, animation, stepStart, rawTick, (float) rawTick, stepEnd, false);
                    }
                    if (index == steps.size() - 1) {
                        return new Playback(index, step, animation, stepStart, length, length, stepEnd, true);
                    }

                    stepStart = stepEnd;
                }
                case LOOP -> {
                    // Loops the rest of the sequence by contract so it doesn't really advance
                    final float tick = (float) (rawTick % length);
                    return new Playback(index, step, animation, stepStart, rawTick, tick, Double.POSITIVE_INFINITY, false);
                }
                case HOLD_ON_LAST_FRAME -> {
                    // Keeps the raw tick growing for molang while clamping only the transform sample
                    final float tick = (float) Math.min(rawTick, length);
                    return new Playback(index, step, animation, stepStart, rawTick, tick, Double.POSITIVE_INFINITY, false);
                }

            }

        }

        throw new IllegalStateException("[KnightLib] Animation sequence did not resolve a playback step");
    }

    public record Playback(
            int stepIndex,
            KnightLibAnim.Step step,
            KnightLibAnimation animation,
            double stepStartTick,
            double rawTick,
            float sampleTick,
            double endTick,
            boolean finished
    ) {

        private static Playback empty() {
            return new Playback(-1, null, null, 0.0, 0.0, 0f, 0.0, true);
        }

    }

}
