package dev.xylonity.knightlib.client.animation.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.knightlib.client.animation.KnightLibPose;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Poseable model implementation driven by {@code KnightLibAnimator}. Geo bones store degrees and model units, while
 * vanilla parts store radians and their own pivot convention.
 *
 * <p>The two built-in implementations are {@link GeoModel} and {@link VanillaModel}.</p>
 */
public abstract class KnightLibModel {

    public abstract Set<String> boneNames();

    /**
     * Visits every bone in the parent-first tree order when the backend exposes a hierarchy
     */
    public final void forEachBone(Consumer<String> visitor) {
        visitEachBone(Objects.requireNonNull(visitor, "visitor"));
    }

    protected void visitEachBone(Consumer<String> visitor) {
        boneNames().forEach(visitor);
    }

    public abstract boolean hasBone(String name);

    /**
     * Restores every bone to its rest pose
     */
    public abstract void resetPose();

    public abstract void applyPosition(String bone, float x, float y, float z);

    /**
     * Rotation offsets in degrees
     */
    public abstract void applyRotation(String bone, float xDeg, float yDeg, float zDeg);

    public abstract void applyScale(String bone, float x, float y, float z);

    /**
     * Shows or hides a bone (children included)
     */
    public abstract void setBoneVisible(String bone, boolean visible);

    public abstract KnightLibPose capturePose();

    /**
     * Replaces the current transform of every bone present in the pose
     */
    public abstract void applyPose(KnightLibPose pose);

    /**
     * Composes the transform delta between layer and rest poses onto the current pose
     */
    public abstract void applyPoseDelta(KnightLibPose layer, KnightLibPose rest);

    /**
     * Blends the current pose towards {@code from}. At 0 weight the model shows {@code from} exactly, at
     * 1 the current pose is untouched
     */
    public abstract void blendFromPose(KnightLibPose from, float currentWeight);

    /**
     * Orients the model into render space
     */
    public abstract void setupRootTransform(PoseStack poseStack, float bodyYawDegrees, boolean entityContext);

