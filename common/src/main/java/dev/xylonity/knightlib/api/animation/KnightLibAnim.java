package dev.xylonity.knightlib.api.animation;

import dev.xylonity.knightlib.api.util.KnightLibEasings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable reusable animation command made of one or more ordered steps
 *
 * <pre>{@code
 * private static final KnightLibAnimation ATTACK = KnightLibAnimation.begin()
 *         .thenPlay("windup")
 *         .thenPlay("stab")
 *         .thenLoop("idle")
 *         .controller("attack")
 *         .transition(2);
 * }</pre>
 *
 * <p>{@link #thenPlay} advances to the next step when the authored animation length is reached.
 * {@link #thenLoop} and {@link #thenPlayAndHold} never advance and must therefore be the final
 * step.</p>
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/animation/RawAnimation.java
 */
public record KnightLibAnim(
        List<Step> steps,
        String controller,
        int transitionTicks,
        KnightLibEasings transitionEasing,
        float speed,
        int durationTicks,
        KnightLibAnimationBlendMode blendMode
) {

    public static final int MAX_STEPS = 32;

    public KnightLibAnim {
        Objects.requireNonNull(steps, "steps");
        if (!isValidSequence(steps)) {
            throw new IllegalArgumentException("[KnightLib] Animation sequence must contain at most " + MAX_STEPS + " steps and cannot continue after a loop or hold");
        }

        steps = List.copyOf(steps);
        Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(transitionEasing, "transitionEasing");
        Objects.requireNonNull(blendMode, "blendMode");
    }

    public KnightLibAnim(List<Step> steps, String controller, int transitionTicks, KnightLibEasings transitionEasing, float speed, int durationTicks) {
        this(steps, controller, transitionTicks, transitionEasing, speed, durationTicks, KnightLibAnimationBlendMode.AUTHORED);
    }

    /**
     * Starts an empty animation
     */
    public static KnightLibAnim begin() {
        return new KnightLibAnim(List.of(),
                KnightLibAnimationHandler.MAIN_CONTROLLER,
                KnightLibAnimatable.DEFAULT_TRANSITION_TICKS,
                KnightLibAnimatable.DEFAULT_TRANSITION_EASING,
                1f, 0
        );

    }

    /**
     * Adds a single step which advances to the following step at its authored end
     */
    public KnightLibAnim thenPlay(String animation) {
        return append(animation, PlaybackMode.ONCE);
    }

    /**
     * Adds a terminal step which repeats indefinitely
     */
    public KnightLibAnim thenLoop(String animation) {
        return append(animation, PlaybackMode.LOOP);
    }

    /**
     * Adds a terminal step which freezes on its final authored frame
     */
    public KnightLibAnim thenPlayAndHold(String animation) {
        return append(animation, PlaybackMode.HOLD_ON_LAST_FRAME);
    }

    private KnightLibAnim append(String animation, PlaybackMode mode) {
        if (!steps.isEmpty() && steps.get(steps.size() - 1).mode().terminal()) {
            throw new IllegalStateException("[KnightLib] Cannot append an animation after a loop or hold step");
        }
        if (steps.size() >= MAX_STEPS) {
            throw new IllegalStateException("[KnightLib] An animation sequence cannot contain more than " + MAX_STEPS + " steps");
        }

        final List<Step> appended = new ArrayList<>(steps.size() + 1);
        appended.addAll(steps);
        appended.add(new Step(animation, mode));

        return new KnightLibAnim(appended, controller, transitionTicks, transitionEasing, speed, durationTicks, blendMode);
    }

    public KnightLibAnim controller(String controller) {
        return new KnightLibAnim(steps, controller, transitionTicks, transitionEasing, speed, durationTicks, blendMode);
    }

    public KnightLibAnim transition(int ticks) {
        return new KnightLibAnim(steps, controller, ticks, transitionEasing, speed, durationTicks, blendMode);
    }

    public KnightLibAnim transition(int ticks, KnightLibEasings easing) {
        return new KnightLibAnim(steps, controller, ticks, easing, speed, durationTicks, blendMode);
    }

    public KnightLibAnim easing(KnightLibEasings easing) {
        return new KnightLibAnim(steps, controller, transitionTicks, easing, speed, durationTicks, blendMode);
    }

    public KnightLibAnim speed(float speed) {
        return new KnightLibAnim(steps, controller, transitionTicks, transitionEasing, speed, durationTicks, blendMode);
    }

    /**
     * Stops the animation on the server after this many animation ticks. For a sequence, the duration applies to the
     * whole animation rather than to each individual step.
     */
    public KnightLibAnim duration(int ticks) {
        return new KnightLibAnim(steps, controller, transitionTicks, transitionEasing, speed, Math.max(0, ticks), blendMode);
    }

    /**
     * Forces how this command combines with animation controllers evaluated before it
     */
    public KnightLibAnim blendMode(KnightLibAnimationBlendMode blendMode) {
        return new KnightLibAnim(steps, controller, transitionTicks, transitionEasing, speed, durationTicks, blendMode);
    }

    public KnightLibAnim overridePreviousAnimation() {
        return blendMode(KnightLibAnimationBlendMode.OVERRIDE);
    }

    public KnightLibAnim additive() {
        return blendMode(KnightLibAnimationBlendMode.ADDITIVE);
    }

    /**
     * An empty sequence is valid while it is being built but animation handlers reject it when the playback is requested.
     */
    public static boolean isValidSequence(List<Step> steps) {
        if (steps == null || steps.size() > MAX_STEPS) {
            return false;
        }

        for (int i = 0; i < steps.size(); i++) {
            final Step step = steps.get(i);
            if (step == null || (i < steps.size() - 1 && step.mode().terminal())) {
                return false;
            }

        }

        return true;
    }

    public enum PlaybackMode {

        ONCE(0, false),
        LOOP(1, true),
        HOLD_ON_LAST_FRAME(2, true);

        private final int id;
        private final boolean terminal;

        PlaybackMode(int id, boolean terminal) {
            this.id = id;
            this.terminal = terminal;
        }

        public int id() {
            return id;
        }

        public boolean terminal() {
            return terminal;
        }

        public static PlaybackMode byId(int id) {
            return switch (id) {
                case 0 -> ONCE;
                case 1 -> LOOP;
                case 2 -> HOLD_ON_LAST_FRAME;
                default -> throw new IllegalArgumentException("[KnightLib] Unknown animation playback mode: " + id);
            };

        }

    }

    public record Step(
            String animation,
            PlaybackMode mode
    ) {

        public Step {
            if (animation == null || animation.isBlank() || animation.length() > 256) {
                throw new IllegalArgumentException("[KnightLib] animation must contain 1..256 characters");
            }

            Objects.requireNonNull(mode, "mode");
        }

    }

}