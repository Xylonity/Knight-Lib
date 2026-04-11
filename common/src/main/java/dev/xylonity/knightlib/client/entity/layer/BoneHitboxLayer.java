package dev.xylonity.knightlib.client.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitbox;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxHolder;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxManager;
import dev.xylonity.knightlib.network.packets.BoneHitboxSyncC2S;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.*;
import java.util.function.Function;

/**
 * A GeoRenderLayer that extracts bone world-space transforms during rendering and syncs them to the server for OBB collision detection
 * <p>
 * <pre>{@code
 * public class TestEntityRenderer extends GeoEntityRenderer<TestEntity> {
 *     public TestEntityRenderer(EntityRendererProvider.Context context) {
 *         super(context, new TestEntityModel());
 *         addRenderLayer(new BoneHitboxLayer<>(this, TestEntity::getBoneHitboxManager));
 *     }
 *
 * }</pre>
 */
public class BoneHitboxLayer<T extends LivingEntity & GeoAnimatable & BoneHitboxHolder> extends GeoRenderLayer<T> {

    private final Function<T, BoneHitboxManager> managerGetter;
    private final Map<String, BoneHitboxSyncC2S.BoneTransform> pendingTransforms = new HashMap<>();
    private int lastSyncTick = -1;

    public BoneHitboxLayer(GeoRenderer<T> renderer, Function<T, BoneHitboxManager> managerGetter) {
        super(renderer);
        this.managerGetter = managerGetter;
    }

    /**
     * Enables matrix tracking on all hitbox bones before GeckoLib processes them
     */
    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        final BoneHitboxManager manager = managerGetter.apply(animatable);
        if (manager == null || !manager.isActive()) {
            return;
        }

        final Set<String> tracked = manager.getTrackedBoneNames();
        if (tracked.isEmpty()) {
            return;
        }

        for (final GeoBone topBone : bakedModel.topLevelBones()) {
            enableTrackingRecursive(topBone, tracked);
        }
    }

    private static void enableTrackingRecursive(GeoBone bone, Set<String> trackedNames) {
        if (trackedNames.contains(bone.getName())) {
            bone.setTrackingMatrices(true);
        }
        for (final GeoBone childBone : bone.getChildBones()) {
            enableTrackingRecursive(childBone, trackedNames);
        }

    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        final BoneHitboxManager manager = managerGetter.apply(animatable);
        if (manager == null || !manager.isActive()) {
            return;
        }

        final Set<String> tracked = manager.getTrackedBoneNames();
        if (!tracked.contains(bone.getName())) {
            return;
        }

        final BoneHitbox hitbox = manager.get(bone.getName());
        if (hitbox != null && hitbox.isAutoSize() && hitbox.getBaseHalfExtents() == null) {
            computeAutoSize(hitbox, bone);
        }

        // World-space position of the bone
        final Vector3d worldPosition = bone.getWorldPosition();

        // Extracts rotation (3x3) from the model matrix
        final Matrix3f boneRotation = new Matrix3f();
        bone.getModelSpaceMatrix().get3x3(boneRotation);

        // Extracts the scale (column lengths) before normalizing
        final float scaleX = columnLength(boneRotation, 0);
        final float scaleY = columnLength(boneRotation, 1);
        final float scaleZ = columnLength(boneRotation, 2);

        // Applies bone scale to the base half-extents
        if (hitbox != null) {
            hitbox.applyScale(scaleX, scaleY, scaleZ);
        }

        // Removes scaling from the rotation matrix
        normalizeColumns(boneRotation);

        // Applies entity body yaw to align with world orientation
        final float bodyYaw = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
        final Matrix3f rotation = new Matrix3f().rotateY((float) Math.toRadians(180.0 - bodyYaw));
        rotation.mul(boneRotation);

        final Vec3 position = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        // Updates the manager with the computed data
        manager.updateBoneTransform(bone.getName(), position, rotation);

        // Includes the current half-extents in sync packet (the server needs them for autosized
        // hitboxes, and they change each frame when bone scale is animated)
        final Vec3 halfExtents = (hitbox != null) ? hitbox.getHalfExtents() : null;
        pendingTransforms.put(bone.getName(), new BoneHitboxSyncC2S.BoneTransform(position, rotation, halfExtents));
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (pendingTransforms.isEmpty()) {
            return;
        }

        // Sends a sync packet to the server with the bb info of the hitbox
        final int currentTick = animatable.tickCount;
        if (currentTick != lastSyncTick) {
            lastSyncTick = currentTick;
            KnightLib.NET.sendToServer(new BoneHitboxSyncC2S(animatable.getId(), new HashMap<>(pendingTransforms)));
        }

        pendingTransforms.clear();
    }

    /**
     * Computes the base half-extents for an auto-sized hitbox by enclosing all cubes of the bone
     */
    private static void computeAutoSize(BoneHitbox hitbox, GeoBone bone) {
        final List<GeoCube> cubes = bone.getCubes();
        if (cubes.isEmpty()) {
            hitbox.setBaseHalfExtents(new Vec3(0.1, 0.1, 0.1));
            return;
        }

        double maxX = 0;
        double maxY = 0;
        double maxZ = 0;
        for (final GeoCube cube : cubes) {
            final Vec3 size = cube.size();
            final double hx = (size.x / 2.0) / 16.0;
            final double hy = (size.y / 2.0) / 16.0;
            final double hz = (size.z / 2.0) / 16.0;
            maxX = Math.max(maxX, hx);
            maxY = Math.max(maxY, hy);
            maxZ = Math.max(maxZ, hz);
        }

        hitbox.setBaseHalfExtents(new Vec3(maxX, maxY, maxZ));
    }

    /**
     * Returns the length of the specified column of a 3x3 matrix
     */
    private static float columnLength(Matrix3f matrix, int column) {
        return switch (column) {
            case 0 -> (float) Math.sqrt(matrix.m00 * matrix.m00 + matrix.m01 * matrix.m01 + matrix.m02 * matrix.m02);
            case 1 -> (float) Math.sqrt(matrix.m10 * matrix.m10 + matrix.m11 * matrix.m11 + matrix.m12 * matrix.m12);
            case 2 -> (float) Math.sqrt(matrix.m20 * matrix.m20 + matrix.m21 * matrix.m21 + matrix.m22 * matrix.m22);
            default -> 1f;
        };

    }

    /**
     * Normalizes the columns of a 3x3 rotation matrix to remove any scale factor
     */
    private static void normalizeColumns(Matrix3f matrix) {
        float len0 = columnLength(matrix, 0);
        float len1 = columnLength(matrix, 1);
        float len2 = columnLength(matrix, 2);

        if (len0 > 1e-6f) {
            matrix.m00 /= len0;
            matrix.m01 /= len0;
            matrix.m02 /= len0;
        }
        if (len1 > 1e-6f) {
            matrix.m10 /= len1;
            matrix.m11 /= len1;
            matrix.m12 /= len1;
        }
        if (len2 > 1e-6f) {
            matrix.m20 /= len2;
            matrix.m21 /= len2;
            matrix.m22 /= len2;
        }
    }

}