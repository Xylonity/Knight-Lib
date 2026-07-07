package dev.xylonity.knightlib.client.camera.path;

import dev.xylonity.knightlib.api.camera.path.impl.CameraEffect;
import dev.xylonity.knightlib.api.camera.path.impl.CameraKeyframe;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPath;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPathSampler;
import dev.xylonity.knightlib.api.camera.path.impl.EffectKeyframe;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import dev.xylonity.knightlib.mixin.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One active camera path playback, where the first sampled frame is captured and builds the shared sampler
 */
public final class CameraPathPlayback {

    private static final long UNSTARTED_TICK = Long.MIN_VALUE;

    private final CameraPath path;
    @Nullable
    private final Runnable onComplete;

    private long startTick = UNSTARTED_TICK;
    private ResourceKey<Level> dimension;

    private CameraPathSampler sampler;

    private final BlurTransitionEffect blur = new BlurTransitionEffect();

    private boolean guiHidden = false;
    private boolean previousHideGui = false;

    public CameraPathPlayback(CameraPath path, @Nullable Runnable onComplete) {
        this.path = path;
        this.onComplete = onComplete;
    }

    /**
     * Overrides the camera transform for this frame.
     * Returns false if the animation is over.
     */
    public boolean apply(Player player, Camera camera, float partialTick) {
        final Level level = player.level();
        ensureStarted(level, camera);

        final float ticks = (level.getGameTime() - startTick) + partialTick;
        if (level.dimension() != dimension) {
            return false;
        }

        if (ticks >= sampler.totalTicks()) {
            return applyReturn(camera, ticks - sampler.totalTicks());
        }

        blur.update(intensityOf(CameraEffect.BLUR, ticks));

        final CameraPathSampler.Pose pose = sampler.sample(ticks);

        final CameraAccessor accessor = (CameraAccessor) camera;
        accessor.setRotationAccessor(pose.yaw(), pose.pitch());
        accessor.setPositionAccessor(pose.position().x, pose.position().y, pose.position().z);

        // Keeps the local player visible while the cutscene is playing (otherwise the player will be completely invisible)
        accessor.setDetachedAccessor(true);

        return true;
    }

    /**
     * Blends the camera from the last keyframe back to the player's own camera once the path ends
     */
    private boolean applyReturn(Camera camera, float ticksPastEnd) {
        if (path.returnTicks() <= 0 || ticksPastEnd >= path.returnTicks()) {
            return false;
        }

        blur.update(0f);

        final float time = path.returnEasing().apply(ticksPastEnd / path.returnTicks());
        final CameraPathSampler.Pose pose = sampler.sample(sampler.totalTicks());

        final Vec3 position = pose.position().lerp(camera.getPosition(), time);

        final CameraAccessor accessor = (CameraAccessor) camera;
        accessor.setRotationAccessor(Mth.rotLerp(time, pose.yaw(), camera.getYRot()), Mth.lerp(time, pose.pitch(), camera.getXRot()));
        accessor.setPositionAccessor(position.x, position.y, position.z);
        accessor.setDetachedAccessor(true);

        return true;
    }

    /**
     * Draws the overlays
     */
    public void renderOverlay(GuiGraphics graphics, float partialTick) {
        if (startTick == UNSTARTED_TICK) {
            return;
        }

        final Level level = Minecraft.getInstance().level;
        if (level == null || level.dimension() != dimension) {
            return;
        }

        final float ticks = (level.getGameTime() - startTick) + partialTick;

        final float bars = path.letterbox() ? LetterboxOverlay.pathIntensity(ticks, sampler.totalTicks()) : 0f;
        LetterboxOverlay.render(graphics, Math.max(bars, intensityOf(CameraEffect.LETTERBOX, ticks)));

        FadeOverlay.render(graphics, intensityOf(CameraEffect.FADE, ticks));
    }

    public void tickEffects() {
        blur.tick();
    }

    public void restore() {
        if (guiHidden) {
            Minecraft.getInstance().options.hideGui = previousHideGui;
            guiHidden = false;
        }

        blur.close();
    }

    public void runCompletion() {
        if (onComplete != null) {
            onComplete.run();
        }

    }

    private void ensureStarted(Level level, Camera camera) {
        if (startTick != UNSTARTED_TICK) {
            return;
        }

        startTick = level.getGameTime();
        dimension = level.dimension();

        final List<CameraKeyframe> points = new ArrayList<>(path.keyframes().size() + 1);
        if (path.fromCurrentCamera()) {
            points.add(new CameraKeyframe(camera.getPosition(), camera.getYRot(), camera.getXRot(), 0, KnightLibEasings.LINEAR));
        }

        points.addAll(path.keyframes());

        sampler = CameraPathSampler.of(points, path.smoothPosition(), path.holdTicks());
        blur.setMaxRadius(path.blurRadius());

        // Cutscenes hides the hud
        previousHideGui = Minecraft.getInstance().options.hideGui;
        Minecraft.getInstance().options.hideGui = true;
        guiHidden = true;

    }

    private float intensityOf(CameraEffect effect, float ticks) {
        float intensity = 0f;
        for (final EffectKeyframe window : path.effects()) {
            if (window.effect() == effect) {
                intensity = Math.max(intensity, window.intensityAt(ticks));
            }

        }

        return intensity;
    }

}