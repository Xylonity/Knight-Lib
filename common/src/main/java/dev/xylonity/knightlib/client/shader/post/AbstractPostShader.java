package dev.xylonity.knightlib.client.shader.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.xylonity.knightlib.client.shader.post.internal.PostShader;
import dev.xylonity.knightlib.client.shader.post.internal.PostShaderSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import org.joml.Matrix4f;

/**
 * Base class for post-processing shaders.
 * Provides a "safe" PostChain execution that restores framebuffer, viewport and the common GL state.
 */
public abstract class AbstractPostShader<PSS extends PostShaderSettings> implements PostShader<PSS> {

    /**
     * Runs a PostChain safely, binding the main render target before and after, restoring the viewport and the common render state
     */
    protected final void process(PostChain chain, float partialTicks, Runnable beforeProcess) {
        final Minecraft minecraft = Minecraft.getInstance();
        final RenderTarget mainRenderTarget = minecraft.getMainRenderTarget();

        final Matrix4f oldProjection = RenderSystem.getProjectionMatrix();
        final boolean projectionWasNull = (oldProjection == null);
        if (projectionWasNull) {
            RenderSystem.setProjectionMatrix(new Matrix4f().identity(), VertexSorting.DISTANCE_TO_ORIGIN);
        }

        final PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        RenderSystem.applyModelViewMatrix();

        try {
            // Ensures "minecraft:main" is the current main target
            mainRenderTarget.bindWrite(true);
            RenderSystem.viewport(0, 0, mainRenderTarget.width, mainRenderTarget.height);

            if (beforeProcess != null) {
                beforeProcess.run();
            }

            chain.process(partialTicks);
        }
        finally {
            // Restores the matrices
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
            if (!projectionWasNull) {
                RenderSystem.setProjectionMatrix(oldProjection, VertexSorting.DISTANCE_TO_ORIGIN);
            }

            // Restores the framebuffer/viewport for the rest of the frame
            mainRenderTarget.bindWrite(true);
            RenderSystem.viewport(0, 0, mainRenderTarget.width, mainRenderTarget.height);

            // Restores the common render state
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.resetTextureMatrix();
        }

    }

}