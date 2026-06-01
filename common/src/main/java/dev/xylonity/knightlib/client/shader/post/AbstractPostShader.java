package dev.xylonity.knightlib.client.shader.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Base class for post-processing shaders.
 * Provides a "safe" PostChain execution that restores framebuffer, viewport
 * and the common GL state, as well as lifecycle management for GPU resources.
 */
public abstract class AbstractPostShader<PSS extends PostShaderSettings> implements PostShader<PSS> {

    private final Map<PostChain, Long> chainSizeCache = new WeakHashMap<>();

    @Override
    public void initOrReload(TextureManager textures, ResourceManager resources, RenderTarget target) {
        chainSizeCache.clear();
    }

    /**
     * Releases GPU resources held by the given chain.
     * Subclasses that hold a {@link PostChain} field should override {@link #dispose()}
     * and call {@code closeChain(myChain)} there.
     */
    protected final void closeChain(PostChain chain) {
        if (chain != null) {
            chain.close();
            chainSizeCache.remove(chain);
        }

    }

    /**
     * Called when this shader is unregistered or replaced.
     * Subclasses must override this to close their PostChain(s).
     */
    @Override
    public void dispose() {
        chainSizeCache.clear();
    }

    /**
     * Runs a PostChain safely, binding the main render target before and after,
     * restoring the viewport and the common render state.
     */
    protected final void process(PostChain chain, float partialTicks, Runnable beforeProcess) {
        if (chain == null) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final RenderTarget mainRenderTarget = minecraft.getMainRenderTarget();

        // Save projection (copy, as the RenderSystem may mutate the instance later)
        final Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());

        // Identity model-view for full-screen quad space
        final PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        RenderSystem.applyModelViewMatrix();

        try {
            mainRenderTarget.bindWrite(true);
            RenderSystem.viewport(0, 0, mainRenderTarget.width, mainRenderTarget.height);

            if (beforeProcess != null) {
                beforeProcess.run();
            }

            chain.process(partialTicks);
        }
        finally {
            // Restores model-view
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();

            // Restores projection
            RenderSystem.setProjectionMatrix(savedProjection, VertexSorting.DISTANCE_TO_ORIGIN);

            // Rebinds main and restore viewport
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
        ensureSizedChanged(chain);
    }

    /**
     * Same as ensureSized, but returns whether a resize actually happened
     */
    protected final boolean ensureSizedChanged(PostChain chain) {
        if (chain == null) {
            return false;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final int width = minecraft.getWindow().getWidth();
        final int height = minecraft.getWindow().getHeight();

        final long current = ((long) width << 32) | (height & 0xFFFFFFFFL);
        final Long previous = chainSizeCache.get(chain);

        if (previous == null || previous != current) {
            chain.resize(width, height);
            chainSizeCache.put(chain, current);
            return true;
        }

        return false;
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