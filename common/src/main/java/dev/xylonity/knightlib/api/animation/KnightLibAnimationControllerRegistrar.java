package dev.xylonity.knightlib.api.animation;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Collects the client sided controllers declared by an animatable. Registration happens once per instance and the selectors
 * are evaluated once per tick.
 *
 * Mirror's GeckoLib's predicate API implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/animation/AnimatableManager.java
 */
public interface KnightLibAnimationControllerRegistrar {

    /**
     * Adds a preconfigured controller
     */
    default void add(KnightLibAnimationController controller) {
        Objects.requireNonNull(controller, "controller");
        add(controller.name(), controller.stopTransitionTicks(), controller.resolvedSelector());
    }

    /**
     * Adds a continuously active controller. Return an animation to play it when the selection
     * changes, or {@code null} to stop this controller.
     */
    default void add(String controllerName, Supplier<KnightLibAnim> selector) {
        add(controllerName, KnightLibAnimatable.DEFAULT_TRANSITION_TICKS, selector);
    }

    /**
     * Adds a continuously active controller with an explicit transition when stopping
     */
    void add(String controllerName, int stopTransitionTicks, Supplier<KnightLibAnim> selector);

    /**
     * Adds a continuously active controller whose selector receives the evaluation state
     */
    default void add(String controllerName, Function<KnightLibAnimationState, KnightLibAnim> selector) {
        add(controllerName, KnightLibAnimatable.DEFAULT_TRANSITION_TICKS, selector);
    }

    /**
     * Adds a state(aware) active controller with an explicit transition when stopping
     */
    void add(String controllerName, int stopTransitionTicks, Function<KnightLibAnimationState, KnightLibAnim> selector);

    /**
     * Replays an animation only for client-observable entities
     */
    void addTrigger(String controllerName, BooleanSupplier trigger, KnightLibAnim animation);

    void addTrigger(String controllerName, Predicate<KnightLibAnimationState> trigger, KnightLibAnim animation);

}