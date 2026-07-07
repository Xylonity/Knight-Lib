package dev.xylonity.knightlib.api.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class KnightLibMath {

    private static final double TAU = Math.PI * 2.0;
    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final double RAD_TO_DEG = 180.0 / Math.PI;
    private static final float EPSILON = 1.0E-6F;

    private KnightLibMath() {
        ;;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp01(float value) {
        return clamp(value, 0.0F, 1.0F);
    }

    public static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    /**
     * Linear interpolation between {@code a} and {@code b}
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Returns the {@code t} that produces {@code value} between {@code a} and {@code b}
     */
    public static float inverseLerp(float a, float b, float value) {
        if (Math.abs(b - a) < EPSILON) {
            return 0;
        }

        return (value - a) / (b - a);
    }

    public static double inverseLerp(double a, double b, double value) {
        if (Math.abs(b - a) < EPSILON) {
            return 0;
        }

        return (value - a) / (b - a);
    }

    /**
     * Remaps {@code value} from range [inMin, inMax] to [outMin, outMax]
     */
    public static float remap(float value, float inputMin, float inputMax, float outputMin, float outputMax) {
        final float t = inverseLerp(inputMin, inputMax, value);
        return lerp(outputMin, outputMax, t);
    }

    public static double remap(double value, double inputMin, double inputMax, double outputMin, double outputMax) {
        final double t = inverseLerp(inputMin, inputMax, value);
        return lerp(outputMin, outputMax, t);
    }

    /**
     * Angle of a horizontal offset
     */
    public static float yawAngleOf(Vec3 offset) {
        return (float) Math.toDegrees(Math.atan2(-offset.x, offset.z));
    }

    /**
     * Clamped lerp, where {@code t} is clamped to [0, 1] before interpolation
     */
    public static float clampedLerp(float a, float b, float t) {
        return lerp(a, b, clamp01(t));
    }

    public static double clampedLerp(double a, double b, double t) {
        return lerp(a, b, clamp01(t));
    }

    /**
     * Linearly interpolates a Vec3
     */
    public static Vec3 lerpVec3(Vec3 a, Vec3 b, double t) {
        return new Vec3(
                lerp(a.x, b.x, t),
                lerp(a.y, b.y, t),
                lerp(a.z, b.z, t)
        );

    }

    public static float toRadians(float degrees) {
        return (float) (degrees * DEG_TO_RAD);
    }

    public static float toDegrees(float radians) {
        return (float) (radians * RAD_TO_DEG);
    }

    public static double toRadians(double degrees) {
        return degrees * DEG_TO_RAD;
    }

    public static double toDegrees(double radians) {
        return radians * RAD_TO_DEG;
    }

    /**
     * Wraps a degree value into [-180, 180)
     */
    public static float wrapDegrees(float degrees) {
        float outputDegrees = degrees % 360F;
        if (outputDegrees >= 180F) {
            outputDegrees -= 360F;
        }
        if (outputDegrees < -180F) {
            outputDegrees += 360F;
        }

        return outputDegrees;
    }

    public static double wrapDegrees(double degrees) {
        double outputDegrees = degrees % 360.0;
        if (outputDegrees >= 180.0) {
            outputDegrees -= 360.0;
        }
        if (outputDegrees < -180.0) {
            outputDegrees += 360.0;
        }

        return outputDegrees;
    }

    /**
     * Shortest signed angle difference between two degree values
     */
    public static float angleDelta(float from, float to) {
        return wrapDegrees(to - from);
    }

    /**
     * Rotates {@code from} towards {@code to} by at most {@code maxStep} degrees
     */
    public static float approachAngle(float from, float to, float maxStep) {
        final float delta = angleDelta(from, to);
        return from + clamp(delta, -maxStep, maxStep);
    }

    /**
     * Lerp between two angles in degrees, taking the shortest path
     */
    public static float lerpAngle(float a, float b, float t) {
        return a + angleDelta(a, b) * t;
    }

    /**
     * Squared distance between two points (avoids sqrt)
     */
    public static double distanceSqr(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    public static double distanceSqr(Vec3 a, Vec3 b) {
        return distanceSqr(a.x, a.y, a.z, b.x, b.y, b.z);
    }

    public static double distanceSqr(BlockPos a, BlockPos b) {
        return distanceSqr(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    /**
     * Euclidean distance between two points
     */
    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(distanceSqr(x1, y1, z1, x2, y2, z2));
    }

    public static double distance(Vec3 a, Vec3 b) {
        return Math.sqrt(distanceSqr(a, b));
    }

    public static double distance(BlockPos a, BlockPos b) {
        return Math.sqrt(distanceSqr(a, b));
    }

    /**
     * 2D (XZ) horizontal distance squared
     */
    public static double horizontalDistanceSqr(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return dx * dx + dz * dz;
    }

    public static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        return horizontalDistanceSqr(a.x, a.z, b.x, b.z);
    }

    public static double horizontalDistance(Vec3 a, Vec3 b) {
        return Math.sqrt(horizontalDistanceSqr(a, b));
    }

    /**
     * Returns a normalized direction vector from {@code from} to {@code to}
     */
    public static Vec3 direction(Vec3 from, Vec3 to) {
        final Vec3 difference = to.subtract(from);
        final double length = difference.length();
        return length < EPSILON ? Vec3.ZERO : difference.scale(1.0 / length);
    }

    /**
     * Projects a point {@code p} onto the line segment from {@code a} to {@code b}
     */
    public static Vec3 projectOntoSegment(Vec3 point, Vec3 a, Vec3 b) {
        final Vec3 ab = b.subtract(a);
        final double lengthSqr = ab.lengthSqr();
        if (lengthSqr < EPSILON) {
            return a;
        }

        final double t = clamp01(point.subtract(a).dot(ab) / lengthSqr);
        return a.add(ab.scale(t));
    }

    /**
     * Rotates a point around the Y axis by {@code angleDeg} degrees relative to {@code center}
     */
    public static Vec3 rotateAroundY(Vec3 point, Vec3 center, double angleDeg) {
        final double rad = toRadians(angleDeg);
        final double cos = Math.cos(rad);
        final double sin = Math.sin(rad);
        final double dx = point.x - center.x;
        final double dz = point.z - center.z;
        return new Vec3(
                center.x + dx * cos - dz * sin,
                point.y,
                center.z + dx * sin + dz * cos
        );

    }

    /**
     *  Random int in [min, max] (inclusive)
     */
    public static int randomInt(final RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    public static int randomInt(int min, int max) {
        final Random random = ThreadLocalRandom.current();
        return min + random.nextInt(max - min + 1);
    }

    /**
     * Random float in [min, max)
     */
    public static float randomFloat(final RandomSource random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    public static float randomFloat(float min, float max) {
        final Random random = ThreadLocalRandom.current();
        return min + random.nextFloat() * (max - min);
    }

    /**
     * Random double in [min, max)
     */
    public static double randomDouble(final RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    public static double randomDouble(double min, double max) {
        final Random random = ThreadLocalRandom.current();
        return min + random.nextDouble() * (max - min);
    }

    /**
     * Returns a random point on the unit sphere
     */
    public static Vec3 randomOnUnitSphere(final RandomSource random) {
        final double theta = random.nextDouble() * TAU;
        final double phi = Math.acos(2.0 * random.nextDouble() - 1.0);
        final double sinPhi = Math.sin(phi);
        return new Vec3(sinPhi * Math.cos(theta), sinPhi * Math.sin(theta), Math.cos(phi));
    }

    /**
     * Returns a random point inside a sphere of given radius
     */
    public static Vec3 randomInSphere(final RandomSource random, double radius) {
        return randomOnUnitSphere(random).scale(radius * Math.cbrt(random.nextDouble()));
    }

    /**
     * Returns a random horizontal direction (XZ) as a unit Vec3
     */
    public static Vec3 randomHorizontalDirection(final RandomSource random) {
        final double angle = random.nextDouble() * TAU;
        return new Vec3(Math.cos(angle), 0, Math.sin(angle));
    }

    /**
     * Returns true with the given probability [0..1]
     */
    public static boolean chance(final RandomSource random, float probability) {
        return random.nextFloat() < probability;
    }

    public static boolean chance(float probability) {
        final Random random = ThreadLocalRandom.current();
        return random.nextFloat() < probability;
    }

    /**
     * Returns 0 if value < edge, else 1
     */
    public static float step(float edge, float value) {
        return value < edge ? 0F : 1F;
    }

    /**
     * Returns true if the value is a power of two
     */
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    /**
     * Maps a tick count + partial tick into a [0, 1] progress over {@code durationTicks}
     */
    public static float tickProgress(int tickCount, float partialTick, int durationTicks) {
        return clamp01((tickCount + partialTick) / durationTicks);
    }

    /**
     * Computes a Hermite spline with the given params
     */
    public static Vec3 hermite(Vec3 from, Vec3 outTangent, Vec3 to, Vec3 inTangent, float t) {
        final float t2 = t * t;
        final float t3 = t2 * t;

        final float h00 = 2f * t3 - 3f * t2 + 1f;
        final float h10 = t3 - 2f * t2 + t;
        final float h01 = -2f * t3 + 3f * t2;
        final float h11 = t3 - t2;

        return new Vec3(
                h00 * from.x + h10 * outTangent.x + h01 * to.x + h11 * inTangent.x,
                h00 * from.y + h10 * outTangent.y + h01 * to.y + h11 * inTangent.y,
                h00 * from.z + h10 * outTangent.z + h01 * to.z + h11 * inTangent.z
        );

    }

}