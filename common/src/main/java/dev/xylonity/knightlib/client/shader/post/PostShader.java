package dev.xylonity.knightlib.client.shader.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderManager;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderContext;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderStage;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderSettings;
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
     */
    ResourceLocation id();

    /**
     * Which render stages this shader wants to be dispatched in.
     * This is called once during registration, since the result is cached by the manager.
     */
    EnumSet<PostShaderRenderStage> stages();

    /**
     * Executes the post-processing pass for the given stage context.
     */
    void renderStage(PostShaderRenderContext context);

    /**
     * (Re)loads the underlying PostChain(s) against current client resources.
     */
    void initOrReload(TextureManager textures, ResourceManager resources, RenderTarget target);

    /**
     * Activates/queues a new instance of the effect with the given settings (shaders with an accumulation per frame do ignore this).
     */
    default void start(PSS settings) {
        ;;
    }

    /**
     * Immediately cancels all running instances of this shader effect.
     */
    default void clear() {
        ;;
    }

    /**
     * Per-tick update (called every client tick while the current level is loaded).
     */
    default void clientTick() {
        ;;
    }

    /**
     * Renders any HUD/overlay elements this shader needs.
     */
    default void renderOverlay(GuiGraphics graphics, float partialTicks) {
        ;;
    }

    /**
     * Called on client disconnect.
     */
    default void onLogout() {
        clear();
    }

    /**
     * Called when the client world unloads (like on dimension change) so certain shader effects can persist.
     */
    default void onWorldUnload() {
        onLogout();
    }

    /**
     * Releases GPU resources (PostChain framebuffers, compiled programs, etc.).
     * Called when the shader is unregistered, replaced, or the client shuts down.
     */
    default void dispose() {
        ;;
    }

}
