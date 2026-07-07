package dev.xylonity.knightlib.client.camera.editor;

import dev.xylonity.knightlib.api.camera.path.impl.CameraKeyframe;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable keyframe of the client-side editor session. Null tangents mean "auto" (the CR spline is drawn automatically)
 */
public final class EditorKeyframe {

    public Vec3 position;
    public float yaw;
    public float pitch;

    public int durationTicks;
    public KnightLibEasings easing;
    public boolean chained = false;

    @Nullable
    public Vec3 inTangent;
    @Nullable
    public Vec3 outTangent;
    public boolean linkedHandles = true;

    public EditorKeyframe(Vec3 position, float yaw, float pitch, int durationTicks, KnightLibEasings easing) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.durationTicks = durationTicks;
        this.easing = easing;
    }

    public boolean hasCustomTangents() {
        return inTangent != null || outTangent != null;
    }

    public void resetTangents() {
        inTangent = null;
        outTangent = null;
    }

    /**
     * Sets the outgoing tangent from a dragged handle position
     */
    public void dragOutHandle(Vec3 handlePosition) {
        final Vec3 tangent = handlePosition.subtract(position).scale(3.0);

        outTangent = tangent;
        if (linkedHandles) {
            inTangent = tangent;
        }

    }

    /**
     * Sets the incoming tangent from a dragged handle position
     */
    public void dragInHandle(Vec3 handlePosition) {
        final Vec3 tangent = position.subtract(handlePosition).scale(3.0);

        inTangent = tangent;
        if (linkedHandles) {
            outTangent = tangent;
        }

    }

    /**
     * Expands/shrinks the outgoing tangent in place
     */
    public void scaleOutTangent(double factor, Vec3 resolvedOut) {
        final Vec3 tangent = resolvedOut.scale(factor);

        outTangent = tangent;
        if (linkedHandles) {
            inTangent = tangent;
        }

    }

    /**
     * Expands/shrinks the incoming tangent in place
     */
    public void scaleInTangent(double factor, Vec3 resolvedIn) {
        final Vec3 tangent = resolvedIn.scale(factor);

        inTangent = tangent;
        if (linkedHandles) {
            outTangent = tangent;
        }

    }

    public CameraKeyframe toImmutable() {
        return new CameraKeyframe(position, yaw, pitch, durationTicks, easing, chained, inTangent, outTangent);
    }

}