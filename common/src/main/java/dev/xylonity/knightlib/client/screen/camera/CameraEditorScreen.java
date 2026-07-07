package dev.xylonity.knightlib.client.screen.camera;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xylonity.knightlib.api.camera.path.impl.CameraEffect;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPathSampler;
import dev.xylonity.knightlib.api.camera.path.impl.EffectMode;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import dev.xylonity.knightlib.api.util.KnightLibMath;
import dev.xylonity.knightlib.client.camera.editor.CameraEditorSession;
import dev.xylonity.knightlib.client.camera.editor.CameraPathEditor;
import dev.xylonity.knightlib.client.camera.editor.EditorEffect;
import dev.xylonity.knightlib.client.camera.editor.EditorFreeCamera;
import dev.xylonity.knightlib.client.camera.editor.EditorKeyframe;
import dev.xylonity.knightlib.client.camera.editor.EditorPathRenderer;
import dev.xylonity.knightlib.client.event.KnightLibClientEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The campath editor screen
 */
public class CameraEditorScreen extends Screen {

    private static final int COLOR_PANEL_TOP = 0xE0141826;
    private static final int COLOR_PANEL_BOTTOM = 0xEE0A0C12;
    private static final int COLOR_BORDER = 0xFF2E3442;
    private static final int COLOR_EDGE_ACCENT = 0x59E8C04A;
    private static final int COLOR_SHADOW = 0x50000000;
    private static final int COLOR_SECTION = 0xFF8A93A0;
    private static final int COLOR_ACCENT = 0xFFE8C04A;
    private static final int COLOR_ACCENT_GLOW = 0x48E8C04A;
    private static final int COLOR_CURVE_BG = 0xE0090A0F;
    private static final int COLOR_CURVE_GRID = 0x1CFFFFFF;
    private static final int COLOR_CURVE_FILL = 0x2EE8C04A;
    private static final int COLOR_SEPARATOR = 0xFF252B38;
    private static final int COLOR_HINT_KEY = 0xFFD9DDE7;
    private static final int COLOR_HINT_DOT = 0xFF4C5464;

    private static final int COLOR_TIMELINE_RULER = 0xFF6A7284;
    private static final int COLOR_TIMELINE_RULER_MINOR = 0xFF3A404E;
    private static final int COLOR_TIMELINE_GRID = 0x10FFFFFF;
    private static final int COLOR_TIMELINE_HOLD = 0x26FFFFFF;
    private static final int COLOR_TIMELINE_HEADROOM = 0x5E000000;
    private static final int COLOR_TIMELINE_PLAYHEAD = 0xFF59FFC0;
    private static final int COLOR_TIMELINE_PLAYHEAD_GLOW = 0x2459FFC0;
    private static final int COLOR_END_MARKER = 0xFFFF9040;
    private static final int COLOR_END_MARKER_GLOW = 0x24FF9040;
    private static final int COLOR_TRACK_ALT = 0x12FFFFFF;
    private static final int COLOR_CHAIN = 0xFF59FFC0;
    private static final int COLOR_KEY_DIAMOND = 0xFFDDE2EC;
    private static final int COLOR_KEY_DIAMOND_SELECTED = 0xFFE8C04A;
    private static final int COLOR_KEY_DIAMOND_HOVER = 0xFFFFFFFF;
    private static final int COLOR_KEY_DIAMOND_HOVER_GLOW = 0x2EFFFFFF;

    private static final int TOOLBAR_HEIGHT = 24;
    private static final int WIDGET_HEIGHT = 16;
    private static final int RULER_HEIGHT = 12;
    private static final int CAM_TRACK_HEIGHT = 18;
    private static final int EFFECT_TRACK_HEIGHT = 12;
    private static final int LABEL_WIDTH = 30;
    private static final int DIAMOND_RADIUS = 4;

    // Mapped range past the end flag, so it can always be dragged further right to extend the path
    private static final float TIMELINE_HEADROOM = 1.15f;

    private static final KnightLibEasings[] EASINGS = KnightLibEasings.values();
    private static final CameraEffect[] EFFECT_TRACKS = { CameraEffect.FADE, CameraEffect.BLUR, CameraEffect.LETTERBOX };
    private static final String[] EFFECT_TRACK_LABELS = { "FADE", "BLUR", "BARS" };
    private static final int[] EFFECT_TRACK_LABEL_COLORS = { 0xFFA9AFBD, 0xFF6E93D6, 0xFF9A74D8 };

    private final CameraEditorSession session;

    // Timeline
    private int blockLeft;
    private int blockRight;
    private int blockTop;
    private int blockBottom;
    private int stripX;
    private int stripWidth;
    private int toolbarY;
    private int rulerY;
    private int camTrackY;
    private int effectTracksY;
    private int inspectorTop;

    private int curveX;
    private int curveY;
    private int curveWidth;
    private int curveHeight;

    // Timeline drag state
    private boolean draggingPlayhead = false;
    private boolean draggingEnd = false;
    private int draggingKeyIndex = -1;
    private float frozenTotal = 0f;

    private enum EffectDrag { MOVE, START, END }

    @Nullable
    private EditorEffect draggingEffect;
    private EffectDrag effectDragMode = EffectDrag.MOVE;
    private float effectGrabOffset = 0f;

    // Double click detection on effect tracks
    private long lastClickMillis = 0;
    private int lastClickTrack = -1;
    private double lastClickX = 0;

    // Viewport drag state
    @Nullable
    private EditorPathRenderer.PickKind dragKind;
    private int dragIndex;
    private Vec3 dragPlanePoint = Vec3.ZERO;
    private Vec3 dragPlaneNormal = Vec3.ZERO;
    private Vec3 dragOffset = Vec3.ZERO;

    // Gizmo drag state
    private Vec3 dragAxis = Vec3.ZERO;
    private Vec3 dragStartPosition = Vec3.ZERO;
    private double dragGrabScalar = 0.0;
    private float dragStartAngle = 0f;
    private float dragStartYaw = 0f;
    private float dragStartPitch = 0f;

    // RMB
    private boolean looking = false;
    private double lookStartX;
    private double lookStartY;

    // Controls card collapse state
    private static boolean hintsCollapsed = false;
    private int hintsPanelWidth;
    private int hintsPanelHeight;

    private boolean keyForward;
    private boolean keyBack;
    private boolean keyLeft;
    private boolean keyRight;
    private boolean keyUp;
    private boolean keyDown;

    // Thin vertical lines between toolbar groups
    private final List<Integer> toolbarSeparators = new ArrayList<>();

    public CameraEditorScreen() {
        super(Component.literal("Camera Path Editor"));
        this.session = CameraPathEditor.session();
    }

