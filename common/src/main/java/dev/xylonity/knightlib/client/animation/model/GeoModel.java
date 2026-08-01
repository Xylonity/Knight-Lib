package dev.xylonity.knightlib.client.animation.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.knightlib.client.animation.KnightLibPose;
import dev.xylonity.knightlib.client.animation.geo.GeoModelDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Mutable geo bone tree instantiated from a parsed {@link GeoModelDefinition}
 */
public final class GeoModel extends KnightLibModel {

    public static final String ROOT = "__root";

    private final GeoBone root;
    private final Map<String, GeoBone> bones = new LinkedHashMap<>();
    private final Set<String> boneNames;

    public GeoModel(GeoModelDefinition definition) {
        this.root = new GeoBone(ROOT, 0f, 0f, 0f, 0f, 0f, 0f, true, java.util.List.of());
        bones.put(ROOT, root);

        for (final GeoModelDefinition.BoneDefinition boneDefinition : definition.bones()) {
            final GeoBone bone = new GeoBone(
                    boneDefinition.name(),
                    boneDefinition.offsetX(), boneDefinition.offsetY(), boneDefinition.offsetZ(),
                    boneDefinition.rotX(), boneDefinition.rotY(), boneDefinition.rotZ(),
                    boneDefinition.visible(),
                    boneDefinition.cubes()
            );

            bones.put(bone.name, bone);
        }

        for (final GeoModelDefinition.BoneDefinition boneDefinition : definition.bones()) {
            final GeoBone bone = bones.get(boneDefinition.name());
            final GeoBone parent = boneDefinition.parent().isEmpty() ? root : bones.get(boneDefinition.parent());
            if (parent == null) {
                throw new IllegalStateException("[KnightLib] Bone '" + boneDefinition.name() + "' references unknown parent '" + boneDefinition.parent() + "'");
            }

            parent.addChild(bone);
        }

        boneNames = Set.copyOf(bones.keySet());

    }

    public GeoBone bone(String name) {
        return bones.get(name);
    }

    @Override
    public Set<String> boneNames() {
        return boneNames;
    }

    @Override
    protected void visitEachBone(Consumer<String> visitor) {
        for (final GeoBone child : root.children()) {
            child.forEachBone(visitor);
        }

    }

    @Override
    public boolean hasBone(String name) {
        return bones.containsKey(name);
    }

    @Override
    public void resetPose() {
        for (final GeoBone bone : bones.values()) {
            bone.reset();
        }

    }

    @Override
    public void applyPosition(String bone, float x, float y, float z) {
        final GeoBone target = bones.get(bone);
        if (target == null) {
            return;
        }

        target.x += -x;
        target.y += y;
        target.z += z;
    }

    @Override
    public void applyRotation(String bone, float xDeg, float yDeg, float zDeg) {
        final GeoBone target = bones.get(bone);
        if (target == null) {
            return;
        }

        target.rotX += -xDeg;
        target.rotY += -yDeg;
        target.rotZ += zDeg;
    }

    @Override
    public void applyScale(String bone, float x, float y, float z) {
        final GeoBone target = bones.get(bone);
        if (target == null) {
            return;
        }

        target.scaleX *= x;
        target.scaleY *= y;
        target.scaleZ *= z;
    }

    @Override
    public void setBoneVisible(String bone, boolean visible) {
        final GeoBone target = bones.get(bone);
        if (target == null) {
            return;
        }

        target.visible = visible;
    }

    @Override
    public KnightLibPose capturePose() {
        final KnightLibPose pose = new KnightLibPose();
        for (final GeoBone bone : bones.values()) {
            final float[] values = pose.bone(bone.name);
            values[0] = bone.x;
            values[1] = bone.y;
            values[2] = bone.z;
            values[3] = bone.rotX;
            values[4] = bone.rotY;
            values[5] = bone.rotZ;
            values[6] = bone.scaleX;
            values[7] = bone.scaleY;
            values[8] = bone.scaleZ;
        }

        return pose;
    }

    @Override
    public void applyPose(KnightLibPose pose) {
        for (final GeoBone bone : bones.values()) {
            final float[] values = pose.get(bone.name);
            if (values == null) {
                continue;
            }

            bone.x = values[0];
            bone.y = values[1];
            bone.z = values[2];
            bone.rotX = values[3];
            bone.rotY = values[4];
            bone.rotZ = values[5];
            bone.scaleX = values[6];
            bone.scaleY = values[7];
            bone.scaleZ = values[8];
        }

    }

    @Override
    public void applyPoseDelta(KnightLibPose layer, KnightLibPose rest) {
        for (final String name : layer.boneNames()) {
            final GeoBone bone = bones.get(name);
            final float[] values = layer.get(name);
            final float[] base = rest.get(name);
            if (bone == null || values == null || base == null) {
                continue;
            }

            bone.x += values[0] - base[0];
            bone.y += values[1] - base[1];
            bone.z += values[2] - base[2];
            bone.rotX += values[3] - base[3];
            bone.rotY += values[4] - base[4];
            bone.rotZ += values[5] - base[5];
            bone.scaleX *= scaleRatio(values[6], base[6]);
            bone.scaleY *= scaleRatio(values[7], base[7]);
            bone.scaleZ *= scaleRatio(values[8], base[8]);
        }

    }

    private static float scaleRatio(float value, float rest) {
        return Math.abs(rest) < 1.0E-6f ? 1f : value / rest;
    }

    @Override
    public void blendFromPose(KnightLibPose from, float currentWeight) {
        for (final GeoBone bone : bones.values()) {
            final float[] values = from.get(bone.name);
            if (values == null) {
                continue;
            }

            bone.x = Mth.lerp(currentWeight, values[0], bone.x);
            bone.y = Mth.lerp(currentWeight, values[1], bone.y);
            bone.z = Mth.lerp(currentWeight, values[2], bone.z);
            bone.rotX = Mth.rotLerp(currentWeight, values[3], bone.rotX);
            bone.rotY = Mth.rotLerp(currentWeight, values[4], bone.rotY);
            bone.rotZ = Mth.rotLerp(currentWeight, values[5], bone.rotZ);
            bone.scaleX = Mth.lerp(currentWeight, values[6], bone.scaleX);
            bone.scaleY = Mth.lerp(currentWeight, values[7], bone.scaleY);
            bone.scaleZ = Mth.lerp(currentWeight, values[8], bone.scaleZ);
        }

    }

    @Override
    public void setupRootTransform(PoseStack poseStack, float bodyYawDegrees, boolean entityContext) {
        if (entityContext) {
            root.rotY += 180f - bodyYawDegrees;
        }

    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        root.render(poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
    }

    @Override
    public void renderLiving(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        poseStack.pushPose();
        poseStack.translate(0f, 1.501f, 0f);
        poseStack.scale(-1f, -1f, 1f);
        render(poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
        poseStack.popPose();
    }

    @Override
    public void visitBones(PoseStack poseStack, Set<String> names, BoneVisitor visitor) {
        root.visit(poseStack, names, visitor);
    }

    @Override
    public void visitLivingBones(PoseStack poseStack, Set<String> names, BoneVisitor visitor) {
        poseStack.pushPose();
        poseStack.translate(0f, 1.501f, 0f);
        poseStack.scale(-1f, -1f, 1f);
        visitBones(poseStack, names, visitor);
        poseStack.popPose();
    }

    @Override
    public Vec3 boneHalfExtents(String name) {
        final GeoBone bone = bones.get(name);
        return bone == null ? new Vec3(0.1, 0.1, 0.1) : bone.halfExtents();
    }

}
