package dev.xylonity.knightlib.client.animation.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xylonity.knightlib.client.animation.KnightLibPose;
import dev.xylonity.knightlib.mixin.ModelPartAccessor;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Wraps a baked {@link ModelPart} tree and drives it with vanilla conventions
 */
public final class VanillaModel extends KnightLibModel {

    public static final String ROOT = "__root";

    private final ModelPart root;
    private final Map<String, ModelPart> bones = new LinkedHashMap<>();
    private final Set<ModelPart> allParts = new LinkedHashSet<>();
    private final Map<ModelPart, Boolean> initialVisibility = new LinkedHashMap<>();
    private final Map<ModelPart, Boolean> initialSkipDraw = new LinkedHashMap<>();
    private final Set<String> boneNames;

    public VanillaModel(ModelPart root) {
        this.root = root;
        bones.put(ROOT, root);
        allParts.add(root);
        index(root);
        for (final ModelPart part : allParts) {
            initialVisibility.put(part, part.visible);
            initialSkipDraw.put(part, part.skipDraw);
        }

        boneNames = Set.copyOf(bones.keySet());
    }

    private void index(ModelPart part) {
        for (final Map.Entry<String, ModelPart> child : ((ModelPartAccessor) (Object) part).knightlib$getChildren().entrySet()) {
            allParts.add(child.getValue());
            bones.putIfAbsent(child.getKey(), child.getValue());
            index(child.getValue());
        }

    }

    @Override
    public Set<String> boneNames() {
        return boneNames;
    }

    @Override
    protected void visitEachBone(Consumer<String> visitor) {
        visitChildren(root, visitor, new HashSet<>());
    }

    private static void visitChildren(ModelPart parent, Consumer<String> visitor, Set<String> visited) {
        for (final Map.Entry<String, ModelPart> child : ((ModelPartAccessor) (Object) parent).knightlib$getChildren().entrySet()) {
            if (visited.add(child.getKey())) {
                visitor.accept(child.getKey());
            }

            visitChildren(child.getValue(), visitor, visited);
        }

    }

    @Override
    public boolean hasBone(String name) {
        return bones.containsKey(name);
    }

    @Override
    public void resetPose() {
        for (final ModelPart part : allParts) {
            part.resetPose();
            part.visible = initialVisibility.getOrDefault(part, true);
            part.skipDraw = initialSkipDraw.getOrDefault(part, false);
        }

    }

    @Override
    public void applyPosition(String bone, float x, float y, float z) {
        final ModelPart part = bones.get(bone);
        if (part == null) {
            return;
        }

        part.x += x;
        part.y += -y;
        part.z += z;
    }

    @Override
    public void applyRotation(String bone, float xDeg, float yDeg, float zDeg) {
        final ModelPart part = bones.get(bone);
        if (part == null) {
            return;
        }

        part.xRot += (float) Math.toRadians(xDeg);
        part.yRot += (float) Math.toRadians(yDeg);
        part.zRot += (float) Math.toRadians(zDeg);
    }

    @Override
    public void applyScale(String bone, float x, float y, float z) {
        final ModelPart part = bones.get(bone);
        if (part == null) {
            return;
        }

        part.xScale *= x;
        part.yScale *= y;
        part.zScale *= z;
    }

    @Override
    public void setBoneVisible(String bone, boolean visible) {
        final ModelPart part = bones.get(bone);
        if (part == null) {
            return;
        }

        part.visible = visible;
    }

    @Override
    public KnightLibPose capturePose() {
        final KnightLibPose pose = new KnightLibPose();
        for (final Map.Entry<String, ModelPart> entry : bones.entrySet()) {
            final ModelPart part = entry.getValue();
            final float[] values = pose.bone(entry.getKey());
            values[0] = part.x;
            values[1] = part.y;
            values[2] = part.z;
            values[3] = part.xRot;
            values[4] = part.yRot;
            values[5] = part.zRot;
            values[6] = part.xScale;
            values[7] = part.yScale;
            values[8] = part.zScale;
        }

        return pose;
    }

    @Override
    public void applyPose(KnightLibPose pose) {
        for (final Map.Entry<String, ModelPart> entry : bones.entrySet()) {
            final float[] values = pose.get(entry.getKey());
            if (values == null) {
                continue;
            }

            final ModelPart part = entry.getValue();
            part.x = values[0];
            part.y = values[1];
            part.z = values[2];
            part.xRot = values[3];
            part.yRot = values[4];
            part.zRot = values[5];
            part.xScale = values[6];
            part.yScale = values[7];
            part.zScale = values[8];
        }

    }

