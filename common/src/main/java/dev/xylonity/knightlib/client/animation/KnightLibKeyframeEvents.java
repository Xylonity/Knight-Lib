package dev.xylonity.knightlib.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.knightlib.api.animation.internal.AnimationNotification;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import dev.xylonity.knightlib.api.animation.KnightLibKeyframeEvent;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolves deferred animation keyframes against the model's pose and dispatches them.
 */
public final class KnightLibKeyframeEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger("KnightLib");

    /**
     * Adds positions to events that reference a geometry locator. A bone with the same name as a locator is also accepted.
     */
    public static void dispatch(KnightLibAnimationHandler handler, KnightLibAnimator.DeferredEvents events, KnightLibModel model, PoseStack poseStack, boolean livingModelFrame, RenderOrigin origin) {
        if (events.isEmpty()) {
            return;
        }

        final Set<String> locators = new LinkedHashSet<>();
        final Set<String> fallbackBones = new LinkedHashSet<>();
        for (final AnimationNotification notification : events.notifications()) {
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
                model.visitLivingLocators(poseStack, locators, (name, pose, normal) -> positions.put(name, worldPosition(pose, origin)));
            }
            else {
                model.visitLocators(poseStack, locators, (name, pose, normal) -> positions.put(name, worldPosition(pose, origin)));
            }

        }
        if (!fallbackBones.isEmpty()) {
            if (livingModelFrame) {
                model.visitLivingBones(poseStack, fallbackBones, (name, pose, normal) -> positions.put(name, worldPosition(pose, origin)));
            }
            else {
                model.visitBones(poseStack, fallbackBones, (name, pose, normal) -> positions.put(name, worldPosition(pose, origin)));
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
        for (final AnimationNotification notification : events.notifications()) {
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
                LOGGER.error("Animation callback failed for '{}'", animation, exception);
            }

        }

    }

    private static Vec3 worldPosition(Matrix4f pose, RenderOrigin origin) {
        if (origin != null) {
            return origin.worldPosition(pose);
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final Vec3 camera = minecraft != null && minecraft.gameRenderer != null ? minecraft.gameRenderer.getMainCamera().getPosition() : Vec3.ZERO;
        return worldPosition(pose, camera);
    }

    static Vec3 worldPosition(Matrix4f pose, Vec3 camera) {
        final Vector3f position = pose.getTranslation(new Vector3f());
        return new Vec3(position.x() + camera.x, position.y() + camera.y, position.z() + camera.z);
    }

    /**
     * World position the renderer's entry pose maps to + that pose's inverse
     */
    public record RenderOrigin(
            Vec3 position,
            Matrix4f inverseEntryPose
    ) {

        public static RenderOrigin capture(Vec3 position, PoseStack poseStack) {
            return new RenderOrigin(position, new Matrix4f(poseStack.last().pose()).invert());
        }

        /**
         * Matches the translation the entity render dispatcher applies before calling the renderer
         */
        public static RenderOrigin ofEntity(Entity entity, float partialTicks, Vec3 renderOffset, PoseStack poseStack) {
            final double x = Mth.lerp(partialTicks, entity.xOld, entity.getX()) + renderOffset.x;
            final double y = Mth.lerp(partialTicks, entity.yOld, entity.getY()) + renderOffset.y;
            final double z = Mth.lerp(partialTicks, entity.zOld, entity.getZ()) + renderOffset.z;
            return capture(new Vec3(x, y, z), poseStack);
        }

        /**
         * Matches the translation the blockentity render dispatcher applies before calling the renderer
         */
        public static RenderOrigin ofBlock(BlockPos position, PoseStack poseStack) {
            return capture(Vec3.atLowerCornerOf(position), poseStack);
        }

        Vec3 worldPosition(Matrix4f pose) {
            final Vector3f local = inverseEntryPose.mul(pose, new Matrix4f()).getTranslation(new Vector3f());
            if (!Float.isFinite(local.x()) || !Float.isFinite(local.y()) || !Float.isFinite(local.z())) {
                return KnightLibKeyframeEvents.worldPosition(pose, (RenderOrigin) null);
            }

            return new Vec3(position.x + local.x(), position.y + local.y(), position.z + local.z());
        }

    }

}
