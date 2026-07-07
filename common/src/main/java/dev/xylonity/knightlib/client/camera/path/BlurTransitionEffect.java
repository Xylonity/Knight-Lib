package dev.xylonity.knightlib.client.camera.path;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPath;
import dev.xylonity.knightlib.mixin.GameRendererAccessor;
import dev.xylonity.knightlib.mixin.PostChainAccessor;
import dev.xylonity.knightlib.mixin.PostPassAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

/**
 * Blur post chain wrapper for blur transitions (using a derivative post shader based on the blur from 1.21.1's background blur shader)
 */
public final class BlurTransitionEffect {

    private static final ResourceLocation BLUR_POST_CHAIN = KnightLib.of("shaders/post/camera_blur.json");

    private boolean ownsChain = false;
    private float pendingIntensity = 0f;
    private float maxRadius = CameraPath.DEFAULT_BLUR_RADIUS;

    /**
     * Radius (in pixels) reached at full intensity
     */
    public void setMaxRadius(float radius) {
        this.maxRadius = Math.max(0f, radius);
    }

    public void update(float intensity) {
        pendingIntensity = intensity;

        if (ownsChain) {
            setRadius(Math.max(1f, intensity * maxRadius));
        }

    }

    public void tick() {
        final Minecraft minecraft = Minecraft.getInstance();

        final boolean shouldBlur = pendingIntensity > 0.01f;
        if (shouldBlur && !ownsChain && minecraft.gameRenderer.currentEffect() == null) {
            ((GameRendererAccessor) minecraft.gameRenderer).loadEffectAccessor(BLUR_POST_CHAIN);
            ownsChain = true;

            setRadius(Math.max(1f, pendingIntensity * maxRadius));
        }
        else if (!shouldBlur && ownsChain) {
            close();
        }

    }

    public void close() {
        if (!ownsChain) {
            return;
        }

        ownsChain = false;
        Minecraft.getInstance().tell(BlurTransitionEffect::shutdownIfOwned);
    }

    private static void shutdownIfOwned() {
        final Minecraft minecraft = Minecraft.getInstance();
        final PostChain chain = minecraft.gameRenderer.currentEffect();
        if (chain != null && BLUR_POST_CHAIN.toString().equals(chain.getName())) {
            minecraft.gameRenderer.shutdownEffect();
        }

    }

    private void setRadius(float radius) {
        final PostChain chain = Minecraft.getInstance().gameRenderer.currentEffect();
        if (chain == null) {
            return;
        }

        for (final PostPass pass : ((PostChainAccessor) chain).knightlib$getPasses()) {
            ((PostPassAccessor) pass).knightlib$getEffect().safeGetUniform("Radius").set(radius);
        }

    }

}