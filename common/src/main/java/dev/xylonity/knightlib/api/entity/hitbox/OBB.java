package dev.xylonity.knightlib.api.entity.hitbox;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Oriented Bounding Box (a rotation aware 3D box, unlike AABBs, which are aligned by axis).
 * Using the Separating Axis Theorem (SAT) for intersection tests.
 */
public class OBB {

    private Vec3 center;
    private final Vec3 halfExtents;
    private final Vector3f[] axes = new Vector3f[3];

    public OBB(final Vec3 center, final Vec3 halfExtents, final Matrix3f rotation) {
        this.center = center;
        this.halfExtents = halfExtents;
        this.axes[0] = rotation.getColumn(0, new Vector3f());
        this.axes[1] = rotation.getColumn(1, new Vector3f());
        this.axes[2] = rotation.getColumn(2, new Vector3f());
    }

    public Vec3 getCenter() {
        return center;
    }

    public Vec3 getHalfExtents() {
        return halfExtents;
    }

    public Vector3f[] getAxes() {
        return axes;
    }

    public void setCenter(Vec3 center) {
        this.center = center;
    }

    /**
     * Tests intersection with an axis-aligned bounding box using SAT.
     * An AABB is just an OBB with identity rotation, so I test 15 potential separating axes (3 from this OBB, 3 from
     * the AABB, and 9 cross products).
     */
    public boolean intersects(final AABB aabb) {
        final Vec3 aabbCenter = aabb.getCenter();
        final Vec3 aabbHalf = new Vec3(
                (aabb.maxX - aabb.minX) * 0.5,
                (aabb.maxY - aabb.minY) * 0.5,
                (aabb.maxZ - aabb.minZ) * 0.5
        );

        final Vector3f[] aabbAxes = {
                new Vector3f(1, 0, 0),
                new Vector3f(0, 1, 0),
                new Vector3f(0, 0, 1)
        };

        return satTest(aabbCenter, aabbHalf, aabbAxes);
    }

    /**
     * Tests intersection with another OBB using SAT
     */
    public boolean intersects(final OBB other) {
        return satTest(other.center, other.halfExtents, other.axes);
    }

