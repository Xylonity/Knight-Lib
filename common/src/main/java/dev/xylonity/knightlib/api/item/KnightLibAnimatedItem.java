package dev.xylonity.knightlib.api.item;

import dev.xylonity.knightlib.api.animation.KnightLibAnim;
import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import dev.xylonity.knightlib.api.animation.KnightLibItemAnimationControllerRegistrar;
import dev.xylonity.knightlib.api.animation.KnightLibItemAnimations;
import dev.xylonity.knightlib.api.animation.KnightLibKeyframeEvent;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Common per-stack animation API shared by ordinary custom-rendered items and equipped armor. It mirrors the entity API, but every call includes the stack.
 *
 * <p>An {@code Item} is a singleton, so every method receives the particular stack whose
 * controllers are being queried or changed.</p>
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/animatable/GeoItem.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/animatable/SingletonGeoAnimatable.java
 */
public interface KnightLibAnimatedItem {

    /**
     * Declares animation selection rules evaluated locally once per client tick
     */
    default void registerAnimationControllers(KnightLibItemAnimationControllerRegistrar controllers) {
        ;;
    }

    /**
     * Returns this stack's animation handler
     */
    default KnightLibAnimationHandler getAnimationHandler(ItemStack stack, @Nullable Level level) {
        return KnightLibItemAnimations.getAnimationHandler(this, stack, level);
    }

    /**
     * Plays a pre-declared animation command on this stack
     */
    default void playAnimation(ItemStack stack, @Nullable Level level, KnightLibAnim anim) {
        getAnimationHandler(stack, level).play(anim);
    }

    /**
     * Plays an animation once on the main controller with the default transition
     */
    default void playAnimation(ItemStack stack, @Nullable Level level, String animation) {
        playAnimation(stack, level, KnightLibAnimationHandler.MAIN_CONTROLLER, animation,
                KnightLibAnimatable.DEFAULT_TRANSITION_TICKS,
                KnightLibAnimatable.DEFAULT_TRANSITION_EASING, 1f);
    }

    default void playAnimation(ItemStack stack, @Nullable Level level, String animation, int transitionTicks) {
        playAnimation(stack, level, KnightLibAnimationHandler.MAIN_CONTROLLER, animation, transitionTicks, KnightLibAnimatable.DEFAULT_TRANSITION_EASING, 1f);
    }

    default void playAnimation(ItemStack stack, @Nullable Level level, String controller, String animation, int transitionTicks, KnightLibEasings transitionEasing) {
        playAnimation(stack, level, controller, animation, transitionTicks, transitionEasing, 1f);
    }

    /**
     * Plays an animation on the named controller for this particular stack
     */
    default void playAnimation(ItemStack stack, @Nullable Level level, String controller, String animation, int transitionTicks, KnightLibEasings transitionEasing, float speed) {
        getAnimationHandler(stack, level).play(controller, animation, transitionTicks, transitionEasing, speed);
    }

    default void stopAnimation(ItemStack stack, @Nullable Level level) {
        stopAnimation(stack, level, KnightLibAnimationHandler.MAIN_CONTROLLER, KnightLibAnimatable.DEFAULT_TRANSITION_TICKS);
    }

    /**
     * Stops a named controller and blends it back to the rest pose
     */
    default void stopAnimation(ItemStack stack, @Nullable Level level, String controller, int transitionTicks) {
        getAnimationHandler(stack, level).stop(controller, transitionTicks);
    }

    default @Nullable String getActiveAnimation(ItemStack stack, @Nullable Level level, String controller) {
        return getAnimationHandler(stack, level).getActiveAnimation(controller);
    }

    default boolean isAnimationActive(ItemStack stack, @Nullable Level level, String animation) {
        return getAnimationHandler(stack, level).isAnimationActive(animation);
    }

    /**
     * Internal for animation ticking
     */
    default void tickAnimations(ItemStack stack, @Nullable Level level) {
        getAnimationHandler(stack, level).tick();
    }

    /**
     * Called on the client when a keyframe is crossed
     */
    default void onAnimationKeyframe(ItemStack stack, KnightLibKeyframeEvent event) {
        ;;
    }

    /**
     * Called on the client when an animation step ends
     */
    default void onAnimationFinished(ItemStack stack, String controller, String animation) {
        ;;
    }

}