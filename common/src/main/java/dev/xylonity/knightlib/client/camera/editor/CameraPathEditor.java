package dev.xylonity.knightlib.client.camera.editor;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPathManager;
import dev.xylonity.knightlib.client.screen.camera.CameraEditorScreen;
import dev.xylonity.knightlib.mixin.CameraAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CameraPathEditor {

    @Nullable
    private static CameraEditorSession session;
    private static final EditorFreeCamera CAMERA = new EditorFreeCamera();

    private static boolean active = false;
    private static boolean previewing = false;

    private CameraPathEditor() {
        ;;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isPreviewing() {
        return previewing;
    }

    public static CameraEditorSession session() {
        if (session == null) {
            session = new CameraEditorSession();
        }

        return session;
    }

    public static EditorFreeCamera freeCamera() {
        return CAMERA;
    }

    public static void toggle() {
        if (active) {
            close();
        }
        else {
            open();
        }

    }

    /**
     * Enters the editor
     */
    public static void open() {
        // The campath is a dev-only system, in real deploy it's disabled
        if (!KnightLib.PLATFORM.isDevelopmentEnvironment()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (!minecraft.player.hasPermissions(2)) {
            minecraft.player.displayClientMessage(Component.literal("[KnightLib] The camera editor requires OP level 2").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!active) {
            active = true;

            Camera camera = minecraft.gameRenderer.getMainCamera();
            CAMERA.reset(camera.getPosition(), camera.getYRot(), camera.getXRot());
        }

        minecraft.setScreen(new CameraEditorScreen());
    }

    /**
     * Leaves the editor
     */
    public static void close() {
        final boolean wasPreviewing = previewing;

        active = false;
        previewing = false;

        if (wasPreviewing) {
            CameraPathManager.stop();
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CameraEditorScreen) {
            minecraft.setScreen(null);
        }

    }

    /**
     * Called by the editor screen when it is closed by the user (pressing esc)
     */
    public static void onScreenClosed() {
        if (active && !previewing) {
            active = false;
        }

    }

    /**
     * Plays the current session as a real path
     */
    public static void preview() {
        final Minecraft minecraft = Minecraft.getInstance();
        final CameraEditorSession current = session();

        if (current.isEmpty()) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("[KnightLib] No keyframes to preview yet"), true);
            }

            return;
        }

        previewing = true;
        if (minecraft.screen instanceof CameraEditorScreen) {
            minecraft.setScreen(null);
        }

        CameraPathManager.play(current.buildPath(), CameraPathEditor::onPreviewFinished);
    }

    /**
     * Preview keybind entrypoint
     */
    public static void togglePreview() {
        if (CameraPathManager.isPlaying()) {
            CameraPathManager.stop();
            onPreviewFinished();
            return;
        }

        preview();
    }

    private static void onPreviewFinished() {
        final boolean reopen = active && previewing;
        previewing = false;

        if (reopen) {
            Minecraft.getInstance().setScreen(new CameraEditorScreen());
        }

    }

    /**
     * Overrides the camera transform with the freecam, unless a path owns the camera
     */
    public static boolean applyCamera(Camera camera, float partialTick) {
        if (!active || previewing || CameraPathManager.isPlaying()) {
            return false;
        }

        final Vec3 position = CAMERA.position(partialTick);

        final CameraAccessor accessor = (CameraAccessor) camera;
        accessor.setRotationAccessor(CAMERA.yaw(), CAMERA.pitch());
        accessor.setPositionAccessor(position.x, position.y, position.z);

        // Keeps the local player visible while the camera flies around
        accessor.setDetachedAccessor(true);

        return true;
    }

    public static void tick() {
        if (active && !previewing) {
            CAMERA.tick();
        }

    }

    /**
     * Drops everything
     */
    public static void discard() {
        active = false;
        previewing = false;
        session = null;
    }

}
