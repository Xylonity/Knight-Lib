package dev.xylonity.knightlib.client.animation;

import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import dev.xylonity.knightlib.api.animation.internal.AnimationNotification;
import dev.xylonity.knightlib.api.animation.internal.KnightLibAnimationEvaluator;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationPlayback;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.client.animation.molang.MolangContext;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.function.Function;

/**
 * Client pose evaluator.
 */
public final class KnightLibAnimator {

    /**
     * Evaluates the complete pose before dispatching callbacks, allowing callbacks to play/stop controllers safely
     */
    public static void animate(KnightLibAnimationHandler handler, KnightLibModel model, Function<String, KnightLibAnimation> resolver, double now) {
        KnightLibKeyframeEvents.dispatchUnresolved(handler, animateDeferred(handler, model, resolver, now));
    }

    public static DeferredEvents animateDeferred(KnightLibAnimationHandler handler, KnightLibModel model, Function<String, KnightLibAnimation> resolver, double now) {
        final KnightLibAnimationEvaluator evaluator = handler.clientEvaluator();
        evaluator.resourceGeneration(KnightLibAnimationAssets.generation());
        evaluator.bindSkeleton(model.boneNames());
        model.applyAnimationPose(evaluator.evaluate(handler.controllers(), resolver, now, clientMolang(handler.animationEntity(), now), true));
        return evaluator.notifications().isEmpty() ? DeferredEvents.EMPTY : new DeferredEvents(evaluator.notifications());
    }

    /**
     * Last evaluated step
     */
    public static KnightLibAnimationPlayback playback(KnightLibAnimationHandler handler, String controller) {
        return handler.getPlayback(controller);
    }

    public static void applyAmbient(KnightLibModel model, KnightLibAnimation animation, double now, float speed) {
        applySample(model, animation, now, speed, true);
    }

    public static void applyAt(KnightLibModel model, KnightLibAnimation animation, double elapsedTicks, float speed) {
        applySample(model, animation, elapsedTicks, speed, false);
    }

    private static void applySample(KnightLibModel model, KnightLibAnimation animation, double now, float speed, boolean loop) {
        final KnightLibAnimationEvaluator evaluator = model.ambientEvaluator();
        evaluator.resourceGeneration(KnightLibAnimationAssets.generation());
        evaluator.bindSkeleton(model.boneNames());
        model.applyAnimationPose(evaluator.sample(animation, now, speed, clientMolang(null, now), loop));
    }

    private static MolangContext clientMolang(Entity entity, double now) {
        final MolangContext context = new MolangContext();
        context.setEntity(entity);
        context.setNow(now);

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            if (minecraft.level != null) {
                context.setLevel(minecraft.level);
                context.setActorCount(minecraft.level.getEntityCount());
            }

            if (entity != null && minecraft.gameRenderer != null) {
                context.setDistanceFromCamera((float) minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(entity.position()));
            }

        }

        return context;
    }

    public static void clearWarnings() {
        KnightLibAnimationEvaluator.clearWarnings();
    }

    public static final class DeferredEvents {

        private static final DeferredEvents EMPTY = new DeferredEvents(List.of());
        private final List<AnimationNotification> notifications;

        private DeferredEvents(List<AnimationNotification> notifications) {
            this.notifications = List.copyOf(notifications);
        }

        public boolean isEmpty() {
            return notifications.isEmpty();
        }

        List<AnimationNotification> notifications() {
            return notifications;
        }

    }

}
