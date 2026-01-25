package dev.xylonity.knightlib.client.shader.post.internal;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.EnumSet;

/**
 * Generic contract for a client-sided post-processing shader effect
 *
 * A post shader typically runs as {@link net.minecraft.client.renderer.PostChain} pass (it processes the final scene
 * buffers) and is driven by a manager that ticks and renders all registered shaders
 *
 * @param <PSS> is the settings payload used when starting/spawning an instance of the shader
 */
public interface PostShader<PSS extends PostShaderSettings> {

    ResourceLocation id();

    /**
     * Starts (or enqueues) a new instance of the shader with the given settings
     */
    void start(PSS settings);

    void clear();

    void clientTick();

    void renderOverlay(GuiGraphics graphics, float partialTicks);

    /**
     * Render hook with full render context. The manager calls this only for fixed stages
     */
    default void renderStage(PostShaderRenderContext context) {
        ;;
    }

    /**
     * Declares which render stages this shader wants to run on
     */
    default EnumSet<PostShaderRenderStage> stages() {
        return EnumSet.noneOf(PostShaderRenderStage.class);
    }

    /**
     * Called when shaders/resources are (re)loaded or when the main render target changes size
     */
    default void initOrReload(TextureManager textures, ResourceManager resources, RenderTarget target) {
        ;;
    }

    default void onLogout() {
        clear();
    }

}
