package dev.xylonity.knightlib.client.shader.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.xylonity.knightlib.client.shader.post.internal.PostShader;
import dev.xylonity.knightlib.client.shader.post.internal.PostShaderSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Base class for post-processing shaders.
 * Provides a "safe" PostChain execution that restores framebuffer, viewport and the common GL state.
 */
public abstract class AbstractPostShader<PSS extends PostShaderSettings> implements PostShader<PSS> {

    private int lastW = -1;
    private int lastH = -1;

    @Override
    public void initOrReload(TextureManager textures, ResourceManager resources, RenderTarget target) {
        this.lastW = -1;
        this.lastH = -1;
    }

    /**
     * Runs a PostChain safely, binding the main render target before and after, restoring the viewport and the common render state
     */
    protected final void process(PostChain chain, float partialTicks, Runnable beforeProcess) {
        if (chain == null) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final RenderTarget mainRenderTarget = minecraft.getMainRenderTarget();

        // The render system may mutate the same instance later on, so a copy of the projection is made
        final Matrix4f oldProjection = RenderSystem.getProjectionMatrix();
        final Matrix4f oldProjectionCopy = (oldProjection == null) ? null : new Matrix4f(oldProjection);

        // Provides a safe identity projection for full-screen passes
        final boolean projectionWasNull = (oldProjection == null);
        if (projectionWasNull) {
            RenderSystem.setProjectionMatrix(new Matrix4f().identity(), VertexSorting.DISTANCE_TO_ORIGIN);
        }

        // Forces identity model-view for full-screen quad space and then restores afterward
        final PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        RenderSystem.applyModelViewMatrix();

        try {
            // Ensures "minecraft:main" is bound and viewport matches it
            mainRenderTarget.bindWrite(true);
            RenderSystem.viewport(0, 0, mainRenderTarget.width, mainRenderTarget.height);

            if (beforeProcess != null) {
                beforeProcess.run();
            }

            chain.process(partialTicks);

        }
        finally {
            // Restores the model-view
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();

            // Restores the projection
            if (!projectionWasNull && oldProjectionCopy != null) {
                RenderSystem.setProjectionMatrix(oldProjectionCopy, VertexSorting.DISTANCE_TO_ORIGIN);
            }

            // Rebinds main and restores the viewport for anything rendered after the postchain
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

    /**
     * Resizes the shader depending on the window size. Must be called before processing the shader
     */
    protected final void ensureSized(PostChain chain) {
        if (chain == null) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final int width = minecraft.getWindow().getWidth();
        final int height = minecraft.getWindow().getHeight();

        if (width != lastW || height != lastH) {
            chain.resize(width, height);
            lastW = width;
            lastH = height;
        }

    }

    protected final void setUniformInt(EffectInstance effectInstance, String name, int value) {
        final Uniform uniform = effectInstance.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }

    }

    protected final void setUniformFloat(EffectInstance effectInstance, String name, float value) {
        final Uniform uniform = effectInstance.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }

    }

    protected final void setUniformVec2(EffectInstance effectInstance, String name, float x, float y) {
        final Uniform uniform = effectInstance.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }

    }

    protected final void setUniformVec3(EffectInstance effectInstance, String name, Vec3 vector) {
        final Uniform uniform = effectInstance.getUniform(name);
        if (uniform != null) {
            uniform.set((float) vector.x, (float) vector.y, (float) vector.z);
        }

    }

    protected final void setUniformMat4(EffectInstance effectInstance, String name, Matrix4f matrix) {
        final Uniform uniform = effectInstance.getUniform(name);
        if (uniform != null) {
            uniform.set(matrix);
        }

    }

}