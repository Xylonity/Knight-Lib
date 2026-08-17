package dev.xylonity.knightlib.client.camera.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPathManager;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPathSampler;
import dev.xylonity.knightlib.api.event.impl.client.ClientRenderLevelStageEvent;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderStage;
import dev.xylonity.knightlib.client.KnightLibRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * In-world visualization of the path being edited by the campath system.
 * The trajectory curve, a camera per keyframe, tangent handles on the selected keyframe and an additional camera at the current timeline position.
 */
public final class EditorPathRenderer {

    private static final int CURVE_STEPS = 24;
    private static final float TIME_MARK_EVERY_TICKS = 20f;
    private static final float HANDLE_FRACTION = 1f / 3f;

    // Path gradient endpoints
    private static final float[] COLOR_START = { 0.30f, 0.85f, 1.00f };
    private static final float[] COLOR_END = { 1.00f, 0.55f, 0.20f };

    private static final float[] COLOR_SELECTED = { 0.95f, 0.78f, 0.25f };
    private static final float[] COLOR_KEYFRAME = { 0.92f, 0.92f, 0.92f };
    private static final float[] COLOR_HANDLE = { 1.00f, 0.35f, 0.80f };
    private static final float[] COLOR_GHOST = { 0.35f, 1.00f, 0.75f };
    private static final float[] COLOR_CUT = { 0.60f, 0.60f, 0.65f };

    // Move/rotate camera of the selected keyframe
    private static final float[] COLOR_AXIS_X = { 0.96f, 0.28f, 0.32f };
    private static final float[] COLOR_AXIS_Y = { 0.42f, 0.88f, 0.36f };
    private static final float[] COLOR_AXIS_Z = { 0.30f, 0.52f, 0.98f };
    private static final float[] COLOR_RING_YAW = { 0.25f, 0.85f, 0.85f };
    private static final float[] COLOR_RING_PITCH = { 0.95f, 0.60f, 0.25f };

    private static final int RING_SEGMENTS = 40;

    private static final Matrix4f LAST_PROJECTION = new Matrix4f();
    private static final Matrix4f LAST_MODEL_VIEW = new Matrix4f();
    private static Vec3 lastCameraPos = Vec3.ZERO;
    private static boolean hasMatrices = false;

    private EditorPathRenderer() {
        ;;
    }