    private boolean satTest(final Vec3 otherCenter, final Vec3 otherHalf, final Vector3f[] otherAxes) {
        final Vector3f center = new Vector3f(
                (float) (otherCenter.x - this.center.x),
                (float) (otherCenter.y - this.center.y),
                (float) (otherCenter.z - this.center.z)
        );

        final float[] aHalf = {
                (float) halfExtents.x,
                (float) halfExtents.y,
                (float) halfExtents.z
        };
        final float[] bHalf = {
                (float) otherHalf.x,
                (float) otherHalf.y,
                (float) otherHalf.z
        };

        // Tests 3 axes from this OBB
        for (int i = 0; i < 3; i++) {
            if (isSeparating(axes[i], center, aHalf, bHalf, otherAxes)) {
                return false;
            }

        }

        // Tests 3 axes from the other box
        for (int i = 0; i < 3; i++) {
            if (isSeparating(otherAxes[i], center, aHalf, bHalf, otherAxes)) {
                return false;
            }

        }

        // Tests 9 cross-product axes
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final Vector3f cross = new Vector3f();
                axes[i].cross(otherAxes[j], cross);
                if (cross.lengthSquared() < 1e-6f) {
                    // Parallel axes
                    continue;
                }

                cross.normalize();
                if (isSeparating(cross, center, aHalf, bHalf, otherAxes)) {
                    return false;
                }
            }

        }

        return true;
    }

    private boolean isSeparating(final Vector3f axis, final Vector3f center, float[] aHalf, float[] bHalf, final Vector3f[] otherAxes) {
        final float projection = Math.abs(axis.dot(center));

        float aExtent = 0;
        for (int i = 0; i < 3; i++) {
            aExtent += aHalf[i] * Math.abs(axis.dot(axes[i]));
        }

        float bExtent = 0;
        for (int i = 0; i < 3; i++) {
            bExtent += bHalf[i] * Math.abs(axis.dot(otherAxes[i]));
        }

        return projection > aExtent + bExtent;
    }

    /**
     * Expands the OBB half-extents by the given amount
     */
    public OBB inflate(double amount) {
        return new OBB(center, halfExtents.add(amount, amount, amount), toMatrix());
    }

    /**
     * Reconstructs the rotation matrix from the stored axes
     */
    public Matrix3f toMatrix() {
        final Matrix3f matrix = new Matrix3f();
        matrix.setColumn(0, axes[0]);
        matrix.setColumn(1, axes[1]);
        matrix.setColumn(2, axes[2]);
        return matrix;
    }

    /**
     * Computes the AABB that fully encloses this OBB
     */
    public AABB enclosingAABB() {
        float rx = 0;
        float ry = 0;
        float rz = 0;
        for (int i = 0; i < 3; i++) {
            final float h = (float) halfExtent(i);
            rx += Math.abs(axes[i].x()) * h;
            ry += Math.abs(axes[i].y()) * h;
            rz += Math.abs(axes[i].z()) * h;
        }

        return new AABB(
                center.x - rx, center.y - ry, center.z - rz,
                center.x + rx, center.y + ry, center.z + rz
        );
    }

    /**
     * Ray-OBB intersection test using the slab method in the OBB's local coordinate space.
     * Returns the distance along the ray to the hit point, or -1 if there is no intersection.
     *
     * @param start ray origin (world space)
     * @param end ray end (world space)
     * @return distance to intersection, or -1 if none
     */
    public double rayIntersects(final Vec3 start, final Vec3 end) {
        final Vec3 direction = end.subtract(start);
        final double rayLength = direction.length();
        if (rayLength < 1e-8) {
            return -1;
        }

        // Transforms the ray into the OBB local space
        final Vec3 delta = start.subtract(center);
        final double[] origins = {
                dot(delta, axes[0]),
                dot(delta, axes[1]),
                dot(delta, axes[2])
        };
        final double[] directions = {
                dot(direction, axes[0]),
                dot(direction, axes[1]),
                dot(direction, axes[2])
        };
        final double[] halfs = {
                halfExtents.x,
                halfExtents.y,
                halfExtents.z
        };

        double tMin = 0;
        double tMax = 1;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(directions[i]) < 1e-8) {
                // If the ray is parallel it must be inside
                if (origins[i] < -halfs[i] || origins[i] > halfs[i]) {
                    return -1;
                }

            }
            else {
                final double invD = 1.0 / directions[i];
                double t1 = (-halfs[i] - origins[i]) * invD;
                double t2 = (halfs[i] - origins[i]) * invD;
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }

                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) {
                    return -1;
                }
            }

        }

        return tMin * rayLength;
    }

    private static double dot(Vec3 vector, Vector3f axis) {
        return vector.x * axis.x() + vector.y * axis.y() + vector.z * axis.z();
    }

    /**
     * Returns the 8 corners of the OBB in world space
     */
    public Vec3[] getCorners() {
        final Vec3[] corners = new Vec3[8];
        final double hx = halfExtents.x;
        final double hy = halfExtents.y;
        final double hz = halfExtents.z;
        final Vec3 ax = toVec3(axes[0]);
        final Vec3 ay = toVec3(axes[1]);
        final Vec3 az = toVec3(axes[2]);

        int idx = 0;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    corners[idx++] = center
                            .add(ax.scale(sx * hx))
                            .add(ay.scale(sy * hy))
                            .add(az.scale(sz * hz));
                }
            }

        }

        return corners;
    }

    private static Vec3 toVec3(Vector3f vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }

    private double halfExtent(int i) {
        return switch (i) {
            case 0 -> halfExtents.x;
            case 1 -> halfExtents.y;
            case 2 -> halfExtents.z;
            default -> 0;
        };

    }

}
