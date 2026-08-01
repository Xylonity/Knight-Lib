package dev.xylonity.knightlib.api.animation;

import dev.xylonity.knightlib.api.util.KnightLibEasings;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * A configurable animation controller, based on GeckoLib's {@code AnimationController}.
 *
 * <pre>{@code
 * controllers.add(KnightLibAnimationController.of("movement")
 *         .locomotion(IDLE, WALK)
 *         .movementSpeed(BLOCKS_PER_CYCLE, CYCLE_SECONDS)
 *         .transition(5, KnightLibEasings.EASE_IN_OUT_QUAD));
 * }</pre>
 */
public final class KnightLibAnimationController {

    public static final float MIN_SPEED = 0.001f;
    public static final float MAX_SPEED = 100f;
    public static final float DEFAULT_MIN_MOVEMENT_SPEED = 0.25f;
    public static final float DEFAULT_MAX_MOVEMENT_SPEED = 3f;

    private final String name;

    private int transitionTicks = -1;
    private KnightLibEasings transitionEasing;
    private int stopTransitionTicks = KnightLibAnimatable.DEFAULT_TRANSITION_TICKS;
    private Function<KnightLibAnimationState, KnightLibAnim> selector = state -> null;
    private ToDoubleFunction<KnightLibAnimationState> speed;
    private ToDoubleFunction<KnightLibAnimationState> speedMultiplier;

    private KnightLibAnimationController(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public static KnightLibAnimationController of(String name) {
        return new KnightLibAnimationController(name);
    }

    /**
     * Transition used whenever this controller changes between two animations. When this is not configured,
     * each selected animation keeps its own transition
     */
    public KnightLibAnimationController transitionTicks(int ticks) {
        return transition(ticks);
    }

    public KnightLibAnimationController transition(int ticks) {
        validateTransitionTicks(ticks);
        this.transitionTicks = ticks;
        this.transitionEasing = null;
        return this;
    }

    public KnightLibAnimationController transition(int ticks, KnightLibEasings easing) {
        validateTransitionTicks(ticks);
        this.transitionTicks = ticks;
        this.transitionEasing = Objects.requireNonNull(easing, "easing");
        return this;
    }

    /**
     * Transition used when the selector declines to pick an animation and the controller stops
     */
    public KnightLibAnimationController stopTransitionTicks(int ticks) {
        this.stopTransitionTicks = ticks;
        return this;
    }

    public KnightLibAnimationController selects(Function<KnightLibAnimationState, KnightLibAnim> selector) {
        this.selector = Objects.requireNonNull(selector, "selector");
        return this;
    }

    public KnightLibAnimationController selects(Supplier<KnightLibAnim> selector) {
        Objects.requireNonNull(selector, "selector");
        return selects(state -> selector.get());
    }

    /**
     * Helper that selects {@code movement} while the target is moving and {@code idle} otherwise
     */
    public KnightLibAnimationController locomotion(KnightLibAnim idle, KnightLibAnim movement) {
        return locomotion(idle, movement, KnightLibAnimationState.DEFAULT_MOVEMENT_THRESHOLD);
    }

    public KnightLibAnimationController locomotion(KnightLibAnim idle, KnightLibAnim movement, double minimumBlocksPerTick) {
        Objects.requireNonNull(idle, "idle");
        Objects.requireNonNull(movement, "movement");
        validateNonNegativeFinite(minimumBlocksPerTick, "minimumBlocksPerTick");
        return selects(state -> state.isMoving(minimumBlocksPerTick) ? movement : idle);
    }

    /**
     * Selects {@code animation} while {@code condition} is true and stops this controller otherwise
     */
    public KnightLibAnimationController when(Predicate<KnightLibAnimationState> condition, KnightLibAnim animation) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(animation, "animation");
        return selects(state -> condition.test(state) ? animation : null);
    }

    /**
     * Fixed absolute playback speed, where 1 is the main rate
     */
    public KnightLibAnimationController speed(double speed) {
        return speed(state -> speed);
    }

    public KnightLibAnimationController speed(ToDoubleFunction<KnightLibAnimationState> speed) {
        this.speed = Objects.requireNonNull(speed, "speed");
        return this;
    }