    @Override
    protected void init() {
        blockLeft = 6;
        blockRight = width - 6;
        blockBottom = height - 6;
        blockTop = blockBottom - (TOOLBAR_HEIGHT + RULER_HEIGHT + CAM_TRACK_HEIGHT + EFFECT_TRACK_HEIGHT * EFFECT_TRACKS.length + 4);

        stripX = blockLeft + LABEL_WIDTH + 4;
        stripWidth = blockRight - 6 - stripX;

        toolbarY = blockTop + 4;
        rulerY = toolbarY + WIDGET_HEIGHT + 4;
        camTrackY = rulerY + RULER_HEIGHT;
        effectTracksY = camTrackY + CAM_TRACK_HEIGHT;

        initToolbar();
        initInspector();
    }

    private void initToolbar() {
        toolbarSeparators.clear();

        int x = blockLeft + 6;
        final int y = toolbarY;

        addRenderableWidget(new CameraEditorWidgets.FlatButton(x, y, 44, WIDGET_HEIGHT, Component.literal("+ Key"), CameraEditorWidgets.Style.NORMAL, this::addKeyframeAtView));
        x += 48;

        addRenderableWidget(new CameraEditorWidgets.FlatButton(x, y, 44, WIDGET_HEIGHT, Component.literal("▶ Play"), CameraEditorWidgets.Style.PRIMARY, CameraPathEditor::preview));
        x += 48;

        addRenderableWidget(new CameraEditorWidgets.FlatButton(x, y, 48, WIDGET_HEIGHT, Component.literal("Export"), CameraEditorWidgets.Style.NORMAL, this::exportCode));
        x += 54;

        toolbarSeparators.add(x);
        x += 7;

        // Smooth curve between keyframes
        x = addToolbarToggle(x, "Curve", session.smoothPosition, value -> {
            session.smoothPosition = value;
            session.markDirty();
        });

        // Smooth blend in from the player camera when the path starts
        x = addToolbarToggle(x, "Cam", session.fromCurrentCamera, value -> session.fromCurrentCamera = value);

        // Letterbox
        x = addToolbarToggle(x, "Bars", session.letterbox, value -> session.letterbox = value);

        // Smooth blend back to the player camera when the path ends
        addToolbarToggle(x, "Ret", session.returnToPlayer, value -> session.returnToPlayer = value);

        // Destructive
        addRenderableWidget(new CameraEditorWidgets.FlatButton(blockRight - 6 - 18, y, 18, WIDGET_HEIGHT, Component.literal("×"), CameraEditorWidgets.Style.DANGER, CameraPathEditor::close));
        addRenderableWidget(new CameraEditorWidgets.FlatButton(blockRight - 6 - 18 - 4 - 42, y, 42, WIDGET_HEIGHT, Component.literal("Clear"), CameraEditorWidgets.Style.DANGER, () -> {
            session.clear();
            rebuildWidgets();
        }));
    }

    private int addToolbarToggle(int x, String label, boolean initial, Consumer<Boolean> onChange) {
        final int width = CameraEditorWidgets.FlatToggle.widthFor(font, label);
        addRenderableWidget(new CameraEditorWidgets.FlatToggle(x, toolbarY, width, WIDGET_HEIGHT, Component.literal(label), initial, onChange));

        return x + width + 4;
    }

