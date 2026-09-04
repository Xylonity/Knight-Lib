package dev.xylonity.knightlib.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import dev.xylonity.knightlib.api.animation.KnightLibKeyframeEvent;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolves deferred animation keyframes against the model's pose and dispatches them.
 */
public final class KnightLibKeyframeEvents {

    /**
     * Adds positions to events that reference a geometry locator. A bone with the same name as a locator is also accepted.
     */
    public static void dispatch(KnightLibAnimationHandler handler, KnightLibAnimator.DeferredEvents events, KnightLibModel model, PoseStack poseStack, boolean livingModelFrame) {
        if (events.isEmpty()) {
            return;
        }

        final Set<String> locators = new LinkedHashSet<>();
        final Set<String> fallbackBones = new LinkedHashSet<>();
        for (final KnightLibAnimator.Notification notification : events.notifications()) {
            if (!notification.isKeyframe()) {
                continue;
            }

            final KnightLibKeyframeEvent event = notification.keyframe();
            final String locator = event.locator();
            if (locator == null || locator.isBlank()) {
                continue;
            }

            if (model.hasLocator(locator)) {
                locators.add(locator);
            }
            else if (model.hasBone(locator)) {
                fallbackBones.add(locator);
            }

        }

        final Map<String, Vec3> positions = new HashMap<>();
        if (!locators.isEmpty()) {
            if (livingModelFrame) {
                model.visitLivingLocators(poseStack, locators, (name, pose, normal) -> positions.put(name, worldPosition(pose)));
            }
            else {
                model.visitLocators(poseStack, locators, (name, pose, normal) -> positions.put(name, worldPosition(pose)));
            }

        }
        if (!fallbackBones.isEmpty()) {
            if (livingModelFrame) {
                model.visitLivingBones(poseStack, fallbackBones, (name, pose, normal) -> positions.put(name, worldPosition(pose)));
            }
            else {
                model.visitBones(poseStack, fallbackBones, (name, pose, normal) -> positions.put(name, worldPosition(pose)));
            }

        }

        dispatch(handler, events, positions);
    }

    /**
     * Dispatches events without locator positions when a living model was evaluated but had no render pass (maybe full bone visibility disabled)
     */
    public static void dispatchUnresolved(KnightLibAnimationHandler handler, KnightLibAnimator.DeferredEvents events) {
        if (events != null && !events.isEmpty()) {
            dispatch(handler, events, Map.of());
        }

    }

    private static void dispatch(KnightLibAnimationHandler handler, KnightLibAnimator.DeferredEvents events, Map<String, Vec3> positions) {
        for (final KnightLibAnimator.Notification notification : events.notifications()) {
            try {
                if (notification.isKeyframe()) {
                    final KnightLibKeyframeEvent event = notification.keyframe();
                    handler.dispatchKeyframe(event.withLocatorPosition(positions.get(event.locator())));
                }
                else {
                    handler.dispatchFinished(notification.controller(), notification.animation());
                }

            }
            catch (Exception exception) {
                final String animation = notification.isKeyframe() ? notification.keyframe().animation() : notification.animation();
                KnightLib.LOGGER.error("Animation callback failed for '{}'", animation, exception);
            }

        }

    }

    private static Vec3 worldPosition(Matrix4f pose) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Vec3 camera = minecraft != null && minecraft.gameRenderer != null ? minecraft.gameRenderer.getMainCamera().getPosition() : Vec3.ZERO;
        return worldPosition(pose, RenderSystem.getInverseViewRotationMatrix(), camera);
    }

    static Vec3 worldPosition(Matrix4f pose, Matrix3f inverseViewRotation, Vec3 camera) {
        final Vector3f position = pose.getTranslation(new Vector3f());
        inverseViewRotation.transform(position);
        return new Vec3(position.x() + camera.x, position.y() + camera.y, position.z() + camera.z);
    }

}
