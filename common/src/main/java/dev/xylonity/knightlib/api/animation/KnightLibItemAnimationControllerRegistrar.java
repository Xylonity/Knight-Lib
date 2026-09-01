package dev.xylonity.knightlib.api.animation;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Itemstack version of {@link KnightLibAnimationControllerRegistrar}. Items are singletons, so the stack and
 * its holder have to come from the state passed to each selector instead of fields on the item.
 *
 * <p>An {@code Item} is a singleton, so selectors receive a {@link KnightLibAnimationState} describing the particular stack being evaluated.</p>
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/animation/AnimatableManager.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/animatable/SingletonGeoAnimatable.java
 */
public interface KnightLibItemAnimationControllerRegistrar {

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