    public static void onRenderStage(final ClientRenderLevelStageEvent event) {
        if (event.getStage() != PostShaderRenderStage.AFTER_LEVEL) {
            return;
        }

        if (!CameraPathEditor.isActive() || CameraPathEditor.isPreviewing() || CameraPathManager.isPlaying()) {
            hasMatrices = false;
            return;
        }

        final Camera camera = event.getCamera();

        // The picking matrix is rebuilt from the camera state (as it is not guaranteed that the camera rotations are present in this stage)
        LAST_PROJECTION.set(event.getProjection());
        LAST_MODEL_VIEW.identity()
                .rotate(Axis.XP.rotationDegrees(camera.getXRot()))
                .rotate(Axis.YP.rotationDegrees(camera.getYRot() + 180f));
        lastCameraPos = camera.getPosition();
        hasMatrices = true;

        final CameraEditorSession session = CameraPathEditor.session();
        if (session.isEmpty()) {
            return;
        }

        final PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180f));
        poseStack.translate(-lastCameraPos.x, -lastCameraPos.y, -lastCameraPos.z);

        final MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();

        draw(poseStack, buffers.getBuffer(KnightLibRenderTypes.LINES_SEE_THROUGH), session, 0.25f);
        buffers.endBatch(KnightLibRenderTypes.LINES_SEE_THROUGH);

        draw(poseStack, buffers.getBuffer(RenderType.lines()), session, 1f);
        buffers.endBatch(RenderType.lines());

        drawLabels(poseStack, buffers, session, camera);
        buffers.endBatch();
    }

    /**
     * World-space ray under the mouse cursor, reconstructed from the last frame matrices
     */
    @Nullable
    public static PickRay pickRay(double mouseX, double mouseY) {
        if (!hasMatrices) {
            return null;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final double guiWidth = minecraft.getWindow().getGuiScaledWidth();
        final double guiHeight = minecraft.getWindow().getGuiScaledHeight();

        final float ndcX = (float) (2.0 * mouseX / guiWidth - 1.0);
        final float ndcY = (float) (1.0 - 2.0 * mouseY / guiHeight);

        final Matrix4f inverse = new Matrix4f(LAST_PROJECTION).mul(LAST_MODEL_VIEW);
        if (Math.abs(inverse.determinant()) < 1.0E-12) {
            return null;
        }

        inverse.invert();

        final Vector4f near = inverse.transform(new Vector4f(ndcX, ndcY, -1f, 1f));
        final Vector4f far = inverse.transform(new Vector4f(ndcX, ndcY, 1f, 1f));
        if (near.w == 0f || far.w == 0f) {
            return null;
        }

        // The matrices are camera-relative, so I shift back into the absolute world space
        final Vec3 nearPos = lastCameraPos.add(near.x / near.w, near.y / near.w, near.z / near.w);
        final Vec3 farPos = lastCameraPos.add(far.x / far.w, far.y / far.w, far.z / far.w);
        return new PickRay(nearPos, farPos.subtract(nearPos).normalize());
    }

    /**
     * Picks whatever sits under the cursor
     */
    @Nullable
    public static PickResult pick(CameraEditorSession session, double mouseX, double mouseY) {
        final PickRay ray = pickRay(mouseX, mouseY);
        if (ray == null || session.isEmpty()) {
            return null;
        }

        final int selected = session.selectedIndex();
        if (selected >= 0) {
            final PickResult gizmo = pickGizmo(ray, session, selected);
            if (gizmo != null) {
                return gizmo;
            }

            final PickResult handle = pickBetween(ray,
                    pickAt(ray, PickKind.HANDLE_OUT, selected, outHandle(session, selected), 0.10),
                    pickAt(ray, PickKind.HANDLE_IN, selected, inHandle(session, selected), 0.10));

            if (handle != null) {
                return handle;
            }

        }

        PickResult best = null;
        for (int i = 0; i < session.keyframeCount(); i++) {
            best = pickBetween(ray, best, pickAt(ray, PickKind.KEYFRAME, i, session.keyframes().get(i).position, 0.45));
        }

        return best;
    }

    /**
     * Picks the translate arrows and rotation rings of the selected keyframe
     */
    @Nullable
    private static PickResult pickGizmo(PickRay ray, CameraEditorSession session, int selected) {
        final EditorKeyframe keyframe = session.keyframes().get(selected);
        final Vec3 position = keyframe.position;

        final double length = gizmoScale(position);
        final double ring = ringRadius(position);

        // Arrows are only grabbable on their outer span
        final double inner = length * 0.3;

        PickResult best = null;
        best = pickBetween(ray, best, pickSegment(ray, PickKind.AXIS_X, selected, position.add(inner, 0, 0), position.add(length, 0, 0)));
        best = pickBetween(ray, best, pickSegment(ray, PickKind.AXIS_Y, selected, position.add(0, inner, 0), position.add(0, length, 0)));
        best = pickBetween(ray, best, pickSegment(ray, PickKind.AXIS_Z, selected, position.add(0, 0, inner), position.add(0, 0, length)));

        best = pickBetween(ray, best, pickRing(ray, PickKind.RING_YAW, selected, position, new Vec3(0, 1, 0), ring));
        best = pickBetween(ray, best, pickRing(ray, PickKind.RING_PITCH, selected, position, rightVector(keyframe.yaw), ring));

        return best;
    }

    /**
     * World size of the selected keyframe's gizmo
     */
    public static double gizmoScale(Vec3 position) {
        return Math.max(0.5, position.distanceTo(lastCameraPos) * 0.11);
    }

    public static double ringRadius(Vec3 position) {
        return gizmoScale(position) * 0.72;
    }

    public static Vec3 axisDirection(PickKind kind) {
        return switch (kind) {
            case AXIS_X -> new Vec3(1, 0, 0);
            case AXIS_Y -> new Vec3(0, 1, 0);
            case AXIS_Z -> new Vec3(0, 0, 1);
            default -> Vec3.ZERO;
        };

    }

    /**
     * Horizontal right vector of a camera with the given yaw
     */
    public static Vec3 rightVector(float yaw) {
        final float rad = (float) Math.toRadians(yaw);
        return new Vec3(Mth.cos(rad), 0, Mth.sin(rad));
    }

    /**
     * Horizontal forward vector of a camera with the given yaw
     */
    public static Vec3 forwardVector(float yaw) {
        final float rad = (float) Math.toRadians(yaw);
        return new Vec3(-Mth.sin(rad), 0, Mth.cos(rad));
    }

    /**
     * Parameter along an axis line of the point closest to the cursor ray
     */
    public static double axisParameter(PickRay ray, Vec3 origin, Vec3 axis) {
        final Vec3 w = origin.subtract(ray.origin());

        final double b = axis.dot(ray.direction());
        final double denominator = 1.0 - b * b;
        if (Math.abs(denominator) < 1.0E-6) {
            // Axis parallel to the view ray
            return 0.0;
        }

        return (b * ray.direction().dot(w) - axis.dot(w)) / denominator;
    }

    @Nullable
    private static PickResult pickSegment(PickRay ray, PickKind kind, int index, Vec3 from, Vec3 to) {
        Vec3 axis = to.subtract(from);
        final double length = axis.length();
        if (length < 1.0E-6) {
            return null;
        }

        axis = axis.scale(1.0 / length);

        final double s = Mth.clamp(axisParameter(ray, from, axis), 0.0, length);
        final Vec3 point = from.add(axis.scale(s));

        final double along = point.subtract(ray.origin()).dot(ray.direction());
        if (along < 0.2) {
            return null;
        }

        final double radius = Math.max(0.07, along * 0.02);
        final double distance = ray.origin().add(ray.direction().scale(along)).distanceTo(point);

        return distance <= radius ? new PickResult(kind, index, point, along) : null;
    }

    @Nullable
    private static PickResult pickRing(PickRay ray, PickKind kind, int index, Vec3 center, Vec3 normal, double radius) {
        final Vec3 hit = intersectPlane(ray, center, normal);
        if (hit == null) {
            return null;
        }

        final double along = hit.subtract(ray.origin()).dot(ray.direction());
        if (along < 0.2) {
            return null;
        }

        final double tolerance = Math.max(0.07, along * 0.02);
        if (Math.abs(hit.distanceTo(center) - radius) > tolerance) {
            return null;
        }

        return new PickResult(kind, index, hit, along);
    }

    /**
     * Cursor ray / plane intersection
     */
    @Nullable
    public static Vec3 intersectPlane(PickRay ray, Vec3 planePoint, Vec3 planeNormal) {
        final double denominator = ray.direction().dot(planeNormal);
        if (Math.abs(denominator) < 1.0E-6) {
            return null;
        }

        final double t = planePoint.subtract(ray.origin()).dot(planeNormal) / denominator;
        if (t < 0.05) {
            return null;
        }

        return ray.origin().add(ray.direction().scale(t));
    }

    /**
     * World position of the outgoing tangent handle
     */
    @Nullable
    public static Vec3 outHandle(CameraEditorSession session, int index) {
        final CameraPathSampler sampler = session.sampler();
        if (sampler == null || !session.smoothPosition || index < 0 || index >= sampler.keyframeCount() - 1 || sampler.isCut(index + 1)) {
            return null;
        }

        return sampler.keyframe(index).position().add(sampler.outTangent(index).scale(HANDLE_FRACTION));
    }

    /**
     * World position of the incoming tangent handle
     */
    @Nullable
    public static Vec3 inHandle(CameraEditorSession session, int index) {
        final CameraPathSampler sampler = session.sampler();
        if (sampler == null || !session.smoothPosition || index <= 0 || index >= sampler.keyframeCount() || sampler.isCut(index)) {
            return null;
        }

        return sampler.keyframe(index).position().subtract(sampler.inTangent(index).scale(HANDLE_FRACTION));
    }

    @Nullable
    private static PickResult pickAt(PickRay ray, PickKind kind, int index, @Nullable Vec3 position, double baseRadius) {
        if (position == null) {
            return null;
        }

        final double along = position.subtract(ray.origin()).dot(ray.direction());
        if (along < 0.2) {
            return null;
        }

        // The world-space radius of the target (with a little distance increase)
        final double radius = Math.max(baseRadius, along * 0.022);
        final double distance = ray.origin().add(ray.direction().scale(along)).distanceTo(position);

        return distance <= radius ? new PickResult(kind, index, position, along) : null;
    }

    @Nullable
    private static PickResult pickBetween(PickRay ray, @Nullable PickResult a, @Nullable PickResult b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        return a.distanceAlongRay() <= b.distanceAlongRay() ? a : b;
    }

    private static void draw(PoseStack poseStack, VertexConsumer lines, CameraEditorSession session, float alpha) {
        final CameraPathSampler sampler = session.sampler();
        if (sampler == null) {
            return;
        }

        drawTrajectory(poseStack, lines, sampler, alpha);
        drawTimeMarks(poseStack, lines, sampler, alpha);

        for (int i = 0; i < session.keyframeCount(); i++) {
            final EditorKeyframe keyframe = session.keyframes().get(i);
            final float[] color = (i == session.selectedIndex()) ? COLOR_SELECTED : COLOR_KEYFRAME;

            drawCameraGizmo(poseStack, lines, keyframe.position, keyframe.yaw, keyframe.pitch, color, alpha, 0.7f);
        }

        drawHandles(poseStack, lines, session, alpha);
        drawMoveRotateGizmo(poseStack, lines, session, alpha);
        drawGhost(poseStack, lines, session, sampler, alpha);
    }

    private static void drawMoveRotateGizmo(PoseStack poseStack, VertexConsumer lines, CameraEditorSession session, float alpha) {
        final EditorKeyframe keyframe = session.selectedKeyframe();
        if (keyframe == null) {
            return;
        }

        final Vec3 position = keyframe.position;
        final double length = gizmoScale(position);
        final double ring = ringRadius(position);

        drawArrow(poseStack, lines, position, new Vec3(1, 0, 0), length, COLOR_AXIS_X, alpha);
        drawArrow(poseStack, lines, position, new Vec3(0, 1, 0), length, COLOR_AXIS_Y, alpha);
        drawArrow(poseStack, lines, position, new Vec3(0, 0, 1), length, COLOR_AXIS_Z, alpha);

        // Yaw and pitch rings
        drawRing(poseStack, lines, position, new Vec3(1, 0, 0), new Vec3(0, 0, 1), ring, COLOR_RING_YAW, alpha);
        drawRing(poseStack, lines, position, forwardVector(keyframe.yaw), new Vec3(0, 1, 0), ring, COLOR_RING_PITCH, alpha);
    }

    private static void drawArrow(PoseStack poseStack, VertexConsumer lines, Vec3 origin, Vec3 direction, double length, float[] color, float alpha) {
        final Vec3 tip = origin.add(direction.scale(length));
        line(poseStack, lines, origin, tip, color[0], color[1], color[2], alpha);

        // Arrow head
        final Vec3 up = Math.abs(direction.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        final Vec3 sideA = direction.cross(up).normalize().scale(length * 0.06);
        final Vec3 sideB = direction.cross(sideA).normalize().scale(length * 0.06);
        final Vec3 back = tip.subtract(direction.scale(length * 0.16));

        line(poseStack, lines, tip, back.add(sideA), color[0], color[1], color[2], alpha);
        line(poseStack, lines, tip, back.subtract(sideA), color[0], color[1], color[2], alpha);
        line(poseStack, lines, tip, back.add(sideB), color[0], color[1], color[2], alpha);
        line(poseStack, lines, tip, back.subtract(sideB), color[0], color[1], color[2], alpha);
    }

    private static void drawRing(PoseStack poseStack, VertexConsumer lines, Vec3 center, Vec3 basisA, Vec3 basisB, double radius, float[] color, float alpha) {
        Vec3 previous = center.add(basisA.scale(radius));

        for (int i = 1; i <= RING_SEGMENTS; i++) {
            final double angle = (Math.PI * 2.0 * i) / RING_SEGMENTS;
            final Vec3 current = center.add(basisA.scale(radius * Math.cos(angle))).add(basisB.scale(radius * Math.sin(angle)));

            line(poseStack, lines, previous, current, color[0], color[1], color[2], alpha);
            previous = current;
        }

    }

    /**
     * The trajectory line
     */
    private static void drawTrajectory(PoseStack poseStack, VertexConsumer lines, CameraPathSampler sampler, float alpha) {
        final float travel = Math.max(1f, sampler.travelTicks());

        for (int i = 1; i < sampler.keyframeCount(); i++) {
            final Vec3 from = sampler.keyframe(i - 1).position();
            final Vec3 to = sampler.keyframe(i).position();

            if (sampler.isCut(i)) {
                drawDashed(poseStack, lines, from, to, COLOR_CUT, alpha * 0.8f);
                continue;
            }

            final float startTime = sampler.segmentStart(i) / travel;
            final float endTime = (sampler.segmentStart(i) + sampler.keyframe(i).durationTicks()) / travel;

            Vec3 previous = from;
            for (int step = 1; step <= CURVE_STEPS; step++) {
                final float t = step / (float) CURVE_STEPS;
                final Vec3 current = sampler.positionOnSegment(i, t);

                final float time = Mth.lerp((step - 0.5f) / CURVE_STEPS, startTime, endTime);
                final float[] color = gradient(time);

                line(poseStack, lines, previous, current, color[0], color[1], color[2], alpha);
                previous = current;
            }

        }

    }

    /**
     * Small vertical marks along the curve at a fixed tick interval
     */
    private static void drawTimeMarks(PoseStack poseStack, VertexConsumer lines, CameraPathSampler sampler, float alpha) {
        float travel = sampler.travelTicks();

        for (float ticks = TIME_MARK_EVERY_TICKS; ticks < travel; ticks += TIME_MARK_EVERY_TICKS) {
            final Vec3 position = sampler.sample(ticks).position();
            final float[] color = gradient(ticks / Math.max(1f, travel));

            line(poseStack, lines, position.add(0, -0.07, 0), position.add(0, 0.07, 0), color[0], color[1], color[2], alpha);
        }

    }

    private static void drawHandles(PoseStack poseStack, VertexConsumer lines, CameraEditorSession session, float alpha) {
        final int selected = session.selectedIndex();
        final EditorKeyframe keyframe = session.selectedKeyframe();
        if (keyframe == null) {
            return;
        }

        final Vec3 outHandle = outHandle(session, selected);
        final Vec3 inHandle = inHandle(session, selected);

        if (outHandle != null) {
            line(poseStack, lines, keyframe.position, outHandle, COLOR_HANDLE[0], COLOR_HANDLE[1], COLOR_HANDLE[2], alpha * 0.85f);
            drawDiamond(poseStack, lines, outHandle, COLOR_HANDLE, alpha);
        }

        if (inHandle != null) {
            line(poseStack, lines, keyframe.position, inHandle, COLOR_HANDLE[0], COLOR_HANDLE[1], COLOR_HANDLE[2], alpha * 0.85f);
            drawDiamond(poseStack, lines, inHandle, COLOR_HANDLE, alpha);
        }

    }

    private static void drawGhost(PoseStack poseStack, VertexConsumer lines, CameraEditorSession session, CameraPathSampler sampler, float alpha) {
        if (session.scrubTicks <= 0f) {
            return;
        }

        final CameraPathSampler.Pose pose = sampler.sample(Math.min(session.scrubTicks, sampler.totalTicks()));
        drawCameraGizmo(poseStack, lines, pose.position(), pose.yaw(), pose.pitch(), COLOR_GHOST, alpha * 0.7f, 0.55f);
    }

    /**
     * Blender-style wireframe camera: a small body box plus a view frustum pyramid opening
     * toward where the keyframe points, with the classic "up" triangle on top of the lens
     */
    private static void drawCameraGizmo(PoseStack poseStack, VertexConsumer lines, Vec3 position, float yaw, float pitch, float[] color, float alpha, float scale) {
        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(scale, scale, scale);

        float r = color[0];
        float g = color[1];
        float b = color[2];

        // Body
        localBox(poseStack, lines, -0.16f, -0.12f, -0.34f, 0.16f, 0.12f, 0.02f, r, g, b, alpha);

        // View frustum
        float lx = 0.32f;
        float ly = 0.21f;
        float lz = 0.55f;

        localLine(poseStack, lines, 0f, 0f, 0.02f, -lx, -ly, lz, r, g, b, alpha);
        localLine(poseStack, lines, 0f, 0f, 0.02f, lx, -ly, lz, r, g, b, alpha);
        localLine(poseStack, lines, 0f, 0f, 0.02f, -lx, ly, lz, r, g, b, alpha);
        localLine(poseStack, lines, 0f, 0f, 0.02f, lx, ly, lz, r, g, b, alpha);

        // Lens rectangle
        localLine(poseStack, lines, -lx, -ly, lz, lx, -ly, lz, r, g, b, alpha);
        localLine(poseStack, lines, lx, -ly, lz, lx, ly, lz, r, g, b, alpha);
        localLine(poseStack, lines, lx, ly, lz, -lx, ly, lz, r, g, b, alpha);
        localLine(poseStack, lines, -lx, ly, lz, -lx, -ly, lz, r, g, b, alpha);

        // Up triangle on top of the lens
        localLine(poseStack, lines, -0.12f, ly + 0.02f, lz, 0.12f, ly + 0.02f, lz, r, g, b, alpha);
        localLine(poseStack, lines, -0.12f, ly + 0.02f, lz, 0f, ly + 0.2f, lz, r, g, b, alpha);
        localLine(poseStack, lines, 0.12f, ly + 0.02f, lz, 0f, ly + 0.2f, lz, r, g, b, alpha);

        poseStack.popPose();
    }

    /**
     * Camera-facing tanget grabbable marker
     */
    private static void drawDiamond(PoseStack poseStack, VertexConsumer lines, Vec3 center, float[] color, float alpha) {
        final Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        final Vec3 up = new Vec3(camera.getUpVector());
        final Vec3 right = new Vec3(camera.getLeftVector()).scale(-1);

        final double size = Math.max(0.05, center.distanceTo(lastCameraPos) * 0.014);

        final Vec3 top = center.add(up.scale(size));
        final Vec3 bottom = center.subtract(up.scale(size));
        final Vec3 rightPoint = center.add(right.scale(size));
        final Vec3 leftPoint = center.subtract(right.scale(size));

        line(poseStack, lines, top, rightPoint, color[0], color[1], color[2], alpha);
        line(poseStack, lines, rightPoint, bottom, color[0], color[1], color[2], alpha);
        line(poseStack, lines, bottom, leftPoint, color[0], color[1], color[2], alpha);
        line(poseStack, lines, leftPoint, top, color[0], color[1], color[2], alpha);
    }

    private static void drawDashed(PoseStack poseStack, VertexConsumer lines, Vec3 from, Vec3 to, float[] color, float alpha) {
        final double length = from.distanceTo(to);
        final int dashes = Math.max(1, (int) (length / 0.8));

        for (int i = 0; i < dashes; i++) {
            final float start = i / (float) dashes;
            final float end = start + 0.5f / dashes;

            line(poseStack, lines, from.lerp(to, start), from.lerp(to, end), color[0], color[1], color[2], alpha);
        }

    }

    /**
     * Floating labels above each camera keyframe
     */
    private static void drawLabels(PoseStack poseStack, MultiBufferSource buffers, CameraEditorSession session, Camera camera) {
        final Font font = Minecraft.getInstance().font;

        for (int i = 0; i < session.keyframeCount(); i++) {
            final EditorKeyframe keyframe = session.keyframes().get(i);
            final String text = "#" + (i + 1) + " · " + keyframe.durationTicks + "t";
            final int color = (i == session.selectedIndex()) ? 0xFFF2CE60 : 0xFFFFFFFF;

            poseStack.pushPose();
            poseStack.translate(keyframe.position.x, keyframe.position.y + 0.55, keyframe.position.z);
            poseStack.mulPose(camera.rotation());
            poseStack.scale(-0.02f, -0.02f, 0.02f);

            font.drawInBatch(text, -font.width(text) / 2f, 0f, color, false, poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000, 0xF000F0);

            poseStack.popPose();
        }

    }

    private static float[] gradient(float time) {
        final float t = Mth.clamp(time, 0f, 1f);

        return new float[] {
                Mth.lerp(t, COLOR_START[0], COLOR_END[0]),
                Mth.lerp(t, COLOR_START[1], COLOR_END[1]),
                Mth.lerp(t, COLOR_START[2], COLOR_END[2])
        };

    }

    private static void line(PoseStack poseStack, VertexConsumer consumer, Vec3 from, Vec3 to, float r, float g, float b, float a) {
        localLine(poseStack, consumer, (float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y, (float) to.z, r, g, b, a);
    }

    private static void localLine(PoseStack poseStack, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        final PoseStack.Pose pose = poseStack.last();

        final float dx = x2 - x1;
        final float dy = y2 - y1;
        final float dz = z2 - z1;

        final float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4f) {
            return;
        }

        final float nx = dx / length;
        final float ny = dy / length;
        final float nz = dz / length;

        consumer.vertex(pose.pose(), x1, y1, z1).color(r, g, b, a).normal(pose.normal(), nx, ny, nz).endVertex();
        consumer.vertex(pose.pose(), x2, y2, z2).color(r, g, b, a).normal(pose.normal(), nx, ny, nz).endVertex();
    }

    private static void localBox(PoseStack poseStack, VertexConsumer lines, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        localLine(poseStack, lines, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        localLine(poseStack, lines, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        localLine(poseStack, lines, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
        localLine(poseStack, lines, minX, maxY, minZ, minX, minY, minZ, r, g, b, a);

        localLine(poseStack, lines, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a);
        localLine(poseStack, lines, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        localLine(poseStack, lines, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        localLine(poseStack, lines, minX, maxY, maxZ, minX, minY, maxZ, r, g, b, a);

        localLine(poseStack, lines, minX, minY, minZ, minX, minY, maxZ, r, g, b, a);
        localLine(poseStack, lines, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        localLine(poseStack, lines, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        localLine(poseStack, lines, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
    }

    public enum PickKind {
        KEYFRAME,
        HANDLE_IN,
        HANDLE_OUT,
        AXIS_X,
        AXIS_Y,
        AXIS_Z,
        RING_YAW,
        RING_PITCH
    }

    public record PickRay(
            Vec3 origin,
            Vec3 direction
    ) {
        ;;
    }

    public record PickResult(
            PickKind kind,
            int index,
            Vec3 position,
            double distanceAlongRay
    ) {
        ;;
    }

}