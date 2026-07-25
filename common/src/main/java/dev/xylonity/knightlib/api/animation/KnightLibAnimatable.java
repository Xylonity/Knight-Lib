package dev.xylonity.knightlib.api.animation;

import dev.xylonity.knightlib.api.util.KnightLibEasings;

/**
 * Implemented by entities and block entities animated through KnightLib.
 *
 * <p>The implementor holds a single {@link KnightLibAnimationHandler} instance for its whole lifetime and returns it from {@link #getAnimationHandler()}:</p>
 *
 * <pre>{@code
 * private final KnightLibAnimationHandler animations = KnightLibAnimationHandler.of(this);
 *
 * @Override
 * public KnightLibAnimationHandler getAnimationHandler() {
 *     return animations;
 * }
 * }</pre>
 *
 * <p>All default methods can be called from either logical side. When called on the server, the animation is synced automatically
 * to every client tracking this entity or block entity. When called on the client, the animation is client local.</p>
 *
 * <p>Block entities that override {@code getUpdateTag()} must call {@code super.getUpdateTag()} or explicitly save this handler
 * into their returned tag. An override that bypasses the vanilla method also bypasses KnightLib's automatic update-tag injection.</p>
 */
public interface KnightLibAnimatable {

    int DEFAULT_TRANSITION_TICKS = 1;
    KnightLibEasings DEFAULT_TRANSITION_EASING = KnightLibEasings.EASE_IN_OUT_QUAD;

    KnightLibAnimationHandler getAnimationHandler();

    /**
     * Declares animation selection rules evaluated locally once per client tick
     */
    default void registerAnimationControllers(KnightLibAnimationControllerRegistrar controllers) {
        ;;
    }

    /**
     * Plays a pre-declared animation command, see {@link KnightLibAnim}
     */
    default void playAnimation(KnightLibAnim anim) {
        getAnimationHandler().play(anim);
    }

    /**
     * Plays an animation once on the default controller
     */
    default void playAnimation(String animation) {
        playAnimation(KnightLibAnimationHandler.MAIN_CONTROLLER, animation, DEFAULT_TRANSITION_TICKS, DEFAULT_TRANSITION_EASING, 1f);
    }

    default void playAnimation(String animation, int transitionTicks) {
        playAnimation(KnightLibAnimationHandler.MAIN_CONTROLLER, animation, transitionTicks, DEFAULT_TRANSITION_EASING, 1f);
    }

    default void playAnimation(String controller, String animation, int transitionTicks, KnightLibEasings transitionEasing) {
        playAnimation(controller, animation, transitionTicks, transitionEasing, 1f);
    }

    default void playAnimation(String controller, String animation, int transitionTicks, KnightLibEasings transitionEasing, float speed) {
        getAnimationHandler().play(controller, animation, transitionTicks, transitionEasing, speed);
    }

    default void stopAnimation() {
        stopAnimation(KnightLibAnimationHandler.MAIN_CONTROLLER, DEFAULT_TRANSITION_TICKS);
    }

    /**
     * Stops whatever the controller is playing, blending back to the rest pose over the given {@code transitionTicks}.
     */
    default void stopAnimation(String controller, int transitionTicks) {
        getAnimationHandler().stop(controller, transitionTicks);
    }

    /**
     * Name of the first step in the last sequence animated on the controller, or null if stopped.
     * Note that on the server an unbounded command does not expire when a sequence ends (the server does not load trivial
     * client assets), so use {@link KnightLibAnim#duration(int)} for authoritative finite animations.
     */
    default String getActiveAnimation(String controller) {
        return getAnimationHandler().getActiveAnimation(controller);
    }

    default boolean isAnimationActive(String animation) {
        return getAnimationHandler().isAnimationActive(animation);
    }

    /**
     * Internally updates this target's controllers. KnightLib calls this automatically once per logical tick
     * for both entities and block entities, and it remains public for custom schedulers.
     */
    default void tickAnimations() {
        getAnimationHandler().tick();
    }

    /**
     * Called on the client when an authored sound, particle or timeline keyframe is crossed.
     */
    default void onAnimationKeyframe(KnightLibKeyframeEvent event) {
        ;;
    }

    /**
     * Called on the client when a {@link KnightLibAnim#thenPlay} step ends.
     */
    default void onAnimationFinished(String controller, String animation) {
        ;;
    }

}