    /**
     * Fixed multiplier applied on top of the selected animation's speed
     */
    public KnightLibAnimationController speedMultiplier(double multiplier) {
        return speedMultiplier(state -> multiplier);
    }

    public KnightLibAnimationController speedMultiplier(ToDoubleFunction<KnightLibAnimationState> multiplier) {
        this.speedMultiplier = Objects.requireNonNull(multiplier, "multiplier");
        return this;
    }

    /**
     * Scales a walk cycle to the target's actual horizontal movement (for example 5 blocksPC per 2.5 blocks per second
     * resolves to 0.5)
     */
    public KnightLibAnimationController movementSpeed(double blocksPerCycle, double cycleSeconds) {
        return movementSpeed(blocksPerCycle, cycleSeconds, DEFAULT_MIN_MOVEMENT_SPEED, DEFAULT_MAX_MOVEMENT_SPEED);
    }

    public KnightLibAnimationController movementSpeed(double blocksPerCycle, double cycleSeconds, double minimumSpeed, double maximumSpeed) {
        validatePositiveFinite(blocksPerCycle, "blocksPerCycle");
        validatePositiveFinite(cycleSeconds, "cycleSeconds");
        validatePositiveFinite(minimumSpeed, "minimumSpeed");
        validatePositiveFinite(maximumSpeed, "maximumSpeed");
        if (minimumSpeed > maximumSpeed) {
            throw new IllegalArgumentException("minimumSpeed cannot exceed maximumSpeed");
        }

        return speed(state -> {
            if (!state.isMoving()) {
                return 1.0D;
            }

            final double movementSpeed = state.blocksPerSecond() * cycleSeconds / blocksPerCycle;
            return Math.min(maximumSpeed, Math.max(minimumSpeed, movementSpeed));
        });

    }

    public String name() {
        return name;
    }

    public int stopTransitionTicks() {
        return stopTransitionTicks;
    }

    /**
     * The selector with controller-level transition and speed handlers already composed in
     */
    public Function<KnightLibAnimationState, KnightLibAnim> resolvedSelector() {
        final Function<KnightLibAnimationState, KnightLibAnim> picked = selector;
        final int resolvedTransitionTicks = transitionTicks;
        final KnightLibEasings resolvedTransitionEasing = transitionEasing;
        final ToDoubleFunction<KnightLibAnimationState> resolvedSpeed = speed;
        final ToDoubleFunction<KnightLibAnimationState> resolvedSpeedMultiplier = speedMultiplier;
        if (resolvedTransitionTicks < 0 && resolvedSpeed == null && resolvedSpeedMultiplier == null) {
            return picked;
        }

        return state -> {
            KnightLibAnim animation = picked.apply(state);
            if (animation == null) {
                return null;
            }
            if (resolvedTransitionTicks >= 0) {
                animation = resolvedTransitionEasing == null
                        ? animation.transition(resolvedTransitionTicks)
                        : animation.transition(resolvedTransitionTicks, resolvedTransitionEasing);
            }

            if (resolvedSpeed != null || resolvedSpeedMultiplier != null) {
                double playbackSpeed = animation.speed();
                if (resolvedSpeed != null) {
                    playbackSpeed = finiteOrDefault(resolvedSpeed.applyAsDouble(state), 1.0D);
                }
                if (resolvedSpeedMultiplier != null) {
                    playbackSpeed *= finiteOrDefault(resolvedSpeedMultiplier.applyAsDouble(state), 1.0D);
                }

                animation = animation.speed(clamp(playbackSpeed));
            }

            return animation;
        };

    }

    private static void validateTransitionTicks(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("[KnightLib] transitionTicks must be non-negative");
        }

    }

    private static void validateNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException("[KnightLib] " + name + " must be finite and non-negative");
        }

    }

    private static void validatePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException("[KnightLib] " + name + " must be finite and positive");
        }

    }

    private static double finiteOrDefault(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static float clamp(double speed) {
        if (!Double.isFinite(speed)) {
            return 1f;
        }

        return (float) Math.min(MAX_SPEED, Math.max(MIN_SPEED, speed));
    }

}