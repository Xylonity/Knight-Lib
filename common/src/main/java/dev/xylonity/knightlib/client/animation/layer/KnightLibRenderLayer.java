package dev.xylonity.knightlib.client.animation.layer;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Objects;

/**
 * One optional render pass attached to a KnightLib renderer.
 */
public abstract class KnightLibRenderLayer<T> {

    /**
     * Whether this layer should run for the current frame (don't mutate the pose here)
     */
    public boolean shouldRender(KnightLibRenderLayerContext<T> context) {
        return true;
    }

    /**
     * Draws this pass using the evaluated pose exposed by {@code context}, without modifying model state.
     */
    public abstract void render(KnightLibRenderLayerContext<T> context);

    /**
     * Runs a layer in a pushed posestack frame, restored even if rendering throws, and does not snapshot model transforms
     */
    public final void renderIsolated(KnightLibRenderLayerContext<T> context) {
        Objects.requireNonNull(context, "context");
        if (!shouldRender(context)) {
            return;
        }

        final PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        try {
            render(context);
        }
        finally {
            poseStack.popPose();
        }

    }

}