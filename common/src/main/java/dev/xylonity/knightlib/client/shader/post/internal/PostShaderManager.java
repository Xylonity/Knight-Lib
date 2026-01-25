package dev.xylonity.knightlib.client.shader.post.internal;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PostShaderManager {

    private static final Map<ResourceLocation, PostShader<?>> POST_SHADERS = new LinkedHashMap<>();

    private PostShaderManager() {
        ;;
    }

    /**
     * Registers a post shader and immediately reloads it against the current client resources
     */
    public static void register(PostShader<?> postShader) {
        POST_SHADERS.put(postShader.id(), postShader);

        final Minecraft minecraft = Minecraft.getInstance();
        postShader.initOrReload(
                minecraft.getTextureManager(),
                minecraft.getResourceManager(),
                minecraft.getMainRenderTarget()
        );
    }

    @SuppressWarnings("unchecked")
    public static <PSS extends PostShaderSettings> PostShader<PSS> get(ResourceLocation id) {
        return (PostShader<PSS>) POST_SHADERS.get(id);
    }

    public static Collection<PostShader<?>> all() {
        return POST_SHADERS.values();
    }

    public static <PSS extends PostShaderSettings> void start(ResourceLocation id, PSS settings) {
        final PostShader<PSS> postShader = get(id);
        if (postShader != null) {
            postShader.start(settings);
        }

    }

    /**
     * Per-tick update for all shaders present in the current level
     */
    public static void clientTick() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        for (PostShader<?> postShader : POST_SHADERS.values()) {
            postShader.clientTick();
        }

    }

    public static void renderOverlay(GuiGraphics graphics, float partialTicks) {
        for (PostShader<?> postShader : POST_SHADERS.values()) {
            postShader.renderOverlay(graphics, partialTicks);
        }

    }

    /**
     * Each shader declares which stages it can be rendered into
     * @see PostShaderRenderStage
     */
    public static void renderStage(PostShaderRenderContext context) {
        for (PostShader<?> postShader : POST_SHADERS.values()) {
            if (postShader.stages().contains(context.stage)) {
                postShader.renderStage(context);
            }

        }

    }

    /**
     * Called when the client triggers a disconnection. Safely clears each shader
     */
    public static void onLogout() {
        for (PostShader<?> postShader : POST_SHADERS.values()) {
            postShader.onLogout();
        }

    }

    /**
     * Reinitializes all shaders after a client resource reload is performed
     * @see dev.xylonity.knightlib.api.event.impl.client.ClientResourcesReloadedEvent event that should be triggered
     */
    public static void onRegisterShaders(TextureManager textures, ResourceManager resources, RenderTarget target) {
        for (PostShader<?> postShader : POST_SHADERS.values()) {
            postShader.initOrReload(textures, resources, target);
        }

    }

}