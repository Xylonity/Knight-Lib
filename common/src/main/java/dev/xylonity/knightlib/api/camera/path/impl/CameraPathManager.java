package dev.xylonity.knightlib.api.camera.path.impl;

import dev.xylonity.knightlib.client.camera.path.CameraPathPlayback;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Client-sided camera path player, no real reason to be called directly as the helper packet exists.
 */
public final class CameraPathManager {

    @Nullable
    private static CameraPathPlayback active;

    private CameraPathManager() {
        ;;
    }

    public static void play(CameraPath path) {
        play(path, null);
    }

    /**
     * Starts a path, replacing any active one. The completion callback only runs if the path finishes on its own
     */
    public static void play(CameraPath path, @Nullable Runnable onComplete) {
        if (path == null || path.keyframes().isEmpty()) {
            return;
        }

        cancelActive();
        active = new CameraPathPlayback(path, onComplete);
    }

    public static void stop() {
        cancelActive();
    }

    public static boolean isPlaying() {
        return active != null;
    }

    /**
     * Client tick hook (internal)
     */
    public static void tick() {
        final CameraPathPlayback instance = active;
        if (instance != null) {
            instance.tickEffects();
        }

    }

    public static void clearAll() {
        cancelActive();
    }

    public static void applyIfPresent(Player player, Camera camera, float partialTick) {
        final CameraPathPlayback instance = active;
        if (instance == null || player == null || camera == null) {
            return;
        }

        if (!instance.apply(player, camera, partialTick)) {
            finishActive();
        }

    }

    /**
     * Draws renderable overlays
     */
    public static void renderOverlay(GuiGraphics graphics, float partialTick) {
        final CameraPathPlayback instance = active;
        if (instance != null) {
            instance.renderOverlay(graphics, partialTick);
        }

    }

    private static void finishActive() {
        final CameraPathPlayback instance = active;
        active = null;

        if (instance != null) {
            instance.restore();
            instance.runCompletion();
        }

    }

    private static void cancelActive() {
        final CameraPathPlayback instance = active;
        active = null;

        if (instance != null) {
            instance.restore();
        }

    }

}