    public abstract void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float r, float g, float b, float a);

    /**
     * Renders from inside vanilla's model frame
     */
    public void renderLiving(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        render(poseStack, consumer, packedLight, packedOverlay, r, g, b, a);
    }

    /**
     * Visits selected bones using their current model-space matrices
     */
    public abstract void visitBones(PoseStack poseStack, Set<String> names, BoneVisitor visitor);

    /**
     * Visits every bone with its current model-space pose and normal matrices
     */
    public final void visitBones(PoseStack poseStack, BoneVisitor visitor) {
        final Set<String> names = new LinkedHashSet<>();
        forEachBone(names::add);
        visitBones(Objects.requireNonNull(poseStack, "poseStack"), names, Objects.requireNonNull(visitor, "visitor"));
    }

    /**
     * Visits bones from inside vanilla's living-model coordinate frame
     */
    public void visitLivingBones(PoseStack poseStack, Set<String> names, BoneVisitor visitor) {
        visitBones(poseStack, names, visitor);
    }

    /**
     * Visits every bone from inside vanilla's living-model coordinate frame
     */
    public final void visitLivingBones(PoseStack poseStack, BoneVisitor visitor) {
        final Set<String> names = new LinkedHashSet<>();
        forEachBone(names::add);
        visitLivingBones(Objects.requireNonNull(poseStack, "poseStack"), names, Objects.requireNonNull(visitor, "visitor"));
    }

    /**
     * Local half-extents enclosing the named bone's cubes in blocks
     */
    public abstract Vec3 boneHalfExtents(String name);

    public final void walk(String bone, float speed, float degree, boolean invert, float offset, float weight, float limbSwing, float limbSwingAmount) {
        final float direction = invert ? -1f : 1f;
        final float angleRadians = ((float) Math.cos(limbSwing * speed + offset) * degree + weight) * limbSwingAmount * direction;
        applyRotation(bone, (float) Math.toDegrees(angleRadians), 0f, 0f);
    }

    public final void swing(String bone, float speed, float degree, boolean invert, float offset, float weight, float limbSwing, float limbSwingAmount) {
        final float direction = invert ? -1f : 1f;
        final float angleRadians = ((float) Math.cos(limbSwing * speed + offset) * degree + weight) * limbSwingAmount * direction;
        applyRotation(bone, 0f, (float) Math.toDegrees(angleRadians), 0f);
    }

    public final void flap(String bone, float speed, float degree, boolean invert, float offset, float weight, float limbSwing, float limbSwingAmount) {
        final float direction = invert ? -1f : 1f;
        final float angleRadians = ((float) Math.cos(limbSwing * speed + offset) * degree + weight) * limbSwingAmount * direction;
        applyRotation(bone, 0f, 0f, (float) Math.toDegrees(angleRadians));
    }

    public final void bob(String bone, float speed, float degree, boolean bounce, float limbSwing, float limbSwingAmount) {
        float bob = (float) Math.sin(limbSwing * speed) * limbSwingAmount * degree;
        bob = bounce ? -Math.abs(bob) : bob - limbSwingAmount * degree;
        applyPosition(bone, 0f, bob, 0f);
    }

    public final void chainWave(String[] bones, float speed, float degree, float rootOffset, float limbSwing, float limbSwingAmount) {
        chainRotate(bones, 0, speed, degree, rootOffset, limbSwing, limbSwingAmount);
    }

    public final void chainSwing(String[] bones, float speed, float degree, float rootOffset, float limbSwing, float limbSwingAmount) {
        chainRotate(bones, 1, speed, degree, rootOffset, limbSwing, limbSwingAmount);
    }

    public final void chainFlap(String[] bones, float speed, float degree, float rootOffset, float limbSwing, float limbSwingAmount) {
        chainRotate(bones, 2, speed, degree, rootOffset, limbSwing, limbSwingAmount);
    }

    private void chainRotate(String[] bones, int axis, float speed, float degree, float rootOffset, float limbSwing, float limbSwingAmount) {
        if (bones == null || bones.length == 0) {
            return;
        }

        final float offset = rootOffset * Mth.PI / (2f * bones.length);
        for (int i = 0; i < bones.length; i++) {
            final float angle = (float) Math.toDegrees(Math.cos(limbSwing * speed + offset * i) * degree * limbSwingAmount);
            if (axis == 0) {
                applyRotation(bones[i], angle, 0f, 0f);
            }
            else if (axis == 1) {
                applyRotation(bones[i], 0f, angle, 0f);
            }
            else {
                applyRotation(bones[i], 0f, 0f, angle);
            }

        }

    }

    public final void progressPosition(String bone, float progress, float x, float y, float z) {
        progressPosition(bone, progress, x, y, z, 1f);
    }

    public final void progressPosition(String bone, float progress, float x, float y, float z, float divisor) {
        final float clampedDivisor = Math.abs(divisor) < 1.0E-6f ? 1f : divisor;
        applyPosition(bone, x * progress / clampedDivisor, y * progress / clampedDivisor, z * progress / clampedDivisor);
    }

    public final void progressRotation(String bone, float progress, float xDegrees, float yDegrees, float zDegrees) {
        progressRotation(bone, progress, xDegrees, yDegrees, zDegrees, 1f);
    }

    public final void progressRotation(String bone, float progress, float xDegrees, float yDegrees, float zDegrees, float divisor) {
        final float safeDivisor = Math.abs(divisor) < 1.0E-6f ? 1f : divisor;
        applyRotation(bone, xDegrees * progress / safeDivisor, yDegrees * progress / safeDivisor, zDegrees * progress / safeDivisor);
    }

    public final void faceTarget(String bone, float yawDegrees, float pitchDegrees, float divisor) {
        final float safeDivisor = Math.abs(divisor) < 1.0E-6f ? 1f : divisor;
        applyRotation(bone, pitchDegrees / safeDivisor, yawDegrees / safeDivisor, 0f);
    }

    public final void faceTarget(float yawDegrees, float pitchDegrees, float divisor, String... bones) {
        if (bones == null || bones.length == 0) {
            return;
        }

        final float clampedDivisor = Math.abs(divisor) < 1.0E-6f ? 1f : divisor;
        final float distributedDivisor = clampedDivisor * bones.length;
        for (final String bone : bones) {
            faceTarget(bone, yawDegrees, pitchDegrees, distributedDivisor);
        }

    }

    @FunctionalInterface
    public interface BoneVisitor {

        void visit(String name, Matrix4f pose, Matrix3f normal);

    }

}