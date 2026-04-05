package dev.xylonity.knightlib.client.shader.post.internal;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry and tick/render dispatcher for all custom post-processing shaders.
 *
 * Every public method must be called from the render thread, just in case.
 */
public final class PostShaderManager {

    private static final Map<ResourceLocation, PostShader<?>> SHADERS = new LinkedHashMap<>();

    private PostShaderManager() {
        ;;
    }

    /**
     * Registers (or replaces) a custom post shader.
     *
     * If a shader with the same {@link PostShader#id() id} was already
     * registered, it is {@linkplain PostShader#dispose() disposed} first so that
     * GPU resources are not leaked.
     */
    public static void register(PostShader<?> shader) {
        final PostShader<?> oldPostShader = SHADERS.put(shader.id(), shader);
        if (oldPostShader != null && oldPostShader != shader) {
            oldPostShader.dispose();
        }

        final Minecraft minecraft = Minecraft.getInstance();
        shader.initOrReload(
                minecraft.getTextureManager(),
                minecraft.getResourceManager(),
                minecraft.getMainRenderTarget()
        );
    }

    /**
     * Removes and disposes a custom post shader by id.
     */
    public static void unregister(ResourceLocation id) {
        final PostShader<?> removedPostShader = SHADERS.remove(id);
        if (removedPostShader != null) {
            removedPostShader.dispose();
        }

    }

    @SuppressWarnings("unchecked")
    public static <PSS extends PostShaderSettings> PostShader<PSS> get(ResourceLocation id) {
        return (PostShader<PSS>) SHADERS.get(id);
    }

    public static Collection<PostShader<?>> all() {
        return Collections.unmodifiableCollection(SHADERS.values());
    }

    public static <PSS extends PostShaderSettings> void start(ResourceLocation id, PSS settings) {
        final PostShader<PSS> shader = get(id);
        if (shader != null) {
            shader.start(settings);
        }

    }

    public static void stop(ResourceLocation id) {
        final PostShader<?> shader = SHADERS.get(id);
        if (shader != null) {
            shader.clear();
        }

    }

    public static void stopAll() {
        for (final PostShader<?> shader : SHADERS.values()) {
            shader.clear();
        }

    }

    public static void clientTick() {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        for (final PostShader<?> shader : SHADERS.values()) {
            shader.clientTick();
        }

    }

    public static void renderOverlay(GuiGraphics graphics, float partialTicks) {
        for (final PostShader<?> shader : SHADERS.values()) {
            shader.renderOverlay(graphics, partialTicks);
        }

    }

    /**
     * Dispatches a render stage to every shader that declared interest in it.
     *
     * @see PostShaderRenderStage
     */
    public static void renderStage(PostShaderRenderContext context) {
        for (final PostShader<?> shader : SHADERS.values()) {
            if (shader.stages().contains(context.stage)) {
                shader.renderStage(context);
            }

        }

    }

    /**
     * Called on client disconnect.
     */
    public static void onLogout() {
        for (final PostShader<?> shader : SHADERS.values()) {
            shader.onLogout();
        }

    }

    /**
     * Reinitialises all shaders after a resource-pack reload.
     */
    public static void onRegisterShaders(TextureManager textures, ResourceManager resources, RenderTarget target) {
        for (final PostShader<?> shader : SHADERS.values()) {
            shader.initOrReload(textures, resources, target);
        }

    }

    /**
     * Disposes every shader and empties the registry.
     */
    public static void disposeAll() {
        for (final PostShader<?> shader : SHADERS.values()) {
            shader.dispose();
        }

        SHADERS.clear();
    }

}