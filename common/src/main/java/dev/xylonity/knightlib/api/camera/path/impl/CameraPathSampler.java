package dev.xylonity.knightlib.api.camera.path.impl;

import dev.xylonity.knightlib.api.util.KnightLibMath;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Time/space sampler of a keyframe list, shared by the playback ({@link CameraPathManager}) and the ingame editor, so
 * what is drawn is exactly what plays.
 */
public final class CameraPathSampler {

    private final List<CameraKeyframe> points;
    private final float[] segmentStarts;
    private final float travelTicks;
    private final float totalTicks;
    private final boolean smooth;

    private CameraPathSampler(List<CameraKeyframe> points, boolean smooth, int holdTicks) {
        this.points = List.copyOf(points);
        this.smooth = smooth;

        this.segmentStarts = new float[this.points.size()];

        float total = 0f;
        for (int i = 1; i < this.points.size(); i++) {
            segmentStarts[i] = total;
            total += this.points.get(i).durationTicks();
        }

        this.travelTicks = total;
        this.totalTicks = Math.max(1f, total + Math.max(0, holdTicks));
    }

    /**
     * The first keyframe is the starting shot (and it's ignored because there is no path at all)
     */
    public static CameraPathSampler of(List<CameraKeyframe> points, boolean smooth, int holdTicks) {
        if (points.isEmpty()) {
            throw new IllegalArgumentException("[KnightLib] Cannot sample an empty camera path");
        }

        return new CameraPathSampler(points, smooth, holdTicks);
    }

    public int keyframeCount() {
        return points.size();
    }

    public CameraKeyframe keyframe(int index) {
        return points.get(index);
    }

    /**
     * Ticks spent traveling across all segments, without the final hold
     */
    public float travelTicks() {
        return travelTicks;
    }

    public float totalTicks() {
        return totalTicks;
    }

    /**
     * Path time at which the travel toward the given keyframe starts
     */
    public float segmentStart(int index) {
        return segmentStarts[index];
    }

    public boolean isCut(int index) {
        return index > 0 && points.get(index).durationTicks() <= 0;
    }

    /**
     * Whether the segment ending at the given keyframe is chained to the previous one
     */
    public boolean chainedIntoPrevious(int index) {
        return index >= 2 && points.get(index).chained() && !isCut(index) && !isCut(index - 1);
    }

    /**
     * Full camera pose at the given path time, easing included. Times outside the path clamp to its ends
     * (the hold phase stays at the last keyframe). The Chained keyframes are eased as one unit (as
     * the first keyframe in the chain wins)
     */
    public Pose sample(float ticks) {
        if (points.size() == 1 || ticks <= 0f) {
            return Pose.of(points.get(0));
        }

        for (int i = 1; i < points.size(); i++) {
            final float duration = points.get(i).durationTicks();
            if (ticks > segmentStarts[i] + duration) {
                continue;
            }

            // Expands to the chained run this segment belongs to
            int first = i;
            while (chainedIntoPrevious(first)) {
                first--;
            }

            int last = i;
            while (last < points.size() - 1 && chainedIntoPrevious(last + 1)) {
                last++;
            }

            final float groupStart = segmentStarts[first];
            final float groupDuration = (segmentStarts[last] + points.get(last).durationTicks()) - groupStart;

            final float linear = groupDuration <= 0f ? 1f : Mth.clamp((ticks - groupStart) / groupDuration, 0f, 1f);
            final float easedTicks = points.get(first).easing().apply(linear) * groupDuration;

            return sampleGroup(first, last, easedTicks);
        }

        return Pose.of(points.get(points.size() - 1));
    }

    /**
     * Pose at an eased time offset within a chained run
     */
    private Pose sampleGroup(int first, int last, float easedTicks) {
        int segment = first;
        float offset = 0f;

        while (segment < last && easedTicks > offset + points.get(segment).durationTicks()) {
            offset += points.get(segment).durationTicks();
            segment++;
        }

        final float duration = points.get(segment).durationTicks();
        float progress = duration <= 0f ? 1f : (easedTicks - offset) / duration;

        if (segment != first) {
            progress = Math.max(0f, progress);
        }
        if (segment != last) {
            progress = Math.min(1f, progress);
        }

        final Vec3 position = positionOnSegment(segment, progress);
        final float yaw = Mth.rotLerp(progress, points.get(segment - 1).yaw(), points.get(segment).yaw());
        final float pitch = Mth.lerp(progress, points.get(segment - 1).pitch(), points.get(segment).pitch());

        return new Pose(position, yaw, pitch);
    }

    /**
     * Position-only sample along the segment that ends at the given keyframe (without easing)
     */
    public Vec3 positionOnSegment(int index, float progress) {
        final Vec3 from = points.get(index - 1).position();
        final Vec3 to = points.get(index).position();

        if (!smooth || isCut(index)) {
            return from.lerp(to, progress);
        }

        return KnightLibMath.hermite(from, outTangent(index - 1), to, inTangent(index), progress);
    }

    /**
     * Resolved curve velocity leaving the given keyframe
     */
    public Vec3 outTangent(int index) {
        final Vec3 custom = points.get(index).outTangent();
        if (custom != null) {
            return custom;
        }

        return autoTangent(index);
    }

    /**
     * Resolved curve velocity arriving at the given keyframe
     */
    public Vec3 inTangent(int index) {
        final Vec3 custom = points.get(index).inTangent();
        if (custom != null) {
            return custom;
        }

        return autoTangent(index);
    }

    /**
     * CR style centered difference, falling back to one-sided at the path ends and at cut boundaries
     */
    private Vec3 autoTangent(int index) {
        final Vec3 current = points.get(index).position();

       final boolean hasPrevious = index > 0 && !isCut(index);
       final boolean hasNext = index < points.size() - 1 && !isCut(index + 1);

        if (hasPrevious && hasNext) {
            return points.get(index + 1).position().subtract(points.get(index - 1).position()).scale(0.5);
        }
        if (hasNext) {
            return points.get(index + 1).position().subtract(current).scale(0.5);
        }
        if (hasPrevious) {
            return current.subtract(points.get(index - 1).position()).scale(0.5);
        }

        return Vec3.ZERO;
    }

    public record Pose(
            Vec3 position,
            float yaw,
            float pitch
    ) {

        static Pose of(CameraKeyframe keyframe) {
            return new Pose(keyframe.position(), keyframe.yaw(), keyframe.pitch());
        }

    }

}