    @Override
    public void applyPoseDelta(KnightLibPose layer, KnightLibPose rest) {
        for (final String name : layer.boneNames()) {
            final ModelPart part = bones.get(name);
            final float[] values = layer.get(name);
            final float[] base = rest.get(name);
            if (part == null || values == null || base == null) {
                continue;
            }

            part.x += values[0] - base[0];
            part.y += values[1] - base[1];
            part.z += values[2] - base[2];
            part.xRot += values[3] - base[3];
            part.yRot += values[4] - base[4];
            part.zRot += values[5] - base[5];
            part.xScale *= scaleRatio(values[6], base[6]);
            part.yScale *= scaleRatio(values[7], base[7]);
            part.zScale *= scaleRatio(values[8], base[8]);
        }

    }

    private static float scaleRatio(float value, float rest) {
        return Math.abs(rest) < 1.0E-6f ? 1f : value / rest;
    }

    @Override
    public void blendFromPose(KnightLibPose from, float currentWeight) {
        for (final Map.Entry<String, ModelPart> entry : bones.entrySet()) {
            final float[] values = from.get(entry.getKey());
            if (values == null) {
                continue;
            }

            final ModelPart part = entry.getValue();
            part.x = Mth.lerp(currentWeight, values[0], part.x);
            part.y = Mth.lerp(currentWeight, values[1], part.y);
            part.z = Mth.lerp(currentWeight, values[2], part.z);
            part.xRot = lerpRadians(currentWeight, values[3], part.xRot);
            part.yRot = lerpRadians(currentWeight, values[4], part.yRot);
            part.zRot = lerpRadians(currentWeight, values[5], part.zRot);
            part.xScale = Mth.lerp(currentWeight, values[6], part.xScale);
            part.yScale = Mth.lerp(currentWeight, values[7], part.yScale);
            part.zScale = Mth.lerp(currentWeight, values[8], part.zScale);
        }

    }

    private static float lerpRadians(float weight, float from, float to) {
        final float delta = (float) Math.atan2(Math.sin(to - from), Math.cos(to - from));
        return from + delta * weight;
    }

    @Override
    public void setupRootTransform(PoseStack poseStack, float bodyYawDegrees, boolean entityContext) {
        if (entityContext) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - bodyYawDegrees));
            poseStack.scale(-1f, -1f, 1f);
            poseStack.translate(0f, -1.501f, 0f);
        }

    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        root.render(poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
    }

    @Override
    public void visitBones(PoseStack poseStack, Set<String> names, BoneVisitor visitor) {
        visitPart(ROOT, root, poseStack, names, visitor);
    }

    private void visitPart(String name, ModelPart part, PoseStack poseStack, Set<String> names, BoneVisitor visitor) {
        if (!part.visible) {
            return;
        }

        poseStack.pushPose();

        part.translateAndRotate(poseStack);
        if (names.contains(name) && (ROOT.equals(name) || bones.get(name) == part)) {
            visitor.visit(name, new org.joml.Matrix4f(poseStack.last().pose()), new org.joml.Matrix3f(poseStack.last().normal()));
        }

        for (final Map.Entry<String, ModelPart> child : ((ModelPartAccessor) (Object) part).knightlib$getChildren().entrySet()) {
            visitPart(child.getKey(), child.getValue(), poseStack, names, visitor);
        }

        poseStack.popPose();
    }

    @Override
    public Vec3 boneHalfExtents(String name) {
        final ModelPart part = bones.get(name);
        if (part == null) {
            return new Vec3(0.1, 0.1, 0.1);
        }

        double x = 0;
        double y = 0;
        double z = 0;
        for (final ModelPart.Cube cube : ((ModelPartAccessor) (Object) part).knightlib$getCubes()) {
            x = Math.max(x, Math.max(Math.abs(cube.minX), Math.abs(cube.maxX)) / 16.0);
            y = Math.max(y, Math.max(Math.abs(cube.minY), Math.abs(cube.maxY)) / 16.0);
            z = Math.max(z, Math.max(Math.abs(cube.minZ), Math.abs(cube.maxZ)) / 16.0);
        }

        return new Vec3(Math.max(x, 0.1), Math.max(y, 0.1), Math.max(z, 0.1));
    }

}