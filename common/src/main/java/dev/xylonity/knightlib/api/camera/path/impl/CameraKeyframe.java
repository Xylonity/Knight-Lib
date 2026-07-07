package dev.xylonity.knightlib.api.camera.path.impl;

import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A single point of a {@link CameraPath}.
 *
 * @param position absolute world position of the camera
 * @param yaw camera yaw in degrees
 * @param pitch camera pitch in degrees
 * @param durationTicks time to travel from the previous keyframe to this one
 * @param easing easing applied to the travel of this segment (for chained runs, the easing of the run's first segment wins)
 * @param chained whether the easing flows through this keyframe from the previous segment
 * @param inTangent velocity of the curve arriving at this keyframe
 * @param outTangent velocity of the curve leaving this keyframe
 */
public record CameraKeyframe(
        Vec3 position,
        float yaw,
        float pitch,
        int durationTicks,
        KnightLibEasings easing,
        boolean chained,
        @Nullable Vec3 inTangent,
        @Nullable Vec3 outTangent
) {

    public CameraKeyframe {
        durationTicks = Math.max(0, durationTicks);
        easing = (easing == null) ? KnightLibEasings.SMOOTHSTEP : easing;
    }

    public CameraKeyframe(Vec3 position, float yaw, float pitch, int durationTicks, KnightLibEasings easing) {
        this(position, yaw, pitch, durationTicks, easing, false, null, null);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);

        buf.writeFloat(yaw);
        buf.writeFloat(pitch);

        buf.writeVarInt(durationTicks);
        buf.writeVarInt(easing.id());
        buf.writeBoolean(chained);

        encodeTangent(buf, inTangent);
        encodeTangent(buf, outTangent);
    }

    public static CameraKeyframe decode(FriendlyByteBuf buf) {
        final Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());

        final float yaw = buf.readFloat();
        final float pitch = buf.readFloat();

        final int durationTicks = buf.readVarInt();
        final KnightLibEasings easing = KnightLibEasings.byId(buf.readVarInt());
        final boolean chained = buf.readBoolean();

        final Vec3 inTangent = decodeTangent(buf);
        final Vec3 outTangent = decodeTangent(buf);

        return new CameraKeyframe(position, yaw, pitch, durationTicks, easing, chained, inTangent, outTangent);
    }

    private static void encodeTangent(FriendlyByteBuf buf, @Nullable Vec3 tangent) {
        buf.writeBoolean(tangent != null);
        if (tangent != null) {
            buf.writeDouble(tangent.x);
            buf.writeDouble(tangent.y);
            buf.writeDouble(tangent.z);
        }

    }

    @Nullable
    private static Vec3 decodeTangent(FriendlyByteBuf buf) {
        return buf.readBoolean() ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
    }

}