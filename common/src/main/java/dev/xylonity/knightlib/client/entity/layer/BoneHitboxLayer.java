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
        if (hitbox != null && hitbox.isAutoSize() && hitbox.getHalfExtents() == null) {
            computeAutoSize(hitbox, bone);
        }

        // World-space position of the bone
        final Vector3d worldPosition = bone.getWorldPosition();

        // Exacts rotation (3x3) from the model matrix
        final Matrix3f boneRotation = new Matrix3f();
        bone.getModelSpaceMatrix().get3x3(boneRotation);

        // Removes scaling from the rotation matrix
        normalizeColumns(boneRotation);

        // Applies entity body yaw to align with world orientation
        final float bodyYaw = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
        final Matrix3f rotation = new Matrix3f().rotateY((float) Math.toRadians(180.0 - bodyYaw));
        rotation.mul(boneRotation);

        final Vec3 position = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        // Updates the manager with the computed data
        manager.updateBoneTransform(bone.getName(), position, rotation);

        // Includes half-extents if it's autosized (the server cannot compute it)
        final Vec3 halfExtents = (hitbox != null && hitbox.isAutoSize()) ? hitbox.getHalfExtents() : null;
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
     * Computes the half-extents for an auto-sized hitbox by enclosing all cubes of the bone
     */
    private static void computeAutoSize(BoneHitbox hitbox, GeoBone bone) {
        final List<GeoCube> cubes = bone.getCubes();
        if (cubes.isEmpty()) {
            hitbox.setHalfExtents(new Vec3(0.1, 0.1, 0.1));
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

        hitbox.setHalfExtents(new Vec3(maxX, maxY, maxZ));
    }

    /**
     * Normalizes the columns of a 3x3 rotation matrix to remove any scale factor
     */
    private static void normalizeColumns(Matrix3f m) {
        float len0 = (float) Math.sqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02);
        float len1 = (float) Math.sqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12);
        float len2 = (float) Math.sqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22);

        if (len0 > 1e-6f) {
            m.m00 /= len0;
            m.m01 /= len0;
            m.m02 /= len0;
        }
        if (len1 > 1e-6f) {
            m.m10 /= len1;
            m.m11 /= len1;
            m.m12 /= len1;
        }
        if (len2 > 1e-6f) {
            m.m20 /= len2;
            m.m21 /= len2;
            m.m22 /= len2;
        }
    }

}