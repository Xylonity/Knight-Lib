package dev.xylonity.knightlib.client.shader.post.internal;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.EnumSet;

/**
 * Generic contract for a post-processing shader managed by {@link PostShaderManager}.
 *
 * @param <PSS> the settings type that configures each shader
 */
public interface PostShader<PSS extends PostShaderSettings> {

    /**
     * Unique identifier used as the registry key
     * */
    ResourceLocation id();

    /**
     * Which render stages this shader should be dispatched in.
     */
    EnumSet<PostShaderRenderStage> stages();

    /**
     * Activates/queues a new instance of the shader effect with the given settings.
     */
    void start(PSS settings);

    /**
     * Immediately cancels all running instances of this shader effect.
     */
    void clear();

    /**
     * Per-tick update (called every client tick while the current level is loaded).
     */
    void clientTick();

    /**
     * (Re)loads the underlying PostChain against current client resources.
     */
    void initOrReload(TextureManager textures, ResourceManager resources, RenderTarget target);

    /**
     * Renders any HUD/overlay elements this shader needs.
     */
    void renderOverlay(GuiGraphics graphics, float partialTicks);

    /**
     * Executes the post-processing pass for the given stage context.
     */
    void renderStage(final PostShaderRenderContext context);

    /**
     * Called on client disconnect.
     */
    void onLogout();

    /**
     * Releases GPU resources (PostChain framebuffers, compiled programs, etc.).
     * Called when the shader is unregistered, replaced, or the client shuts down.
     */
    default void dispose() {
        ;;
    }

}