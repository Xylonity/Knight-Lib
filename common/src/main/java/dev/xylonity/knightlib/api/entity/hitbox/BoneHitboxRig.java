package dev.xylonity.knightlib.api.entity.hitbox;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Server bone hierarchy used to drive collision hitboxes without accepting render data from a client.
 * A rig instance may keep animation clocks and must therefore belong to one manager.
 */
public interface BoneHitboxRig {

    /// Base half-extents enclosing this bone's geometry, before animated or renderer scale
    @Nullable Vec3 boneHalfExtents(String boneName);

    /// Local offset (in blocks) from the bone pivot to the center of its geometry
    default @Nullable Vec3 boneCenterOffset(String boneName) {
        return Vec3.ZERO;
    }

    /// Evaluates the requested bones at the current server tick and emits world transforms
    void updatePose(LivingEntity owner, Set<String> boneNames, float modelScale, BoneHitboxPoseProvider.BoneTransformSink transforms);

    /// Evaluation at an explicit timeline instant and root transform (the client uses this too so the transforms remain parallel)
    void updatePose(LivingEntity owner, Set<String> boneNames, float modelScale, BoneHitboxPoseProvider.BoneTransformSink transforms, Vec3 rootPosition, float bodyYaw, double time);

    /// Whether an animation is basically playing
    default boolean isAnimationWithin(String animationName, float minTick, float maxTick) {
        return false;
    }

}
