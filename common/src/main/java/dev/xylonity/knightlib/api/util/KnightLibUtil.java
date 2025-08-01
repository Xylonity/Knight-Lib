package dev.xylonity.knightlib.api.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class KnightLibUtil {

    private KnightLibUtil() { ;; }

    /**
     * Checks if an entity is behind the specified one. The fov is discriminatory, not inclusive, so only
     * non-included angles are computed
     */
    public static boolean isEntityBehind(Entity from, Entity to, double fov) {
        if (from == null || to == null) return false;
        Vec3 toTarget = new Vec3(to.getX(), from.getY(), to.getZ()).subtract(from.position()).normalize();
        double angle = Math.acos(from.getLookAngle().normalize().dot(toTarget)) * (180.0 / Math.PI);
        return angle >= (fov / 2f);
    }

    public static @Nullable Entity raycastEntity(Entity exception, Level level, Vec3 start, Vec3 end) {
        return raycastEntity(exception, level, start, end, e -> true);
    }

    /**
     * Performs a raycast and return the first and only entity hit (or null if none)
     */
    public static @Nullable Entity raycastEntity(Entity exception, Level level, Vec3 start, Vec3 end, Predicate<Entity> predicate) {
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, exception, start, end, new AABB(start, end), predicate);
        if (hit == null) return null;
        return hit.getEntity();
    }

    public static <T extends Entity> Optional<T> getNearestEntity(Entity center, Class<T> entityType, double radius) {
        return getNearestEntity(center, entityType, radius, e -> true);
    }

    /**
     * Finds the nearest entity of the given type within the radius
     */
    public static <T extends Entity> Optional<T> getNearestEntity(Entity center, Class<T> entityType, double radius, Predicate<T> filter) {
        return center.level().getEntitiesOfClass(entityType, center.getBoundingBox().inflate(radius)).stream().filter(e -> !e.equals(center)).filter(filter).min(Comparator.comparingDouble(center::distanceToSqr));
    }

    /**
     * Returns entities within a cone in a given direction (just computes a conical segment UNDER the direction given)
     */
    public static List<Entity> getEntitiesInDirection(Entity from, Vec3 direction, double range, double angleRadians) {
        return from.level().getEntities(from, from.getBoundingBox().inflate(range), e -> {
            if (from.distanceTo(e) > range) return false;

            Vec3 flat = flattenXZ(e.position().subtract(from.position()));

            if (flat.equals(Vec3.ZERO)) return false;

            return (Math.acos(Math.max(-1, Math.min(1, flattenXZ(direction).dot(flat))))) <= angleRadians / 2;
        });
    }

    /**
     * Projects and normalizes a vector onto a XZ plane
     */
    public static Vec3 flattenXZ(Vec3 vector) {
        Vec3 flat = new Vec3(vector.x, 0, vector.z);
        return flat.lengthSqr() < 1e-6 ? Vec3.ZERO : flat.normalize();
    }

    /**
     * Tests a LOS between two entities
     */
    public static boolean hasLineOfSight(Entity from, Entity to, boolean ignoreWater, boolean ignoreLava) {
        HitResult result = from.level().clip(new ClipContext(from.getEyePosition(), to.getEyePosition(), ClipContext.Block.COLLIDER, (ignoreWater && ignoreLava) ? ClipContext.Fluid.NONE : ClipContext.Fluid.ANY, from));
        return result.getType() == HitResult.Type.MISS;
    }

    public static boolean hasLineOfSight(Entity from, Entity to) {
        return hasLineOfSight(from, to, true, true);
    }

    /**
     * Return a list of entities within a LOS of a viewer based on a given range and angle
     */
    public static List<Entity> getEntitiesInLineOfSight(LivingEntity from, double range, double angleRadians, Predicate<Entity> filter) {
        Vec3 eye = from.getEyePosition();
        return from.level()
                .getEntities(from, new AABB(eye, eye).inflate(range), e -> !e.equals(from) && filter.test(e))
                .stream()
                .filter(e -> {
                    Vec3 toTarget = (e instanceof LivingEntity living) ? living.getEyePosition() : e.getBoundingBox().getCenter().subtract(eye);
                    double dist = toTarget.length();

                    if (dist > range) return false;

                    return Math.acos(Math.max(-1, Math.min(1, from.getViewVector(1f).normalize().dot(toTarget.scale(1f / dist))))) <= angleRadians / 2;
                })
                .collect(Collectors.toList());
    }

    public static List<Entity> getEntitiesInLineOfSight(LivingEntity entity, double range) {
        return getEntitiesInLineOfSight(entity, range, Math.toRadians(60), e -> true);
    }

    /**
     * Checks if the source entity can see a target within a distance, fov and LOS
     */
    public static boolean canSee(LivingEntity from, Entity to, double maxDistance, double fovRad, boolean checkLOS) {
        if (from.distanceTo(to) > maxDistance) return false;

        Vec3 flat = flattenXZ(to.getEyePosition().subtract(from.getEyePosition()));

        if (flat.equals(Vec3.ZERO)) return false;

        return (Math.acos(Math.max(-1, Math.min(1, flattenXZ(from.getViewVector(1.0f)).dot(flat)))) <= fovRad / 2) && (!checkLOS || hasLineOfSight(from, to));
    }

    public static boolean canSee(LivingEntity from, Entity to, double maxDistance, double fovRad) {
        return canSee(from, to, maxDistance, fovRad, true);
    }

    /**
     * Predicts the position of an entity after a given number of ticks given a certain accel
     */
    public static Vec3 predictPosition(Entity entity, int ticks, Vec3 movement) {
        Vec3 vel = entity.getDeltaMovement();
        Vec3 pos = entity.position();
        return pos.add(vel.scale(ticks)).add(movement.scale(0.5 * (double) ticks * (double) ticks));
    }

    public static Vec3 predictPosition(Entity entity, int ticks) {
        return predictPosition(entity, ticks, entity.getDeltaMovement());
    }

    /**
     * Returns a list of entities within a circle on the XZ axis
     */
    public static List<Entity> getEntitiesInCircle(Level level, Vec3 center, double radius, Predicate<Entity> predicate) {
        return level.getEntities((Entity) null, new AABB(center, center).inflate(radius), e -> e.position().distanceTo(center) <= radius && predicate.test(e));
    }

    /**
     * Finds the closest ground position below the entity within a vertical distance
     */
    public static BlockPos findClosestGroundBelow(LivingEntity entity, float y) {
        Vec3 start = new Vec3(entity.getX(), entity.getBoundingBox().minY + 0.01, entity.getZ());
        BlockHitResult trace = entity.level().clip(new ClipContext(start, start.subtract(0, y, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));

        if (trace.getType() == HitResult.Type.BLOCK) {
            return trace.getBlockPos();
        } else {
            return null;
        }
    }

    /**
     * Normalize degrees within a 360 degree cap
     */
    public static float normalizeDeg(float deg) {
        deg %= 360f;
        return deg < 0 ? deg + 360f : deg;
    }

    /**
     * Degrees to radians fast parser
     */
    public static float degToRad(float deg) {
        return (float) Math.toRadians(deg);
    }

    /**
     * Cubical smoothstep
     */
    public static float smoothStep(float t) {
        return t * t * (3 - 2 * t);
    }

    /**
     * Bezier parabola
     */
    public static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, double t) {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;
        return new Vec3(uu * a.x + 2 * u * t * b.x + tt * c.x, uu * a.y + 2 * u * t * b.y + tt * c.y, uu * a.z + 2 * u * t * b.z + tt * c.z);
    }

    /**
     * Estimate length between a pair of nodes under a bezier parabola
     */
    public static double estimateLengthBezier(Vec3 a, Vec3 b, Vec3 c) {
        double len = 0;
        Vec3 prev = a;
        for (int i = 1; i <= 24; i++) {
            double t = i / (double) 24;
            Vec3 cur = bezier(a, b, c, t);
            len += cur.distanceTo(prev);
            prev = cur;
        }

        return len;
    }

    /**
     * Horizontal direction rotator within a specific direction
     */
    public static Vec3 rotateHorizontalDirection(Vec3 direction, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(direction.x * cos - direction.z * sin, direction.y, direction.x * sin + direction.z * cos);
    }

    /**
     * Finds a valid (empty) position on the vertical axis checking the top face of each block within a specific pos
     */
    public static BlockPos findValidSpawnPos(BlockPos pos, Level level) {
        if (isValidSpawnPos(pos, level)) return pos;

        for (int d = 1; d <= 6; d++) {
            BlockPos below = pos.below(d);
            if (isValidSpawnPos(below, level)) return below;
        }

        for (int u = 1; u <= 3; u++) {
            BlockPos above = pos.above(u);
            if (isValidSpawnPos(above, level)) return above;
        }

        return pos;
    }

    private static boolean isValidSpawnPos(BlockPos pos, Level level) {
        return level.isInWorldBounds(pos) && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    public static Vec3 randomVectorInCone(Vec3 base, double maxAngleDegrees, Random random) {
        Vec3 baseNorm = base.normalize();
        Vec3 u = baseNorm.cross(new Vec3(0, 1, 0));

        if (u.lengthSqr() < 1e-6) {
            u = baseNorm.cross(new Vec3(1, 0, 0));
        }

        u = u.normalize();
        Vec3 v = baseNorm.cross(u).normalize();

        double minCos = Math.cos(Math.toRadians(maxAngleDegrees));
        double cos = minCos + random.nextDouble() * (1.0 - minCos);
        double sin = Math.sqrt(1.0 - cos * cos);
        double phi = random.nextDouble() * 2 * Math.PI;
        return baseNorm.scale(cos).add(u.scale(sin * Math.cos(phi))).add(v.scale(sin * Math.sin(phi))).scale(base.length());
    }

    /**
     * NBT float array to listTag parser
     */
    public static ListTag floatsToList(float[] arr) {
        ListTag list = new ListTag();
        for (float v : arr) {
            list.add(FloatTag.valueOf(v));
        }

        return list;
    }

    /**
     * NBT int array to listTag parser
     */
    public static ListTag intsToList(int[] arr) {
        ListTag list = new ListTag();
        for (int v : arr) {
            list.add(IntTag.valueOf(v));
        }

        return list;
    }

    /**
     * NBT listTag floatTag to float array parser
     */
    public static void listToFloats(ListTag list, float[] o) {
        for (int i = 0; i < o.length && i < list.size(); i++) {
            o[i] = ((FloatTag) list.get(i)).getAsFloat();
        }

    }

    /**
     * NBT listTag intTag to int array parser
     */
    public static void listToInts(ListTag list, int[] o) {
        for (int i = 0; i < o.length && i < list.size(); i++) {
            o[i] = ((IntTag) list.get(i)).getAsInt();
        }

    }

}