package dev.xylonity.knightlib.api.animation;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Client sided animation controller registration, analogous to GeckoLib's controller predicates. Registration happens once per animatable instance
 */
public interface KnightLibAnimationControllerRegistrar {

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