    /**
     * The context above the timeline
     */
    private void initInspector() {
        curveWidth = 0;

        final EditorKeyframe keyframe = session.selectedKeyframe();
        final EditorEffect effect = session.selectedEffect();

        if (keyframe != null) {
            final int rowTwoY = blockTop - 22;
            final int rowOneY = rowTwoY - 20;
            inspectorTop = rowOneY - 6;

            int x = stripX;

            // Row 1
            addRenderableWidget(new CameraEditorWidgets.FlatButton(x, rowOneY, 14, WIDGET_HEIGHT, Component.literal("<"), CameraEditorWidgets.Style.NORMAL, () -> cycleEasing(-1)));
            curveX = x + 16;
            curveY = rowOneY;
            curveWidth = 56;
            curveHeight = WIDGET_HEIGHT;
            addRenderableWidget(new CameraEditorWidgets.FlatButton(x + 74, rowOneY, 14, WIDGET_HEIGHT, Component.literal(">"), CameraEditorWidgets.Style.NORMAL, () -> cycleEasing(1)));
            x += 92;

            addRenderableWidget(new CameraEditorWidgets.FlatSlider(x, rowOneY, 92, WIDGET_HEIGHT, "Travel", 0, 200, keyframe.durationTicks, value -> {
                keyframe.durationTicks = value;
                session.markDirty();
            }, " (0 = cut)"));
            x += 96;

            final CameraEditorWidgets.FlatToggle chain = new CameraEditorWidgets.FlatToggle(x, rowOneY, CameraEditorWidgets.FlatToggle.widthFor(font, "Chain"), WIDGET_HEIGHT, Component.literal("Chain"), keyframe.chained, value -> {
                keyframe.chained = value;
                session.markDirty();
            });
            chain.active = session.selectedIndex() >= 2;
            addRenderableWidget(chain);

            // Row 2
            x = stripX;

            addRenderableWidget(new CameraEditorWidgets.FlatButton(x, rowTwoY, 50, WIDGET_HEIGHT, Component.literal("Set view"), CameraEditorWidgets.Style.NORMAL, () -> {
                keyframe.position = freeCamera().position();
                keyframe.yaw = freeCamera().yaw();
                keyframe.pitch = freeCamera().pitch();
                session.markDirty();
            }));
            x += 52;

            addRenderableWidget(new CameraEditorWidgets.FlatButton(x, rowTwoY, 36, WIDGET_HEIGHT, Component.literal("Go to"), CameraEditorWidgets.Style.NORMAL, () -> {
                freeCamera().moveTo(keyframe.position.add(keyframe.position.subtract(freeCamera().position()).normalize().scale(-2.5)));
                freeCamera().setRotation(keyframe.yaw, keyframe.pitch);
            }));
            x += 38;

            addRenderableWidget(new CameraEditorWidgets.FlatButton(x, rowTwoY, 16, WIDGET_HEIGHT, Component.literal("«"), CameraEditorWidgets.Style.NORMAL, () -> {
                session.moveSelected(-1);
                rebuildWidgets();
            }));
            x += 18;
            addRenderableWidget(new CameraEditorWidgets.FlatButton(x, rowTwoY, 16, WIDGET_HEIGHT, Component.literal("»"), CameraEditorWidgets.Style.NORMAL, () -> {
                session.moveSelected(1);
                rebuildWidgets();
            }));
            x += 20;

            addRenderableWidget(new CameraEditorWidgets.FlatButton(x, rowTwoY, 62, WIDGET_HEIGHT, Component.literal("Reset tang."), CameraEditorWidgets.Style.NORMAL, () -> {
                keyframe.resetTangents();
                session.markDirty();
            }));
            x += 64;

            addRenderableWidget(new CameraEditorWidgets.FlatToggle(x, rowTwoY, CameraEditorWidgets.FlatToggle.widthFor(font, "Link"), WIDGET_HEIGHT, Component.literal("Link"), keyframe.linkedHandles, value -> keyframe.linkedHandles = value));
            x += CameraEditorWidgets.FlatToggle.widthFor(font, "Link") + 4;

            addRenderableWidget(new CameraEditorWidgets.FlatButton(x, rowTwoY, 44, WIDGET_HEIGHT, Component.literal("Delete"), CameraEditorWidgets.Style.DANGER, () -> {
                session.removeSelected();
                rebuildWidgets();
            }));
        }
        else if (effect != null) {
            final int rowY = blockTop - 22;
            inspectorTop = rowY - 6;

            int x = stripX;

            // Cycles through the effect window modes (in/out/full/hold)
            addRenderableWidget(new CameraEditorWidgets.FlatButton(x, rowY, 120, WIDGET_HEIGHT, Component.literal(modeLabel(effect.effect, effect.mode)), CameraEditorWidgets.Style.NORMAL, () -> {
                effect.mode = EffectMode.values()[(effect.mode.ordinal() + 1) % EffectMode.values().length];
                rebuildWidgets();
            }));
            x += 124;

            addRenderableWidget(new CameraEditorWidgets.FlatButton(x, rowY, 44, WIDGET_HEIGHT, Component.literal("Delete"), CameraEditorWidgets.Style.DANGER, () -> {
                session.removeSelected();
                rebuildWidgets();
            }));
        }
        else {
            inspectorTop = blockTop;
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Widgets win first
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Clicking the controls card collapses/expands it
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseX >= 6 && mouseX < 6 + hintsPanelWidth && mouseY >= 6 && mouseY < 6 + hintsPanelHeight) {
            hintsCollapsed = !hintsCollapsed;
            return true;
        }

        if (mouseY >= blockTop) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                timelineClicked(mouseX, mouseY);
            }
            else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                // Right click deletes the effect window under the cursor
                final EditorEffect hit = effectWindowAt(mouseX, mouseY);
                if (hit != null) {
                    session.effects().remove(hit);
                    if (session.selectedEffect() == hit) {
                        session.selectEffect(null);
                        rebuildWidgets();
                    }

                }

            }

            return true;
        }

        // Dead space of the inspector panel
        if (mouseY >= inspectorTop) {
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            final EditorPathRenderer.PickResult result = EditorPathRenderer.pick(session, mouseX, mouseY);
            if (result != null) {
                // First click on a camera only selects it
                if (result.kind() == EditorPathRenderer.PickKind.KEYFRAME && result.index() != session.selectedIndex()) {
                    session.select(result.index());
                    rebuildWidgets();
                    return true;
                }

                beginDrag(result, mouseX, mouseY);
            }

            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            beginLook();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (looking && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            double scale = minecraft.getWindow().getGuiScale();
            freeCamera().look(dragX * scale, dragY * scale);
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingKeyIndex >= 0) {
            retimeKeyframe(draggingKeyIndex, xToTime(mouseX), hasShiftDown());
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingEnd) {
            session.setEndTicks(Math.round(xToTime(mouseX)));
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingEffect != null) {
            dragEffect(mouseX);
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingPlayhead) {
            session.scrubTicks = Math.min(xToTime(mouseX), session.endTicks());
            return true;
        }

        if (dragKind != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isGizmoDrag()) {
                dragGizmo(mouseX, mouseY);
                return true;
            }

            final EditorPathRenderer.PickRay ray = EditorPathRenderer.pickRay(mouseX, mouseY);
            if (ray != null) {
                final Vec3 hit = EditorPathRenderer.intersectPlane(ray, dragPlanePoint, dragPlaneNormal);
                if (hit != null) {
                    applyDrag(hit.add(dragOffset));
                }

            }

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && looking) {
            endLook();
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && (draggingKeyIndex >= 0 || draggingEnd)) {
            draggingKeyIndex = -1;
            draggingEnd = false;
            frozenTotal = 0f;

            rebuildWidgets();
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingEffect != null) {
            draggingEffect = null;
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingPlayhead) {
            draggingPlayhead = false;
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragKind != null) {
            dragKind = null;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (dragKind != null && isGizmoDrag()) {
            return true;
        }

        if (dragKind != null) {
            final double step = Math.max(0.25, dragPlanePoint.distanceTo(freeCamera().position()) * 0.06);
            final Vec3 shift = dragPlaneNormal.scale(-delta * step);

            final Vec3 current = currentDragPosition();
            if (current != null) {
                dragPlanePoint = dragPlanePoint.add(shift);
                dragOffset = dragOffset.add(shift);
                applyDrag(current.add(shift));
            }

            return true;
        }

        if (mouseY >= blockTop) {
            // Scrolling over the ruler moves the end flag (5 ticks)
            if (mouseY >= rulerY && mouseY < camTrackY && !session.isEmpty()) {
                session.setEndTicks(session.endTicks() + (int) Math.signum(delta) * 5);
            }

            return true;
        }

        if (!looking) {
            // Scrolling over a tangent handle diamond expands/shrinks that tangent in place
            final EditorPathRenderer.PickResult hover = EditorPathRenderer.pick(session, mouseX, mouseY);
            if (hover != null && (hover.kind() == EditorPathRenderer.PickKind.HANDLE_IN || hover.kind() == EditorPathRenderer.PickKind.HANDLE_OUT)) {
                scaleHoveredTangent(hover, delta > 0 ? 1.15 : 1 / 1.15);
                return true;
            }

        }

        freeCamera().adjustSpeed((int) Math.signum(delta));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KnightLibClientEvents.campathEditorKey() != null && KnightLibClientEvents.campathEditorKey().matches(keyCode, scanCode)) {
            CameraPathEditor.close();
            return true;
        }

        if (KnightLibClientEvents.campathPreviewKey() != null && KnightLibClientEvents.campathPreviewKey().matches(keyCode, scanCode)) {
            CameraPathEditor.preview();
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_W -> {
                keyForward = true;
                return true;
            }
            case GLFW.GLFW_KEY_S -> {
                keyBack = true;
                return true;
            }
            case GLFW.GLFW_KEY_A -> {
                keyLeft = true;
                return true;
            }
            case GLFW.GLFW_KEY_D -> {
                keyRight = true;
                return true;
            }
            case GLFW.GLFW_KEY_SPACE -> {
                keyUp = true;
                return true;
            }
            case GLFW.GLFW_KEY_LEFT_SHIFT -> {
                keyDown = true;
                return true;
            }
            case GLFW.GLFW_KEY_F -> {
                addKeyframeAtView();
                return true;
            }
            case GLFW.GLFW_KEY_C -> {
                final EditorKeyframe keyframe = session.selectedKeyframe();
                if (keyframe != null && session.selectedIndex() >= 2) {
                    keyframe.chained = !keyframe.chained;
                    session.markDirty();
                    rebuildWidgets();
                }

                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                session.removeSelected();
                rebuildWidgets();
                return true;
            }

        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_W -> {
                keyForward = false;
                return true;
            }
            case GLFW.GLFW_KEY_S -> {
                keyBack = false;
                return true;
            }
            case GLFW.GLFW_KEY_A -> {
                keyLeft = false;
                return true;
            }
            case GLFW.GLFW_KEY_D -> {
                keyRight = false;
                return true;
            }
            case GLFW.GLFW_KEY_SPACE -> {
                keyUp = false;
                return true;
            }
            case GLFW.GLFW_KEY_LEFT_SHIFT -> {
                keyDown = false;
                return true;
            }

        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        freeCamera().setInput(keyForward, keyBack, keyLeft, keyRight, keyUp, keyDown, hasControlDown());
    }

    private void beginDrag(EditorPathRenderer.PickResult result, double mouseX, double mouseY) {
        dragKind = result.kind();
        dragIndex = result.index();
        dragPlanePoint = result.position();
        dragPlaneNormal = freeCamera().lookDirection().scale(-1);
        dragOffset = Vec3.ZERO;

        final EditorPathRenderer.PickRay ray = EditorPathRenderer.pickRay(mouseX, mouseY);
        if (ray == null) {
            dragKind = null;
            return;
        }

        final EditorKeyframe keyframe = (dragIndex >= 0 && dragIndex < session.keyframeCount()) ? session.keyframes().get(dragIndex) : null;

        switch (result.kind()) {
            case AXIS_X, AXIS_Y, AXIS_Z -> {
                if (keyframe == null) {
                    dragKind = null;
                    return;
                }

                dragAxis = EditorPathRenderer.axisDirection(result.kind());
                dragStartPosition = keyframe.position;
                dragGrabScalar = EditorPathRenderer.axisParameter(ray, dragStartPosition, dragAxis);
            }
            case RING_YAW -> {
                final Vec3 hit = keyframe == null ? null : EditorPathRenderer.intersectPlane(ray, keyframe.position, new Vec3(0, 1, 0));
                if (hit == null) {
                    dragKind = null;
                    return;
                }

                dragStartYaw = keyframe.yaw;
                dragStartAngle = KnightLibMath.yawAngleOf(hit.subtract(keyframe.position));
            }
            case RING_PITCH -> {
                final Vec3 hit = keyframe == null ? null : EditorPathRenderer.intersectPlane(ray, keyframe.position, EditorPathRenderer.rightVector(keyframe.yaw));
                if (hit == null) {
                    dragKind = null;
                    return;
                }

                dragStartYaw = keyframe.yaw;
                dragStartPitch = keyframe.pitch;
                dragStartAngle = pitchAngleOf(hit.subtract(keyframe.position), keyframe.yaw);
            }
            default -> {
                final Vec3 hit = EditorPathRenderer.intersectPlane(ray, dragPlanePoint, dragPlaneNormal);
                if (hit != null) {
                    // Keeps the element exactly under the cursor
                    dragOffset = result.position().subtract(hit);
                }

            }

        }

    }

    private boolean isGizmoDrag() {
        return dragKind == EditorPathRenderer.PickKind.AXIS_X
                || dragKind == EditorPathRenderer.PickKind.AXIS_Y
                || dragKind == EditorPathRenderer.PickKind.AXIS_Z
                || dragKind == EditorPathRenderer.PickKind.RING_YAW
                || dragKind == EditorPathRenderer.PickKind.RING_PITCH;
    }

    /**
     * Drags the gizmo of the selected keyframe
     */
    private void dragGizmo(double mouseX, double mouseY) {
        final EditorPathRenderer.PickRay ray = EditorPathRenderer.pickRay(mouseX, mouseY);
        if (ray == null || dragIndex < 0 || dragIndex >= session.keyframeCount()) {
            return;
        }

        final EditorKeyframe keyframe = session.keyframes().get(dragIndex);

        switch (dragKind) {
            case AXIS_X, AXIS_Y, AXIS_Z -> {
                final double s = EditorPathRenderer.axisParameter(ray, dragStartPosition, dragAxis);
                keyframe.position = dragStartPosition.add(dragAxis.scale(s - dragGrabScalar));
            }
            case RING_YAW -> {
                final Vec3 hit = EditorPathRenderer.intersectPlane(ray, keyframe.position, new Vec3(0, 1, 0));
                if (hit != null) {
                    keyframe.yaw = Mth.wrapDegrees(dragStartYaw + KnightLibMath.yawAngleOf(hit.subtract(keyframe.position)) - dragStartAngle);
                }

            }
            case RING_PITCH -> {
                final Vec3 hit = EditorPathRenderer.intersectPlane(ray, keyframe.position, EditorPathRenderer.rightVector(dragStartYaw));
                if (hit != null) {
                    keyframe.pitch = Mth.clamp(dragStartPitch + pitchAngleOf(hit.subtract(keyframe.position), dragStartYaw) - dragStartAngle, -89.9f, 89.9f);
                }

            }
            default -> {
                ;;
            }

        }

        session.markDirty();
    }

    /**
     * Angle of an offset within the vertical pitch ring plane
     */
    private static float pitchAngleOf(Vec3 offset, float yaw) {
        final Vec3 forward = EditorPathRenderer.forwardVector(yaw);
        final double forwardComponent = offset.x * forward.x + offset.z * forward.z;

        return (float) Math.toDegrees(Math.atan2(-offset.y, forwardComponent));
    }

    private void applyDrag(Vec3 position) {
        if (dragKind == null || dragIndex < 0 || dragIndex >= session.keyframeCount()) {
            return;
        }

        final EditorKeyframe keyframe = session.keyframes().get(dragIndex);

        switch (dragKind) {
            case KEYFRAME -> keyframe.position = position;
            case HANDLE_OUT -> {
                if (hasAltDown()) {
                    keyframe.linkedHandles = false;
                }

                keyframe.dragOutHandle(position);
            }
            case HANDLE_IN -> {
                if (hasAltDown()) {
                    keyframe.linkedHandles = false;
                }

                keyframe.dragInHandle(position);
            }

        }

        session.markDirty();
    }

    @Nullable
    private Vec3 currentDragPosition() {
        if (dragKind == null || dragIndex < 0 || dragIndex >= session.keyframeCount()) {
            return null;
        }

        return switch (dragKind) {
            case KEYFRAME -> session.keyframes().get(dragIndex).position;
            case HANDLE_OUT -> EditorPathRenderer.outHandle(session, dragIndex);
            case HANDLE_IN -> EditorPathRenderer.inHandle(session, dragIndex);
            default -> null;
        };

    }

    /**
     * Expands/shrinks the hovered tangent handle
     */
    private void scaleHoveredTangent(EditorPathRenderer.PickResult hover, double factor) {
        final CameraPathSampler sampler = session.sampler();
        if (sampler == null || hover.index() < 0 || hover.index() >= session.keyframeCount()) {
            return;
        }

        final EditorKeyframe keyframe = session.keyframes().get(hover.index());

        if (hover.kind() == EditorPathRenderer.PickKind.HANDLE_OUT) {
            keyframe.scaleOutTangent(factor, sampler.outTangent(hover.index()));
        }
        else {
            keyframe.scaleInTangent(factor, sampler.inTangent(hover.index()));
        }

        session.markDirty();
    }

    private void beginLook() {
        looking = true;
        lookStartX = minecraft.mouseHandler.xpos();
        lookStartY = minecraft.mouseHandler.ypos();

        InputConstants.grabOrReleaseMouse(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR_DISABLED, lookStartX, lookStartY);
    }

    private void endLook() {
        looking = false;
        InputConstants.grabOrReleaseMouse(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR_NORMAL, lookStartX, lookStartY);
    }

    private void cycleEasing(int direction) {
        final EditorKeyframe keyframe = session.selectedKeyframe();
        if (keyframe != null) {
            keyframe.easing = EASINGS[Math.floorMod(keyframe.easing.ordinal() + direction, EASINGS.length)];
            session.markDirty();
        }

    }

    private void addKeyframeAtView() {
        session.addKeyframe(freeCamera().position(), freeCamera().yaw(), freeCamera().pitch());
        rebuildWidgets();
    }

    private void exportCode() {
        if (session.isEmpty()) {
            return;
        }

        final String code = session.toCode();
        minecraft.keyboardHandler.setClipboard(code);

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("[KnightLib] Current CamPath cutscene builder code copied to the clipboard").withStyle(ChatFormatting.GREEN), false);
        }

    }

    private EditorFreeCamera freeCamera() {
        return CameraPathEditor.freeCamera();
    }

    private void timelineClicked(double mouseX, double mouseY) {
        // Toolbar row
        if (mouseY < rulerY) {
            return;
        }

        // Ruler
        if (mouseY < camTrackY) {
            if (!session.isEmpty() && Math.abs(mouseX - timeToX(session.endTicks())) <= 4) {
                draggingEnd = true;
                frozenTotal = timelineTotal();
                return;
            }

            beginScrub(mouseX);
            return;
        }

        // Camera track
        if (mouseY < effectTracksY) {
            final int hovered = diamondAt(mouseX, mouseY);

            if (hovered >= 0) {
                if (hovered != session.selectedIndex()) {
                    session.select(hovered);
                    rebuildWidgets();
                }

                if (hovered > 0) {
                    draggingKeyIndex = hovered;
                    frozenTotal = timelineTotal();
                }

                return;
            }

            beginScrub(mouseX);
            return;
        }

        // Effect tracks
        final int track = effectTrackAt(mouseY);
        if (track < 0) {
            beginScrub(mouseX);
            return;
        }

        final EditorEffect hit = effectWindowAt(mouseX, mouseY);
        if (hit != null) {
            if (hit != session.selectedEffect()) {
                session.selectEffect(hit);
                rebuildWidgets();
            }

            beginEffectDrag(hit, mouseX);
            return;
        }

        final long now = Util.getMillis();
        final boolean doubleClick = now - lastClickMillis < 350 && track == lastClickTrack && Math.abs(mouseX - lastClickX) < 6;

        lastClickMillis = now;
        lastClickTrack = track;
        lastClickX = mouseX;

        if (doubleClick) {
            session.addEffect(EFFECT_TRACKS[track], Math.round(xToTime(mouseX)));
            rebuildWidgets();
        }
        else {
            beginScrub(mouseX);
        }

    }

    private void beginScrub(double mouseX) {
        draggingPlayhead = true;
        session.scrubTicks = Math.min(xToTime(mouseX), session.endTicks());
    }

    private void beginEffectDrag(EditorEffect effect, double mouseX) {
        draggingEffect = effect;

        final float startX = timeToX(effect.start);
        final float endX = timeToX(effect.end);

        if (mouseX <= startX + 3) {
            effectDragMode = EffectDrag.START;
        }
        else if (mouseX >= endX - 3) {
            effectDragMode = EffectDrag.END;
        }
        else {
            effectDragMode = EffectDrag.MOVE;
            effectGrabOffset = xToTime(mouseX) - effect.start;
        }

    }

    private void dragEffect(double mouseX) {
        final EditorEffect effect = draggingEffect;
        if (effect == null) {
            return;
        }

        final int end = session.endTicks();

        switch (effectDragMode) {
            case MOVE -> {
                final int length = effect.length();
                final int start = Mth.clamp(Math.round(xToTime(mouseX) - effectGrabOffset), 0, Math.max(0, end - length));

                effect.start = start;
                effect.end = start + length;
            }
            case START -> effect.start = Mth.clamp(Math.round(xToTime(mouseX)), 0, effect.end - 1);
            case END -> effect.end = Mth.clamp(Math.round(xToTime(mouseX)), effect.start + 1, Math.max(effect.start + 1, end));
        }

    }

    /**
     * Moves a keyframe to a new arrival time
     */
    private void retimeKeyframe(int index, float newTime, boolean ripple) {
        final CameraPathSampler sampler = session.sampler();
        if (sampler == null || index <= 0 || index >= session.keyframeCount()) {
            return;
        }

        final EditorKeyframe keyframe = session.keyframes().get(index);
        final float previousArrival = arrivalTime(sampler, index - 1);

        if (ripple || index == session.keyframeCount() - 1) {
            keyframe.durationTicks = Math.max(0, Math.round(newTime - previousArrival));
        }
        else {
            final float nextArrival = arrivalTime(sampler, index + 1);

            final int newDuration = Math.max(0, Math.round(Mth.clamp(newTime, previousArrival, nextArrival) - previousArrival));
            final int nextDuration = Math.max(0, Math.round(nextArrival - previousArrival) - newDuration);

            keyframe.durationTicks = newDuration;
            session.keyframes().get(index + 1).durationTicks = nextDuration;
        }

        session.markDirty();
    }

    /**
     * Path time at which the camera arrives at the given keyframe
     */
    private float arrivalTime(CameraPathSampler sampler, int index) {
        return index <= 0 ? 0f : sampler.segmentStart(index) + sampler.keyframe(index).durationTicks();
    }

    /**
     * The time range mapped onto the strip
     */
    private float timelineTotal() {
        if (frozenTotal > 0f) {
            return frozenTotal;
        }

        return Math.max(1f, session.endTicks()) * TIMELINE_HEADROOM;
    }

    private float xToTime(double mouseX) {
        final float total = timelineTotal();
        return Mth.clamp((float) ((mouseX - stripX) / stripWidth) * total, 0f, total);
    }

    private float timeToX(float ticks) {
        return stripX + (ticks / timelineTotal()) * stripWidth;
    }

    private int diamondX(CameraPathSampler sampler, int index) {
        return Mth.clamp(Math.round(timeToX(arrivalTime(sampler, index))), stripX, stripX + stripWidth);
    }

    private int diamondY(CameraPathSampler sampler, int index) {
        int stack = 0;
        for (int i = index; i > 0 && arrivalTime(sampler, i) - arrivalTime(sampler, i - 1) < 0.5f; i--) {
            stack++;
        }

        return camTrackY + CAM_TRACK_HEIGHT / 2 - stack * (DIAMOND_RADIUS * 2 + 1);
    }

    private int diamondAt(double mouseX, double mouseY) {
        final CameraPathSampler sampler = session.sampler();
        if (sampler == null) {
            return -1;
        }

        int best = -1;
        double bestDistance = Double.MAX_VALUE;

        for (int i = 0; i < sampler.keyframeCount(); i++) {
            final double dx = Math.abs(mouseX - diamondX(sampler, i));
            final double dy = Math.abs(mouseY - diamondY(sampler, i));
            if (dx <= DIAMOND_RADIUS + 2 && dy <= DIAMOND_RADIUS + 2 && dx + dy < bestDistance) {
                bestDistance = dx + dy;
                best = i;
            }

        }

        return best;
    }

    private int effectTrackAt(double mouseY) {
        if (mouseY < effectTracksY || mouseY >= effectTracksY + EFFECT_TRACK_HEIGHT * EFFECT_TRACKS.length) {
            return -1;
        }

        return Math.min(EFFECT_TRACKS.length - 1, (int) ((mouseY - effectTracksY) / EFFECT_TRACK_HEIGHT));
    }

    @Nullable
    private EditorEffect effectWindowAt(double mouseX, double mouseY) {
        final int track = effectTrackAt(mouseY);
        if (track < 0) {
            return null;
        }

        final List<EditorEffect> effects = session.effects();
        for (int i = effects.size() - 1; i >= 0; i--) {
            final EditorEffect effect = effects.get(i);
            if (effect.effect != EFFECT_TRACKS[track]) {
                continue;
            }

            if (mouseX >= timeToX(effect.start) - 2 && mouseX <= timeToX(effect.end) + 2) {
                return effect;
            }

        }

        return null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No black background
        final boolean hasSelection = session.selectedKeyframe() != null || session.selectedEffect() != null;
        final int panelTop = hasSelection ? inspectorTop : blockTop;

        // Soft drop shadow above the block
        graphics.fillGradient(blockLeft, panelTop - 7, blockRight, panelTop, 0x00000000, COLOR_SHADOW);

        drawPanel(graphics, blockLeft, blockTop, blockRight, blockBottom, !hasSelection);
        if (hasSelection) {
            drawPanel(graphics, blockLeft, inspectorTop, blockRight, blockTop, true);
        }

        renderTimeline(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);

        renderToolbarSeparators(graphics);
        renderInspectorExtras(graphics);
        renderViewportHints(graphics);
    }

    private void drawPanel(GuiGraphics graphics, int left, int top, int right, int bottom, boolean accentTop) {
        graphics.fillGradient(left, top, right, bottom, COLOR_PANEL_TOP, COLOR_PANEL_BOTTOM);

        graphics.fill(left, top, right, top + 1, COLOR_BORDER);
        graphics.fill(left, bottom - 1, right, bottom, COLOR_BORDER);
        graphics.fill(left, top, left + 1, bottom, COLOR_BORDER);
        graphics.fill(right - 1, top, right, bottom, COLOR_BORDER);

        if (accentTop) {
            graphics.fill(left + 1, top, right - 1, top + 1, COLOR_EDGE_ACCENT);
        }

    }

    private void renderToolbarSeparators(GuiGraphics graphics) {
        for (final int x : toolbarSeparators) {
            graphics.fill(x, toolbarY + 2, x + 1, toolbarY + WIDGET_HEIGHT - 2, COLOR_SEPARATOR);
        }

    }

    private void renderInspectorExtras(GuiGraphics graphics) {
        final EditorKeyframe keyframe = session.selectedKeyframe();
        final EditorEffect effect = session.selectedEffect();

        if (keyframe != null) {
            if (curveWidth > 0) {
                renderEasingCurve(graphics, keyframe.easing);
            }

            final String label = "Keyframe " + (session.selectedIndex() + 1) + " / " + session.keyframeCount() + " · " + prettify(keyframe.easing.name());
            graphics.drawString(font, label, blockRight - 8 - font.width(label), inspectorTop + 10, COLOR_SECTION, false);
        }
        else if (effect != null) {
            final String label = effect.start + "t - " + effect.end + "t";
            graphics.drawString(font, label, blockRight - 8 - font.width(label), inspectorTop + 10, COLOR_SECTION, false);
        }

    }

    /**
     * Timeline
     */
    private void renderTimeline(GuiGraphics graphics, int mouseX, int mouseY) {
        final float total = timelineTotal();
        final int endTicks = session.endTicks();

        // Tracking labels
        graphics.drawString(font, "CAM", blockLeft + 4, camTrackY + (CAM_TRACK_HEIGHT - 8) / 2, 0xFFC2A24C, false);
        for (int i = 0; i < EFFECT_TRACKS.length; i++) {
            int y = effectTracksY + i * EFFECT_TRACK_HEIGHT;
            if (i % 2 == 0) {
                graphics.fill(stripX, y, stripX + stripWidth, y + EFFECT_TRACK_HEIGHT, COLOR_TRACK_ALT);
            }

            graphics.drawString(font, EFFECT_TRACK_LABELS[i], blockLeft + 4, y + (EFFECT_TRACK_HEIGHT - 8) / 2 + 1, EFFECT_TRACK_LABEL_COLORS[i], false);
        }

        final int tracksBottom = effectTracksY + EFFECT_TRACK_HEIGHT * EFFECT_TRACKS.length;

        final int endX = Mth.clamp(Math.round(timeToX(endTicks)), stripX, stripX + stripWidth);
        graphics.fill(endX, rulerY, stripX + stripWidth, tracksBottom, COLOR_TIMELINE_HEADROOM);

        final CameraPathSampler sampler = session.sampler();

        // End hold
        if (sampler != null) {
            final float travel = sampler.travelTicks();
            if (endTicks > travel + 0.5f) {
                graphics.fill(Math.round(timeToX(travel)), camTrackY, endX, camTrackY + CAM_TRACK_HEIGHT, COLOR_TIMELINE_HOLD);
            }

        }

        // Tick ruler
        final int step = rulerStep(total);
        final int minorStep = Math.max(1, step / 5);

        if (minorStep * (stripWidth / Math.max(1f, total)) >= 4f) {
            for (int t = 0; t <= (int) total; t += minorStep) {
                if (t % step == 0) {
                    continue;
                }

                final int x = Math.round(timeToX(t));
                graphics.fill(x, rulerY + RULER_HEIGHT - 2, x + 1, rulerY + RULER_HEIGHT, COLOR_TIMELINE_RULER_MINOR);
            }

        }

        for (int t = 0; t <= (int) total; t += step) {
            final int x = Math.round(timeToX(t));

            graphics.fill(x, rulerY + RULER_HEIGHT - 4, x + 1, rulerY + RULER_HEIGHT, COLOR_TIMELINE_RULER);
            graphics.fill(x, camTrackY, x + 1, tracksBottom, COLOR_TIMELINE_GRID);

            final String label = t + "t";
            if (x + font.width(label) + 2 < stripX + stripWidth) {
                graphics.drawString(font, label, x + 2, rulerY + 1, COLOR_SECTION, false);
            }

        }

        graphics.fill(stripX, rulerY + RULER_HEIGHT, stripX + stripWidth, rulerY + RULER_HEIGHT + 1, COLOR_SEPARATOR);

        // Effect windows
        final EditorEffect hoveredEffect = (draggingEffect != null || draggingPlayhead || draggingKeyIndex >= 0) ? draggingEffect : effectWindowAt(mouseX, mouseY);
        for (final EditorEffect effect : session.effects()) {
            final int track = effectTrackIndex(effect.effect);
            if (track >= 0) {
                drawEffectBand(graphics, effect, effectTracksY + track * EFFECT_TRACK_HEIGHT, effect == session.selectedEffect(), effect == hoveredEffect);
            }

        }

        // End flag
        graphics.fill(endX - 1, rulerY, endX + 2, tracksBottom, COLOR_END_MARKER_GLOW);
        graphics.fill(endX, rulerY, endX + 1, tracksBottom, COLOR_END_MARKER);
        for (int i = 0; i < 4; i++) {
            graphics.fill(endX - i, rulerY + i, endX + 1 + i, rulerY + i + 1, COLOR_END_MARKER);
        }

        final String endLabel = endTicks + "t";
        graphics.drawString(font, endLabel, Math.min(endX + 3, stripX + stripWidth - font.width(endLabel)), rulerY + RULER_HEIGHT + 2, COLOR_END_MARKER, false);

        // Playhead
        final float scrub = Mth.clamp(session.scrubTicks, 0f, endTicks);
        int playheadX = Mth.clamp(Math.round(timeToX(scrub)), stripX, stripX + stripWidth);
        graphics.fill(playheadX - 1, rulerY, playheadX + 2, tracksBottom, COLOR_TIMELINE_PLAYHEAD_GLOW);
        graphics.fill(playheadX, rulerY, playheadX + 1, tracksBottom, COLOR_TIMELINE_PLAYHEAD);
        for (int i = 0; i < 4; i++) {
            graphics.fill(playheadX - 3 + i, rulerY + i, playheadX + 4 - i, rulerY + i + 1, COLOR_TIMELINE_PLAYHEAD);
        }

        final String time = Math.round(scrub) + "t";
        final int timeWidth = font.width(time);
        final int timeX = playheadX + 5 + timeWidth < stripX + stripWidth ? playheadX + 5 : playheadX - timeWidth - 5;
        graphics.fill(timeX - 3, tracksBottom - 12, timeX + timeWidth + 3, tracksBottom - 1, 0xC8090D14);
        graphics.drawString(font, time, timeX, tracksBottom - 10, COLOR_TIMELINE_PLAYHEAD, false);

        // Fallback text if there are no keyframes in the timeline (the sampler is null)
        if (sampler == null) {
            final String hint = "Fly around and press F (or + Key) to place the first keyframe";
            graphics.drawCenteredString(font, Component.literal(hint).withStyle(ChatFormatting.GRAY), stripX + stripWidth / 2, camTrackY + (CAM_TRACK_HEIGHT - 8) / 2, 0xFFFFFF);
            return;
        }

        // Chain brackets between diamonds
        for (int i = 1; i < sampler.keyframeCount(); i++) {
            if (sampler.chainedIntoPrevious(i)) {
                final int x1 = diamondX(sampler, i - 1);
                final int x2 = diamondX(sampler, i);
                final int y = camTrackY + 1;

                graphics.fill(x1, y, x2 + 1, y + 1, COLOR_CHAIN);
                graphics.fill(x1, y, x1 + 1, y + 3, COLOR_CHAIN);
                graphics.fill(x2, y, x2 + 1, y + 3, COLOR_CHAIN);
            }

        }

        // Keyframe diamonds
        final int hovered = (draggingKeyIndex >= 0 || draggingPlayhead || draggingEffect != null) ? draggingKeyIndex : diamondAt(mouseX, mouseY);
        for (int i = 0; i < sampler.keyframeCount(); i++) {
            final int x = diamondX(sampler, i);
            final int y = diamondY(sampler, i);
            final boolean isSelected = i == session.selectedIndex();

            if (isSelected || i == hovered) {
                drawDiamond(graphics, x, y, DIAMOND_RADIUS + 2, isSelected ? COLOR_ACCENT_GLOW : COLOR_KEY_DIAMOND_HOVER_GLOW);
            }

            final int color = isSelected ? COLOR_KEY_DIAMOND_SELECTED : (i == hovered ? COLOR_KEY_DIAMOND_HOVER : COLOR_KEY_DIAMOND);
            drawDiamond(graphics, x, y, DIAMOND_RADIUS, color);
        }

    }

    private int effectTrackIndex(CameraEffect effect) {
        for (int i = 0; i < EFFECT_TRACKS.length; i++) {
            if (EFFECT_TRACKS[i] == effect) {
                return i;
            }

        }

        return -1;
    }

    /**
     * One effect window band
     */
    private void drawEffectBand(GuiGraphics graphics, EditorEffect effect, int trackY, boolean selected, boolean hovered) {
        final int x1 = Mth.clamp(Math.round(timeToX(effect.start)), stripX, stripX + stripWidth);
        final int x2 = Mth.clamp(Math.round(timeToX(effect.end)), stripX, stripX + stripWidth);
        if (x2 <= x1) {
            x2 = x1 + 1;
        }

        final int accent = effectColor(effect.effect);
        final int body = (accent & 0x00FFFFFF) | 0x30000000;

        final int top = trackY + 1;
        final int bottom = trackY + EFFECT_TRACK_HEIGHT - 1;

        graphics.fill(x1, top, x2, bottom, body);

        // Intensity silhouette
        final int shapeHeight = bottom - top - 1;
        for (int px = x1; px < x2; px++) {
            float progress = (px - x1) / (float) Math.max(1, x2 - x1 - 1);
            int columnHeight = Math.round(effect.mode.intensityAt(progress) * shapeHeight);

            if (columnHeight > 0) {
                graphics.fill(px, bottom - columnHeight, px + 1, bottom, (accent & 0x00FFFFFF) | 0x90000000);
            }

        }

        final int borderColor = selected ? COLOR_ACCENT : (hovered ? 0xFFFFFFFF : accent);
        graphics.fill(x1, top, x2, top + 1, borderColor);
        graphics.fill(x1, bottom - 1, x2, bottom, borderColor);
        graphics.fill(x1, top, x1 + 1, bottom, borderColor);
        graphics.fill(x2 - 1, top, x2, bottom, borderColor);

        final String label = prettify(effect.mode.name());
        if (x2 - x1 > font.width(label) + 6) {
            graphics.drawCenteredString(font, label, (x1 + x2) / 2, top + (bottom - top - 8) / 2 + 1, 0xFFFFFFFF);
        }

    }

    private int effectColor(CameraEffect effect) {
        return switch (effect) {
            case FADE -> 0xFFC8CCD8;
            case BLUR -> 0xFF6FA8FF;
            case LETTERBOX -> 0xFFB07BFF;
        };

    }

    private void drawDiamond(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            final int half = radius - Math.abs(dy);
            graphics.fill(centerX - half, centerY + dy, centerX + half + 1, centerY + dy + 1, color);
        }

    }

    private int rulerStep(float total) {
        final int[] steps = { 5, 10, 20, 40, 100, 200, 400, 1000, 2400, 6000 };

        for (final int step : steps) {
            if (step * (stripWidth / Math.max(1f, total)) >= 44f) {
                return step;
            }

        }

        return steps[steps.length - 1];
    }

    /**
     * Live preview of the selected easing
     */
    private void renderEasingCurve(GuiGraphics graphics, KnightLibEasings easing) {
        graphics.fill(curveX, curveY, curveX + curveWidth, curveY + curveHeight, COLOR_CURVE_BG);
        graphics.fill(curveX, curveY, curveX + curveWidth, curveY + 1, COLOR_SEPARATOR);
        graphics.fill(curveX, curveY + curveHeight - 1, curveX + curveWidth, curveY + curveHeight, COLOR_SEPARATOR);
        graphics.fill(curveX, curveY, curveX + 1, curveY + curveHeight, COLOR_SEPARATOR);
        graphics.fill(curveX + curveWidth - 1, curveY, curveX + curveWidth, curveY + curveHeight, COLOR_SEPARATOR);
        graphics.fill(curveX + 1, curveY + curveHeight / 2, curveX + curveWidth - 1, curveY + curveHeight / 2 + 1, COLOR_CURVE_GRID);

        final int plotTop = curveY + 2;
        final int plotHeight = curveHeight - 4;

        int previousY = -1;
        for (int px = 1; px < curveWidth - 1; px++) {
            final float time = (px - 1) / (float) (curveWidth - 3);
            final float value = Mth.clamp(easing.apply(time), -0.15f, 1.15f);

            final int py = Mth.clamp(plotTop + Math.round((1f - value) * plotHeight), curveY + 1, curveY + curveHeight - 2);
            final int fromY = previousY == -1 ? py : Math.min(previousY, py);
            final int toY = previousY == -1 ? py : Math.max(previousY, py);

            // Area fill under the curve
            if (py < curveY + curveHeight - 2) {
                graphics.fill(curveX + px, py + 1, curveX + px + 1, curveY + curveHeight - 1, COLOR_CURVE_FILL);
            }

            graphics.fill(curveX + px, fromY, curveX + px + 1, toY + 1, COLOR_ACCENT);
            previousY = py;
        }

    }

    /**
     * Controls card
     */
    private void renderViewportHints(GuiGraphics graphics) {
        final String speed = String.format(Locale.ROOT, "%.2f", freeCamera().speed());

        final String[] hints = {
                "RMB: look · WASD/Space/Shift: fly · Scroll: speed (" + speed + " b/t)",
                "LMB: select · XYZ arrows: move · rings: yaw/pitch · body drag: free move · F: add · C: chain",
                "Timeline: drag diamonds to retime · 2x click on FADE/BLUR/BARS adds an effect",
                "Drag the orange flag to set the end · RMB deletes an effect"
        };

        final String title = "◆ CAMERA PATH EDITOR";
        final String indicator = hintsCollapsed ? "+" : "–";

        final int paddingX = 8;
        final int lineHeight = 12;
        final int titleHeight = 16;

        int panelWidth = font.width(title) + font.width(indicator) + paddingX * 3;
        if (!hintsCollapsed) {
            for (final String hint : hints) {
                panelWidth = Math.max(panelWidth, font.width(hint) + paddingX * 2);
            }

        }

        final int left = 6;
        final int top = 6;
        final int panelHeight = hintsCollapsed ? titleHeight + 2 : titleHeight + 5 + hints.length * lineHeight + 2;

        hintsPanelWidth = panelWidth;
        hintsPanelHeight = panelHeight;

        graphics.fillGradient(left, top, left + panelWidth, top + panelHeight, 0xBE141826, 0xCE0A0C12);
        graphics.fill(left, top, left + panelWidth, top + 1, COLOR_BORDER);
        graphics.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, COLOR_BORDER);
        graphics.fill(left, top, left + 1, top + panelHeight, COLOR_BORDER);
        graphics.fill(left + panelWidth - 1, top, left + panelWidth, top + panelHeight, COLOR_BORDER);
        graphics.fill(left + 1, top, left + panelWidth - 1, top + 1, COLOR_EDGE_ACCENT);

        graphics.drawString(font, title, left + paddingX, top + 5, COLOR_ACCENT, false);
        graphics.drawString(font, indicator, left + panelWidth - paddingX - font.width(indicator), top + 5, COLOR_HINT_KEY, false);

        if (hintsCollapsed) {
            return;
        }

        graphics.fill(left + 1, top + titleHeight, left + panelWidth - 1, top + titleHeight + 1, COLOR_SEPARATOR);

        int y = top + titleHeight + 5;
        for (final String hint : hints) {
            int x = left + paddingX;
            final String[] segments = hint.split(" · ");

            for (int i = 0; i < segments.length; i++) {
                if (i > 0) {
                    graphics.drawString(font, " · ", x, y, COLOR_HINT_DOT, false);
                    x += font.width(" · ");
                }

                final String segment = segments[i];
                final int colon = segment.indexOf(": ");

                if (colon >= 0) {
                    final String key = segment.substring(0, colon);
                    final String action = segment.substring(colon);

                    graphics.drawString(font, key, x, y, COLOR_HINT_KEY, false);
                    x += font.width(key);
                    graphics.drawString(font, action, x, y, COLOR_SECTION, false);
                    x += font.width(action);
                }
                else {
                    graphics.drawString(font, segment, x, y, COLOR_SECTION, false);
                    x += font.width(segment);
                }

            }

            y += lineHeight;
        }

    }

    @Override
    public void onClose() {
        CameraPathEditor.onScreenClosed();
        super.onClose();
    }

    @Override
    public void removed() {
        if (looking) {
            endLook();
        }

        freeCamera().stopMoving();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String prettify(String enumName) {
        return enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String modeLabel(CameraEffect effect, EffectMode mode) {
        final String name = prettify(effect.name());
        return switch (mode) {
            case IN -> name + " in";
            case OUT -> name + " out";
            case FULL -> name;
            case HOLD -> name + " hold";
        };

    }

}