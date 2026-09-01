package dev.xylonity.knightlib.client.animation.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.world.phys.Vec3;

/**
 * Immutable baked cube geometry. By the time a cube gets here means it's ready for rendering as almost every pass is applied.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/cache/object/GeoCube.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/cache/object/GeoQuad.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/cache/object/GeoVertex.java
 */
public final class GeoCube {

    private final Face[] faces;
    private final Vec3 absoluteExtents;
    private final boolean flatX;
    private final boolean flatY;
    private final boolean flatZ;

    public GeoCube(Face[] faces) {
        this(faces, 1f, 1f, 1f);
    }

    public GeoCube(Face[] faces, float authoredSizeX, float authoredSizeY, float authoredSizeZ) {
        this.faces = faces;
        this.flatX = authoredSizeX == 0f;
        this.flatY = authoredSizeY == 0f;
        this.flatZ = authoredSizeZ == 0f;
        double x = 0;
        double y = 0;
        double z = 0;
        for (final Face face : faces) {
            for (final Vertex vertex : face.vertices()) {
                final Vector3f point = vertex.position();
                x = Math.max(x, Math.abs(point.x()));
                y = Math.max(y, Math.abs(point.y()));
                z = Math.max(z, Math.abs(point.z()));
            }

        }

        this.absoluteExtents = new Vec3(x, y, z);
    }

    public Vec3 absoluteExtents() {
        return absoluteExtents;
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        final Matrix4f pose = poseStack.last().pose();
        final Matrix3f normalMatrix = poseStack.last().normal();
        final Vector3f scratch = new Vector3f();

        for (final Face face : faces) {
            final Vector3f normal = normalMatrix.transform(face.normal().x(), face.normal().y(), face.normal().z(), scratch);
            fixInvertedFlatNormal(normal);

            for (final Vertex vertex : face.vertices()) {
                final Vector3f position = vertex.position();
                consumer.vertex(pose, position.x(), position.y(), position.z());
                consumer.color(r, g, b, a);
                consumer.uv(vertex.u(), vertex.v());
                consumer.overlayCoords(packedOverlay);
                consumer.uv2(packedLight);
                consumer.normal(normal.x(), normal.y(), normal.z());
                consumer.endVertex();
            }

        }

    }

    /**
     * Lighting correction for zero-thickness cubes
     */
    void fixInvertedFlatNormal(Vector3f normal) {
        if (normal.x() < 0f && (flatY || flatZ)) {
            normal.mul(-1f, 1f, 1f);
        }
        if (normal.y() < 0f && (flatX || flatZ)) {
            normal.mul(1f, -1f, 1f);
        }
        if (normal.z() < 0f && (flatX || flatY)) {
            normal.mul(1f, 1f, -1f);
        }

    }

    public record Face(
            Vector3f normal,
            Vertex[] vertices
    ) {
        ;;
    }

    public record Vertex(
            Vector3f position,
            float u,
            float v
    ) {
        ;;
    }

}