package dev.xylonity.knightlib.client.animation.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Mutable bone of the geo backend. Position is stored in model units (16ths of a block) relative to the parent pivot
 */
public final class GeoBone {

    public final String name;

    private final float restX;
    private final float restY;
    private final float restZ;
    private final float restRotX;
    private final float restRotY;
    private final float restRotZ;
    private final boolean restVisible;

    public float x;
    public float y;
    public float z;
    public float rotX;
    public float rotY;
    public float rotZ;
    public float scaleX = 1f;
    public float scaleY = 1f;
    public float scaleZ = 1f;
    public boolean visible = true;

    private final List<GeoCube> cubes;
    private final List<GeoBone> children = new ArrayList<>();

    public GeoBone(String name, float restX, float restY, float restZ, float restRotX, float restRotY, float restRotZ, boolean restVisible, List<GeoCube> cubes) {
        this.name = name;
        this.restX = restX;
        this.restY = restY;
        this.restZ = restZ;
        this.restRotX = restRotX;
        this.restRotY = restRotY;
        this.restRotZ = restRotZ;
        this.restVisible = restVisible;
        this.cubes = cubes;

        reset();
    }

    public void addChild(GeoBone child) {
        children.add(child);
    }

    void forEachBone(Consumer<String> visitor) {
        visitor.accept(name);
        for (final GeoBone child : children) {
            child.forEachBone(visitor);
        }

    }

    public List<GeoBone> children() {
        return Collections.unmodifiableList(children);
    }

    public List<GeoCube> cubes() {
        return Collections.unmodifiableList(cubes);
    }

    public Vec3 halfExtents() {
        double x = 0;
        double y = 0;
        double z = 0;
        for (final GeoCube cube : cubes) {
            final Vec3 extents = cube.absoluteExtents();
            x = Math.max(x, extents.x);
            y = Math.max(y, extents.y);
            z = Math.max(z, extents.z);
        }

        return cubes.isEmpty() ? new Vec3(0.1, 0.1, 0.1) : new Vec3(x, y, z);
    }

    public void reset() {
        x = restX;
        y = restY;
        z = restZ;
        rotX = restRotX;
        rotY = restRotY;
        rotZ = restRotZ;
        scaleX = 1f;
        scaleY = 1f;
        scaleZ = 1f;
        visible = restVisible;
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        if (!visible) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x / 16f, y / 16f, z / 16f);
        if (rotX != 0f || rotY != 0f || rotZ != 0f) {
            poseStack.mulPose(new Quaternionf().rotationZYX(
                    (float) Math.toRadians(rotZ),
                    (float) Math.toRadians(rotY),
                    (float) Math.toRadians(rotX)
            ));

        }

        if (scaleX != 1f || scaleY != 1f || scaleZ != 1f) {
            poseStack.scale(scaleX, scaleY, scaleZ);
        }

        for (final GeoCube cube : cubes) {
            cube.render(poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
        }

        for (final GeoBone child : children) {
            child.render(poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
        }

        poseStack.popPose();
    }

    public void visit(PoseStack poseStack, Set<String> names, KnightLibModel.BoneVisitor visitor) {
        if (!visible) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(x / 16f, y / 16f, z / 16f);
        if (rotX != 0f || rotY != 0f || rotZ != 0f) {
            poseStack.mulPose(new Quaternionf().rotationZYX(
                    (float) Math.toRadians(rotZ),
                    (float) Math.toRadians(rotY),
                    (float) Math.toRadians(rotX)
            ));

        }
        if (scaleX != 1f || scaleY != 1f || scaleZ != 1f) {
            poseStack.scale(scaleX, scaleY, scaleZ);
        }

        if (names.contains(name)) {
            visitor.visit(name, new Matrix4f(poseStack.last().pose()), new Matrix3f(poseStack.last().normal()));
        }
        for (final GeoBone child : children) {
            child.visit(poseStack, names, visitor);
        }

        poseStack.popPose();
    }

}