package dev.xylonity.knightlib.api.animation;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Trivial client sided animation controller registration for item stacks.
 *
 * <p>An {@code Item} is a singleton, so selectors receive a {@link KnightLibAnimationState} describing the particular stack being evaluated.</p>
 */
public interface KnightLibItemAnimationControllerRegistrar {

    /**
     * Adds a continuously active controller. Return an animation to play it when the selection
     * changes, or {@code null} to stop this controller.
     */
    default void add(String controllerName, Function<KnightLibAnimationState, KnightLibAnim> selector) {
        add(controllerName, KnightLibAnimatable.DEFAULT_TRANSITION_TICKS, selector);
    }

    /**
     * Adds a continuously active controller with an explicit transition when stopping.
     */
    void add(String controllerName, int stopTransitionTicks, Function<KnightLibAnimationState, KnightLibAnim> selector);

    /**
     * Replays an animation only for client-observable entities
     */
    void addTrigger(String controllerName, Predicate<KnightLibAnimationState> trigger, KnightLibAnim animation);

}