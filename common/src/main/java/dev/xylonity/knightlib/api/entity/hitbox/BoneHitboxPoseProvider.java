package dev.xylonity.knightlib.api.entity.hitbox;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;

/**
 * Supplies authoritative world-space bone transforms on the server
 */
@FunctionalInterface
public interface BoneHitboxPoseProvider {

    void updatePose(LivingEntity owner, BoneTransformSink transforms);

    @FunctionalInterface
    interface BoneTransformSink {

        /**
         * Updates a bone pivot, world rotation and model scale for the current server tick
         */
        void update(String boneName, Vec3 worldPosition, Matrix3f worldRotation, float scaleX, float scaleY, float scaleZ);

        default void update(String boneName, Vec3 worldPosition, Matrix3f worldRotation) {
            update(boneName, worldPosition, worldRotation, 1f, 1f, 1f);
        }

    }

}
