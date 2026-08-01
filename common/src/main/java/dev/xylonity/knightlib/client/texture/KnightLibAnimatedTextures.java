package dev.xylonity.knightlib.client.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.xylonity.knightlib.KnightLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves standalone textures used by KnightLib renderers without modifying their original TextureManager entry
 */
@ApiStatus.Internal
public final class KnightLibAnimatedTextures {

    private static final String ALIAS_PREFIX = "__animated_texture__/";
    private static final Map<ResourceLocation, Entry> ENTRIES = new HashMap<>();

    private static final AbstractTexture ABSENT_TEXTURE = new AbstractTexture() {

        @Override
        public void load(@NotNull ResourceManager resourceManager) {
            ;;
        }

    };

    private static int resourceGeneration;

    private KnightLibAnimatedTextures() {
        ;;
    }

    /**
     * Returns the texture location that should be passed to a vanilla RenderType
     */
    public static synchronized ResourceLocation resolve(ResourceLocation source) {
        Objects.requireNonNull(source, "source");
        if (isInternalAlias(source)) {
            return source;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final TextureManager textureManager = minecraft.getTextureManager();

        // An explicitly registered texture subclass owns its own loading semantics
        final AbstractTexture originalTexture = textureManager.getTexture(source, ABSENT_TEXTURE);
        if (originalTexture != ABSENT_TEXTURE && originalTexture.getClass() != SimpleTexture.class) {
            return source;
        }

        final Entry entry = ENTRIES.computeIfAbsent(source, Entry::new);
        if (entry.metadataGeneration != resourceGeneration) {
            entry.refreshMetadata(minecraft.getResourceManager(), resourceGeneration);
        }

        if (!entry.hasAnimationMetadata) {
            entry.releaseOwnedTexture(textureManager);
            return source;
        }

        return entry.resolve(textureManager);
    }

    /**
     * Invalidates metadata decisions after a resourcepack change
     */
    public static synchronized void onResourceReload() {
        resourceGeneration++;
    }

    private static boolean isInternalAlias(ResourceLocation location) {
        return KnightLib.MOD_ID.equals(location.getNamespace()) && location.getPath().startsWith(ALIAS_PREFIX);
    }

    private static ResourceLocation aliasFor(ResourceLocation source) {
        return KnightLib.of(ALIAS_PREFIX + source.getNamespace() + "/" + source.getPath());
    }

    private static boolean hasAnimationMetadata(ResourceManager resourceManager, ResourceLocation source) {
        final Resource resource = resourceManager.getResource(source).orElse(null);
        if (resource == null) {
            return false;
        }

        try {
            return resource.metadata().getSection(AnimationMetadataSection.SERIALIZER).isPresent();
        }
        catch (Exception exception) {
            KnightLib.LOGGER.warn("Failed reading animation metadata for {}", source, exception);
            return false;
        }

    }

    private static final class Entry {

        private final ResourceLocation source;
        private final ResourceLocation alias;

        private int metadataGeneration = Integer.MIN_VALUE;
        private boolean hasAnimationMetadata;
        private boolean registrationFailed;
        private boolean warnedAliasCollision;
        @Nullable
        private KnightLibAnimatedTexture ownedTexture;

        private Entry(ResourceLocation source) {
            this.source = source;
            this.alias = aliasFor(source);
        }

        private void refreshMetadata(ResourceManager resourceManager, int generation) {
            this.hasAnimationMetadata = KnightLibAnimatedTextures.hasAnimationMetadata(resourceManager, source);
            this.metadataGeneration = generation;
            this.registrationFailed = false;
            this.warnedAliasCollision = false;
        }

        private ResourceLocation resolve(TextureManager textureManager) {
            if (registrationFailed) {
                return source;
            }

            final AbstractTexture existing = textureManager.getTexture(alias, ABSENT_TEXTURE);
            if (existing instanceof KnightLibAnimatedTexture animatedTexture && animatedTexture.source().equals(source)) {
                ownedTexture = animatedTexture;
                return alias;
            }

            if (existing != ABSENT_TEXTURE) {
                if (existing == MissingTextureAtlasSprite.getTexture()) {
                    registrationFailed = true;
                }
                else if (!warnedAliasCollision) {
                    warnedAliasCollision = true;
                    KnightLib.LOGGER.warn("Internal animated-texture alias {} is already owned by {}, using the original texture {}", alias, existing.getClass().getName(), source);
                }

                return source;
            }

            final KnightLibAnimatedTexture candidate = new KnightLibAnimatedTexture(source);
            textureManager.register(alias, candidate);
            if (textureManager.getTexture(alias, ABSENT_TEXTURE) == candidate) {
                ownedTexture = candidate;
                return alias;
            }

            candidate.close();
            candidate.releaseId();

            registrationFailed = true;

            return source;
        }

        private void releaseOwnedTexture(TextureManager textureManager) {
            final AbstractTexture existing = textureManager.getTexture(alias, ABSENT_TEXTURE);
            final boolean ownsExisting = existing == ownedTexture || existing instanceof final KnightLibAnimatedTexture animatedTexture && animatedTexture.source().equals(source);
            if (ownsExisting) {
                textureManager.release(alias);
            }

            ownedTexture = null;
        }

    }

    /**
     * A standalone texture whose frames and interpolation are delegated to vanilla {@link SpriteContents}
     */
    private static final class KnightLibAnimatedTexture extends SimpleTexture implements Tickable {

        @Nullable
        private SpriteContents contents;
        @Nullable
        private SpriteTicker ticker;
        private boolean uploaded;
        private boolean blur;
        private boolean clamp;

        private KnightLibAnimatedTexture(ResourceLocation source) {
            super(source);
        }

        private ResourceLocation source() {
            return location;
        }

        @Override
        public synchronized void load(ResourceManager resourceManager) throws IOException {
            closeAnimationData();

            final Resource resource = resourceManager.getResourceOrThrow(location);
            final ResourceMetadata metadata = resource.metadata();
            final AnimationMetadataSection animationMetadata;
            try {
                animationMetadata = metadata
                        .getSection(AnimationMetadataSection.SERIALIZER)
                        .orElse(null);
            }
            catch (Exception exception) {
                KnightLib.LOGGER.warn("Invalid animation metadata for {}, loading it as a static texture", location, exception);
                super.load(resourceManager);
                return;
            }

            if (animationMetadata == null) {
                super.load(resourceManager);
                return;
            }

            TextureMetadataSection textureMetadata = null;
            try {
                textureMetadata = metadata.getSection(TextureMetadataSection.SERIALIZER).orElse(null);
            }
            catch (RuntimeException exception) {
                KnightLib.LOGGER.warn("Invalid texture metadata for {}, using vanilla defaults", location, exception);
            }

            final NativeImage image;
            try (final InputStream stream = resource.open()) {
                image = NativeImage.read(stream);
            }

            final FrameSize frameSize;
            try {
                frameSize = animationMetadata.calculateFrameSize(image.getWidth(), image.getHeight());
            }
            catch (RuntimeException exception) {
                image.close();
                KnightLib.LOGGER.warn("Invalid animated frame size for {}, loading it as a static texture", location, exception);
                super.load(resourceManager);
                return;
            }

            if (frameSize.width() <= 0 || frameSize.height() <= 0 || image.getWidth() % frameSize.width() != 0 || image.getHeight() % frameSize.height() != 0) {
                KnightLib.LOGGER.warn("Texture {} size {}x{} is not a multiple of its animated frame size {}x{}, loading it as a static texture", location, image.getWidth(), image.getHeight(), frameSize.width(), frameSize.height());
                image.close();
                super.load(resourceManager);
                return;
            }

            final SpriteContents nextContents;
            try {
                nextContents = new SpriteContents(location, frameSize, image, animationMetadata);
            }
            catch (RuntimeException exception) {
                image.close();
                KnightLib.LOGGER.warn("Failed creating animated frames for {}; loading it as a static texture", location, exception);
                super.load(resourceManager);
                return;
            }

            this.blur = textureMetadata != null && textureMetadata.isBlur();
            this.clamp = textureMetadata != null && textureMetadata.isClamp();
            this.contents = nextContents;
            this.ticker = nextContents.createTicker();

            scheduleFirstFrameUpload(nextContents, frameSize);
        }

        private void scheduleFirstFrameUpload(SpriteContents expectedContents, FrameSize frameSize) {
            final Runnable upload = () -> uploadFirstFrame(expectedContents, frameSize.width(), frameSize.height());
            if (RenderSystem.isOnRenderThreadOrInit()) {
                upload.run();
            }
            else {
                RenderSystem.recordRenderCall(upload::run);
            }

        }

        private synchronized void uploadFirstFrame(SpriteContents expectedContents, int width, int height) {
            if (contents != expectedContents) {
                return;
            }

            TextureUtil.prepareImage(getId(), 0, width, height);
            expectedContents.uploadFirstFrame(0, 0);

            applyTextureParameters();

            uploaded = true;
        }

        @Override
        public void tick() {
            final SpriteTicker expectedTicker;
            synchronized (this) {
                if (!uploaded || ticker == null) {
                    return;
                }

                expectedTicker = ticker;
            }

            if (RenderSystem.isOnRenderThread()) {
                tickOnRenderThread(expectedTicker);
            }
            else {
                RenderSystem.recordRenderCall(() -> tickOnRenderThread(expectedTicker));
            }

        }

        private synchronized void tickOnRenderThread(SpriteTicker expectedTicker) {
            if (!uploaded || ticker != expectedTicker) {
                return;
            }

            bind();

            expectedTicker.tickAndUpload(0, 0);
            applyTextureParameters();
        }

        private void applyTextureParameters() {
            setFilter(blur, false);
            final int wrapping = clamp ? GL12.GL_CLAMP_TO_EDGE : GL11.GL_REPEAT;
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapping);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapping);
        }

        private void closeAnimationData() {
            uploaded = false;
            if (ticker != null) {
                ticker.close();
                ticker = null;
            }
            if (contents != null) {
                contents.close();
                contents = null;
            }

        }

        @Override
        public synchronized void close() {
            closeAnimationData();
            super.close();
        }

    }

}