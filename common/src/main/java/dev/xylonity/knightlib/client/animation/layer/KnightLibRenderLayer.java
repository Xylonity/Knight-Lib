package dev.xylonity.knightlib.client.animation.layer;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.Objects;

/**
 * One modular render pass attached to a KnightLib renderer
 */
public abstract class KnightLibRenderLayer<T> {

    /**
     * Whether this layer should run for the current frame (don't mutate the pose here)
     */
    public boolean shouldRender(KnightLibRenderLayerContext<T> context) {
        return true;
    }

    /**
     * Draws this pass using the backed state exposed by {@code context}
     */
    public abstract void render(KnightLibRenderLayerContext<T> context);

    /**
     * Internal render that runs a layer without allowing its pose-stack changes to leak into later layers
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