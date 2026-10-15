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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Gives standalone textures vanilla-style {@code .mcmeta} animation without replacing their normal {@link TextureManager} entry.
 * Animated files are loaded under a private alias, while static files keep using their original location and pay no extra cost.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/cache/texture/AnimatableTexture.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/mixin/client/TextureManagerMixin.java
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
     * A standalone texture that plays its own frames. Vanilla ticker is avoided on purpose because Sodium/Embeddium cancel
     * its {@code tickAndUpload} for every sprite they didn't see rendered, and nothing ever marks a texture outside an
     * atlas as visible, so it would stay frozen on its first frame
     */
    private static final class KnightLibAnimatedTexture extends SimpleTexture implements Tickable {

        @Nullable
        private Animation animation;
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

            final Animation nextAnimation;
            try {
                nextAnimation = Animation.create(location, image, frameSize, animationMetadata);
            }
            catch (RuntimeException exception) {
                image.close();
                KnightLib.LOGGER.warn("Failed creating animated frames for {}, loading it as a static texture", location, exception);
                super.load(resourceManager);
                return;
            }

            this.blur = textureMetadata != null && textureMetadata.isBlur();
            this.clamp = textureMetadata != null && textureMetadata.isClamp();
            this.animation = nextAnimation;

            scheduleFirstFrameUpload(nextAnimation);
        }

        private void scheduleFirstFrameUpload(Animation expectedAnimation) {
            final Runnable upload = () -> uploadFirstFrame(expectedAnimation);
            if (RenderSystem.isOnRenderThreadOrInit()) {
                upload.run();
            }
            else {
                RenderSystem.recordRenderCall(upload::run);
            }

        }

        private synchronized void uploadFirstFrame(Animation expectedAnimation) {
            if (animation != expectedAnimation) {
                return;
            }

            TextureUtil.prepareImage(getId(), 0, expectedAnimation.frameWidth, expectedAnimation.frameHeight);
            expectedAnimation.uploadFirstFrame();

            applyTextureParameters();

            uploaded = true;
        }

        @Override
        public void tick() {
            final Animation expectedAnimation;
            synchronized (this) {
                if (!uploaded || animation == null || !animation.isAnimated()) {
                    return;
                }

                expectedAnimation = animation;
            }

            if (RenderSystem.isOnRenderThread()) {
                tickOnRenderThread(expectedAnimation);
            }
            else {
                RenderSystem.recordRenderCall(() -> tickOnRenderThread(expectedAnimation));
            }

        }

        private synchronized void tickOnRenderThread(Animation expectedAnimation) {
            if (!uploaded || animation != expectedAnimation) {
                return;
            }

            bind();

            expectedAnimation.tickAndUpload();
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
            if (animation != null) {
                animation.close();
                animation = null;
            }

        }

        @Override
        public synchronized void close() {
            closeAnimationData();
            super.close();
        }

    }

    /**
     * Copy of vanilla's sprite animation ticker, keeps a pointer for the current rendered frame
     */
    private static final class Animation implements AutoCloseable {

        private final NativeImage sheet;
        private final int frameWidth;
        private final int frameHeight;
        private final int framesPerRow;
        private final List<Frame> frames;
        @Nullable
        private final NativeImage interpolatedFrame;

        private int frame;
        private int subFrame;

        private Animation(NativeImage sheet, FrameSize frameSize, List<Frame> frames, boolean interpolate) {
            this.sheet = sheet;
            this.frameWidth = frameSize.width();
            this.frameHeight = frameSize.height();
            this.framesPerRow = sheet.getWidth() / frameWidth;
            this.frames = frames;
            this.interpolatedFrame = interpolate && frames.size() > 1 ? new NativeImage(frameWidth, frameHeight, false) : null;
        }

        private static Animation create(ResourceLocation location, NativeImage sheet, FrameSize frameSize, AnimationMetadataSection metadata) {
            final int frameCount = (sheet.getWidth() / frameSize.width()) * (sheet.getHeight() / frameSize.height());

            final List<Frame> declaredFrames = new ArrayList<>();
            metadata.forEachFrame((index, time) -> declaredFrames.add(new Frame(index, time)));

            final List<Frame> frames = new ArrayList<>();
            if (declaredFrames.isEmpty()) {
                for (int i = 0; i < frameCount; i++) {
                    frames.add(new Frame(i, metadata.getDefaultFrameTime()));
                }

            }
            else {
                for (Frame frame : declaredFrames) {
                    if (frame.time() > 0 && frame.index() >= 0 && frame.index() < frameCount) {
                        frames.add(frame);
                    }
                    else {
                        KnightLib.LOGGER.warn("Ignoring invalid frame {} (time {}) in animated texture {}", frame.index(), frame.time(), location);
                    }

                }

            }

            // Same as vanilla, a sheet with no usable frames shows the first one
            if (frames.isEmpty()) {
                frames.add(new Frame(0, metadata.getDefaultFrameTime()));
            }

            return new Animation(sheet, frameSize, List.copyOf(frames), metadata.isInterpolatedFrames());
        }

        private boolean isAnimated() {
            return frames.size() > 1;
        }

        private void uploadFirstFrame() {
            uploadFrame(frames.get(0).index());
        }

        /**
         * Advances one client tick
         */
        private void tickAndUpload() {
            final Frame current = frames.get(frame);
            if (++subFrame >= current.time()) {
                frame = (frame + 1) % frames.size();
                subFrame = 0;

                final int nextIndex = frames.get(frame).index();
                if (nextIndex != current.index()) {
                    uploadFrame(nextIndex);
                }

            }
            else if (interpolatedFrame != null) {
                uploadInterpolatedFrame(current, interpolatedFrame);
            }

        }

        private void uploadFrame(int index) {
            sheet.upload(0, 0, 0, frameX(index), frameY(index), frameWidth, frameHeight, false, false);
        }

        private void uploadInterpolatedFrame(Frame current, NativeImage target) {
            final int nextIndex = frames.get((frame + 1) % frames.size()).index();
            if (nextIndex == current.index()) {
                return;
            }

            final double delta = 1.0D - (double) subFrame / current.time();
            final int fromX = frameX(current.index());
            final int fromY = frameY(current.index());
            final int toX = frameX(nextIndex);
            final int toY = frameY(nextIndex);

            for (int y = 0; y < frameHeight; y++) {
                for (int x = 0; x < frameWidth; x++) {
                    final int from = sheet.getPixelRGBA(fromX + x, fromY + y);
                    final int to = sheet.getPixelRGBA(toX + x, toY + y);
                    final int c0 = mix(delta, from >> 16 & 255, to >> 16 & 255);
                    final int c1 = mix(delta, from >> 8 & 255, to >> 8 & 255);
                    final int c2 = mix(delta, from & 255, to & 255);

                    target.setPixelRGBA(x, y, from & 0xFF000000 | c0 << 16 | c1 << 8 | c2);
                }

            }

            target.upload(0, 0, 0, 0, 0, frameWidth, frameHeight, false, false);
        }

        private static int mix(double delta, int from, int to) {
            return (int) (delta * from + (1.0D - delta) * to);
        }

        private int frameX(int index) {
            return index % framesPerRow * frameWidth;
        }

        private int frameY(int index) {
            return index / framesPerRow * frameHeight;
        }

        @Override
        public void close() {
            sheet.close();
            if (interpolatedFrame != null) {
                interpolatedFrame.close();
            }

        }

        private record Frame(
                int index,
                int time
        ) {
            ;;
        }

    }

}