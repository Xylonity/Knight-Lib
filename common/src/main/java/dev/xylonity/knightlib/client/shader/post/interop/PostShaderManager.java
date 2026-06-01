package dev.xylonity.knightlib.client.shader.post.interop;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.client.shader.post.PostShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central registry and tick/render dispatcher for all custom post-processing shaders.
 *
 * Every public method must be called from the render thread.
 */
public final class PostShaderManager {

    private static final Map<ResourceLocation, PostShader<?>> SHADERS = new LinkedHashMap<>();
    private static final EnumMap<PostShaderRenderStage, List<PostShader<?>>> BY_STAGE = new EnumMap<>(PostShaderRenderStage.class);
    private static final Set<ResourceLocation> WARNED_MISSING_IDS = new HashSet<>();

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
        final PostShader<?> previous = SHADERS.put(shader.id(), shader);

        // Fabric can fire shader registration callbacks more than once during startup, so this must be idempotent.
        if (previous != null) {
            unindex(previous);
            if (previous != shader) {
                previous.dispose();
            }

        }
        else {
            unindex(shader);
        }

        for (final PostShaderRenderStage stage : shader.stages()) {
            BY_STAGE.computeIfAbsent(stage, renderStage -> new ArrayList<>()).add(shader);
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
        final PostShader<?> removed = SHADERS.remove(id);
        if (removed != null) {
            unindex(removed);
            removed.dispose();
        }

    }

    private static void unindex(PostShader<?> shader) {
        for (final List<PostShader<?>> bucket : BY_STAGE.values()) {
            bucket.removeIf(postShader -> postShader == shader);
        }

    }

    @SuppressWarnings("unchecked")
    public static <PSS extends PostShaderSettings> PostShader<PSS> get(ResourceLocation id) {
        return (PostShader<PSS>) SHADERS.get(id);
    }

    public static boolean isRegistered(ResourceLocation id) {
        return SHADERS.containsKey(id);
    }

    public static Collection<PostShader<?>> all() {
        return Collections.unmodifiableCollection(SHADERS.values());
    }

    public static <PSS extends PostShaderSettings> void start(ResourceLocation id, PSS settings) {
        final PostShader<PSS> shader = get(id);
        if (shader == null) {
            if (WARNED_MISSING_IDS.add(id)) {
                KnightLib.LOGGER.warn("[KnightLib] Tried to start unknown post shader: {}", id);
            }

            return;
        }

        shader.start(settings);
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
     * Uses the inverted stage index, so unrelated shaders are not even iterated.
     */
    public static void renderStage(PostShaderRenderContext context) {
        final List<PostShader<?>> bucket = BY_STAGE.get(context.stage);
        if (bucket == null || bucket.isEmpty()) {
            return;
        }

        for (final PostShader<?> shader : bucket) {
            shader.renderStage(context);
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
     * Called when the client world unloads (like on dimension change).
     */
    public static void onWorldUnload() {
        for (final PostShader<?> shader : SHADERS.values()) {
            shader.onWorldUnload();
        }

    }

    /**
     * Reinitialises all shaders against the (possibly new) client resources.
     * Called both on the first shader registration pass and after F3+T reloads.
     */
    public static void onResourcesReloaded(TextureManager textures, ResourceManager resources, RenderTarget target) {
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
        BY_STAGE.clear();
        WARNED_MISSING_IDS.clear();
    }

}