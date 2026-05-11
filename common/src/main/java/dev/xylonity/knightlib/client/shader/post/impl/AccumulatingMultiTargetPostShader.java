package dev.xylonity.knightlib.client.shader.post.impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.xylonity.knightlib.client.shader.post.*;
import dev.xylonity.knightlib.client.shader.post.interop.MultiTargetPostShaderInstance;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderContext;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderStage;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderSettings;
import dev.xylonity.knightlib.mixin.PostChainAccessor;
import dev.xylonity.knightlib.mixin.PostPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic multi-instance entity-driven post shader dispatcher
 *
 * Vanilla {@link PostChain} is typically used "globally" (one chain, one set of targets, one uniform set...), so
 * when multiple entities try to drive the same post effect in the same frame, they conflict, causing flicker and
 * a huge set of possible visual glitches.
 *
 * This base class solves it by maintaining a registry of instances keyed by K, giving each key its own PostChain,
 * accumulating per-instance uniforms per frame and processing instances sequentially in a chosen render stage.
 *
 * {@link Instance#prepareForDraw()} copies depth from minecraft:main and clears only the color on aux targets,
 * keeping depth-based occlusion stable while avoiding stale color
 *
 * @param <PSS> Optional settings for the current post shader
 * @param <K> Object which the current post shader is linked to (must be an idempotent-key, like a UUID or an entity id)
 *
 * @author Xylonity
 */
public abstract class AccumulatingMultiTargetPostShader<PSS extends PostShaderSettings, K> extends AbstractPostShader<PSS> {

    private final ResourceLocation postShaderJson;
    private final String[] targetNames;

    private final Map<K, Instance> instances = new LinkedHashMap<>();
    private int instanceOrdinal = 0;

    private TextureManager textures;
    private ResourceManager resources;
    private RenderTarget initTarget;

    private long frameId = 0L;

    private EnumSet<PostShaderRenderStage> stagesCache;

    protected AccumulatingMultiTargetPostShader(ResourceLocation postJson, String... targetNames) {
        this.postShaderJson = postJson;
        this.targetNames = targetNames;
    }

    /**
     * Which render stage is used to reset/GC instances and clear per-frame accumulators. Most of the time you want
     * to use {@link PostShaderRenderStage#FRAME_BEGIN}
     */
    protected abstract PostShaderRenderStage stageBegin();

    /**
     * Which render stage is used to actually run PostChains for active instances. For entity-only effects,
     * {@link PostShaderRenderStage#AFTER_LEVEL} is typically correct
     */
    protected abstract PostShaderRenderStage stageProcess();

    /**
     * Called right before processing an instance, with that instance's accumulated values
     */
    protected abstract void applyUniforms(PostShaderRenderContext context, Instance instance, float intensity, float mix01);

    /**
     * TTL (in game ticks) used to garbage collect instances that weren't touched for a while, preventing memory leaks
     * when entities despawn or leave the client view
     */
    protected long instanceCleanTicks() {
        return 200L;
    }

    /**
     * Returns a short stable identifier for the given key, used as a "prefix" of unique RenderType names
     */
    protected String keyId(K key) {
        return Integer.toHexString(key.hashCode());
    }

    /**
     * Accumulation policy that by default keeps the maximum each frame
     */
    protected float combineIntensity(float current, float add) {
        return Math.max(current, add);
    }

    protected float combineMix(float current, float add) {
        return Math.max(current, add);
    }

    @Override
    public void initOrReload(TextureManager textures, ResourceManager resources, RenderTarget target) {
        this.textures = textures;
        this.resources = resources;
        this.initTarget = target;

        // Initializes internal shader bookkeeping in AbstractPostShader
        super.initOrReload(textures, resources, target);

        // Marks all existing instances so they recreate their PostChain using new resources
        for (Instance instance : instances.values()) {
            instance.invalidateChain();
        }

    }

    @Override
    public final EnumSet<PostShaderRenderStage> stages() {
        if (stagesCache == null) {
            stagesCache = EnumSet.of(stageBegin(), stageProcess());
        }

        return stagesCache;
    }

    @Override
    public void clear() {
        for (final Instance instance : instances.values()) {
            instance.dispose();
        }

        instances.clear();
    }

    @Override
    public void renderStage(PostShaderRenderContext context) {
        if (context.stage == stageBegin()) {
            beginFrame();
            return;
        }

        if (context.stage == stageProcess()) {
            endFrame(context);
        }

    }

    /**
     * Returns or creates the per-key instance.
     * Call this from your entity layer/renderer using its id or UUID.
     */
    public final Instance instance(K key) {
        Instance instance = instances.get(key);
        if (instance == null) {
            final String id = keyId(key) + "_" + Integer.toHexString(instanceOrdinal++);
            instance = new Instance(key, id);
            instances.put(key, instance);
        }

        return instance;
    }

    /**
     * Current monotonic frame id
     */
    public final long frameId() {
        return frameId;
    }

    private void beginFrame() {
        frameId++;

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }

        final long gameTime = minecraft.level.getGameTime();
        final long cleanTicks = instanceCleanTicks();

        // Resets all instances and drops old ones to avoid leaks
        final Iterator<Map.Entry<K, Instance>> iterator = instances.entrySet().iterator();
        while (iterator.hasNext()) {
            final Instance instance = iterator.next().getValue();
            if ((gameTime - instance.lastTouchedTick) > cleanTicks) {
                instance.dispose();
                iterator.remove();

                continue;
            }

            instance.onFrameBegin();
        }

    }

    private void endFrame(PostShaderRenderContext context) {
        if (instances.isEmpty()) {
            return;
        }

        // Processes each instance that was activated this frame. Each instance has its own PostChain and targets,
        // so they do not overwrite each other
        for (Instance instance : instances.values()) {
            if (!instance.activeThisFrame) {
                continue;
            }

            instance.process(context);
        }

    }

    protected final ResourceLocation postShaderJson() {
        return postShaderJson;
    }

    protected final String[] targetNames() {
        return targetNames;
    }

    protected final TextureManager textures() {
        return textures;
    }

    protected final ResourceManager resources() {
        return resources;
    }

    protected final RenderTarget initTarget() {
        return initTarget;
    }

    /**
     * Clears only the color buffer (alpha included) preserving the depth buffer
     */
    protected static void clearColorOnly(RenderTarget target) {
        target.bindWrite(true);
        RenderSystem.clearColor(0f, 0f, 0f, 0f);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, true);
    }

    /**
     * Per-key instance. Mods can build RenderTypes that bind outputs to this specific instance via
     * {@link #target(String)} and {@link #keyId()}.
     *
     * This instance owns a PostChain and its auxiliary targets, accumulates uniform inputs, prepares targets per
     * frame and runs the chain once at {@link #stageProcess()} when active
     */
    public final class Instance implements MultiTargetPostShaderInstance {

        private final K key;
        private final String keyId;

        // The per-instance PostChain, created lazily
        private PostChain chain;

        // Cached auxiliary targets for this chain
        private final Map<String, RenderTarget> targets = new LinkedHashMap<>();

        private boolean activeThisFrame = false;
        private float intensityThisFrame = 0f;
        private float mixThisFrame = 0f;

        // Ensures we do the expensive prepare step at most once per frame (this is set to the current frameId)
        private long preparedFrameId = -1L;
        private int lastW = -1;
        private int lastH = -1;

        private long lastTouchedTick = 0L;

        private Instance(K key, String keyId) {
            this.key = key;
            this.keyId = keyId;
        }

        public K key() {
            return key;
        }

        @Override
        public String keyId() {
            return keyId;
        }

        /**
         * Accumulates values from this frame.
         * Call this multiple times if needed (configurable via {@link #combineIntensity(float, float)}
         * and {@link #combineMix(float, float)}).
         */
        public void accumulate(float intensity, float mix01) {
            touch();
            activeThisFrame = true;
            intensityThisFrame = combineIntensity(intensityThisFrame, intensity);
            mixThisFrame = combineMix(mixThisFrame, Mth.clamp(mix01, 0f, 1f));
        }

        /**
         * Prepares auxiliary targets for rendering mask/textures into them
         */
        public void prepareForDraw() {
            touch();
            ensureReady();

            final long currentFrameId = frameId();
            if (preparedFrameId == currentFrameId) {
                return;
            }

            preparedFrameId = currentFrameId;

            final RenderTarget main = Minecraft.getInstance().getMainRenderTarget();

            // Copies depth from main so occlusion works against the world geometry
            for (RenderTarget target : targets.values()) {
                target.copyDepthFrom(main);
            }
            main.bindWrite(true);

            // Clears color only, preserving depth
            for (final RenderTarget target : targets.values()) {
                clearColorOnly(target);
            }

            main.bindWrite(true);
        }

        /**
         * Returns a named auxiliary target, used by RenderTypes to bind the OutputState to this instance
         */
        @Override
        public RenderTarget target(String name) {
            ensureReady();
            return targets.get(name);
        }

        private void onFrameBegin() {
            activeThisFrame = false;
            intensityThisFrame = 0f;
            mixThisFrame = 0f;
            preparedFrameId = -1L;
        }

        private void process(PostShaderRenderContext context) {
            ensureReady();

            final float intensity = Mth.clamp(intensityThisFrame, 0f, 2f);
            final float mix = Mth.clamp(mixThisFrame, 0f, 1f);

            applyUniforms(context, this, intensity, mix);

            // Executes the chain safely with standard GL state cleanup
            AccumulatingMultiTargetPostShader.this.process(chain, context.partialTicks, () -> {
                RenderSystem.disableBlend();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.resetTextureMatrix();
            });

            // Restores the main target for subsequent rendering
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }

        /**
         * Ensures the PostChain exists, is sized and that auxiliary targets are linked.
         * Called lazily because instances might not be used every single frame
         */
        private void ensureReady() {
            if (textures() == null || resources() == null || initTarget() == null) {
                throw new IllegalStateException("[KnightLib] PostShader not initialized (textures/resources/target missing).");
            }

            // Creates the chain lazily
            if (chain == null) {
                try {
                    chain = new PostChain(textures(), resources(), initTarget(), postShaderJson());
                }
                catch (Exception exception) {
                    throw new RuntimeException("[KnightLib] Failed to load post chain: " + postShaderJson(), exception);
                }

                targets.clear();
            }

            final Minecraft minecraft = Minecraft.getInstance();
            final int width = minecraft.getWindow().getWidth();
            final int height = minecraft.getWindow().getHeight();
            if (width != lastW || height != lastH) {
                lastW = width;
                lastH = height;
                targets.clear();
                preparedFrameId = -1L;
            }

            // Ensures the internal chain targets are resized properly
            AccumulatingMultiTargetPostShader.this.ensureSized(chain);

            // Links render targets by name
            relinkTargets();
        }

        /**
         * Links named auxiliary targets from the PostChain into the local map
         */
        private void relinkTargets() {
            if (!targets.isEmpty()) {
                return;
            }

            final Map<String, RenderTarget> renderTargetMap = ((PostChainAccessor) chain).knightlib$getTargets();
            for (final String targetName : targetNames()) {
                final RenderTarget target = renderTargetMap.get(targetName);
                if (target == null) {
                    throw new IllegalStateException("[KnightLib] Missing target '" + targetName + "' in chain " + postShaderJson());
                }

                targets.put(targetName, target);
            }

        }

        public EffectInstance effect(int passIndex) {
            try {
                final PostPass pass = ((PostChainAccessor) chain).knightlib$getPasses().get(passIndex);
                return ((PostPassAccessor) pass).knightlib$getEffect();
            }
            catch (Exception exception) {
                return null;
            }

        }

        private void touch() {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                lastTouchedTick = minecraft.level.getGameTime();
            }

        }

        private void invalidateChain() {
            dispose();
        }

        private void dispose() {
            if (chain != null) {
                try {
                    chain.close();
                }
                catch (Exception ignored) {
                    ;;
                }

            }

            chain = null;
            targets.clear();
            preparedFrameId = -1L;
        }

    }

}