package dev.xylonity.knightlib.api.entity.hitbox.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import dev.xylonity.knightlib.api.animation.internal.GeoAnimationParser;
import dev.xylonity.knightlib.api.animation.internal.AnimationPose;
import dev.xylonity.knightlib.api.animation.internal.AnimationLookup;
import dev.xylonity.knightlib.api.animation.internal.KnightLibAnimationEvaluator;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.client.animation.molang.MolangContext;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxPoseProvider;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxRig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geo skeleton and hitbox handler for the pose evaluator
 */
public final class GeoBoneHitboxRig implements BoneHitboxRig {

    private static final Map<AssetKey, Definition> DEFINITIONS = new ConcurrentHashMap<>();

    private final Definition definition;
    private final Pose rest;
    private final Pose composed;

    private final KnightLibAnimationEvaluator evaluator = new KnightLibAnimationEvaluator();

    private final Matrix4f[] worldMatrices;
    private final boolean[] worldVisibility;
    private final MolangContext molang = new MolangContext();
    private boolean animatedPose;

    public GeoBoneHitboxRig(ResourceLocation geometry, @Nullable ResourceLocation animations) {
        final AssetKey key = new AssetKey(Objects.requireNonNull(geometry, "geometry"), animations);
        this.definition = DEFINITIONS.computeIfAbsent(key, GeoBoneHitboxRig::loadDefinition);
        this.rest = Pose.rest(definition.bones);
        evaluator.bindSkeleton(definition.bones.stream().map(Bone::name).toList());
        this.composed = new Pose(definition.bones.size());
        this.worldMatrices = new Matrix4f[definition.bones.size()];
        this.worldVisibility = new boolean[definition.bones.size()];
        for (int i = 0; i < worldMatrices.length; i++) {
            worldMatrices[i] = new Matrix4f();
        }

    }

    @Override
    public @Nullable Vec3 boneHalfExtents(String boneName) {
        final Integer index = definition.indices.get(boneName);
        return index == null ? null : definition.bones.get(index).halfExtents;
    }

    @Override
    public @Nullable Vec3 boneCenterOffset(String boneName) {
        final Integer index = definition.indices.get(boneName);
        return index == null ? null : definition.bones.get(index).centerOffset;
    }

    @Override
    public void updatePose(LivingEntity owner, Set<String> boneNames, float modelScale, BoneHitboxPoseProvider.BoneTransformSink transforms) {
        updatePose(owner, boneNames, modelScale, transforms, new Vec3(owner.getX(), owner.getY(), owner.getZ()), owner.yBodyRot, owner.level().getGameTime());
    }

    @Override
    public void updatePose(LivingEntity owner, Set<String> boneNames, float modelScale, BoneHitboxPoseProvider.BoneTransformSink transforms, Vec3 rootPosition, float bodyYaw, double time) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(boneNames, "boneNames");
        Objects.requireNonNull(transforms, "transforms");
        Objects.requireNonNull(rootPosition, "rootPosition");
        if (!Float.isFinite(modelScale) || modelScale <= 0f) {
            throw new IllegalArgumentException("[KnightLib] modelScale must be finite and positive");
        }

        if (owner instanceof final KnightLibAnimatable animatable) {
            animate(animatable.getAnimationHandler(), owner, time);
        }
        else {
            composed.copyFrom(rest);
            animatedPose = false;
        }

        emitWorldPose(rootPosition, bodyYaw, boneNames, modelScale, transforms);
    }

    @Override
    public boolean isAnimationWithin(String animationName, float minTick, float maxTick) {
        return animatedPose && evaluator.isAnimationWithin(animationName, minTick, maxTick);
    }

    private void animate(KnightLibAnimationHandler handler, LivingEntity owner, double now) {
        animatedPose = true;
        molang.setEntity(owner);
        molang.setNow(now);

        final AnimationPose pose = evaluator.evaluate(handler.controllers(), this::resolveAnimation, now, molang, false);

        composed.copyFrom(rest);

        for (int i = 0; i < pose.boneCount(); i++) {
            final float[] target = composed.values[i];
            target[0] -= pose.value(i, 0);
            target[1] += pose.value(i, 1);
            target[2] += pose.value(i, 2);
            target[3] -= pose.value(i, 3);
            target[4] -= pose.value(i, 4);
            target[5] += pose.value(i, 5);
            target[6] *= pose.value(i, 6);
            target[7] *= pose.value(i, 7);
            target[8] *= pose.value(i, 8);
        }

    }

    private @Nullable KnightLibAnimation resolveAnimation(@Nullable String name) {
        if (name == null) {
            return null;
        }

        return definition.animations.get(name);
    }

    private void emitWorldPose(Vec3 rootPosition, float bodyYaw, Set<String> requested, float modelScale, BoneHitboxPoseProvider.BoneTransformSink transforms) {
        final Matrix4f root = new Matrix4f()
                .translation((float) rootPosition.x, (float) rootPosition.y, (float) rootPosition.z)
                .rotateY((float) Math.toRadians(180f - bodyYaw))
                .scale(modelScale);

        for (int i = 0; i < definition.bones.size(); i++) {
            final Bone bone = definition.bones.get(i);
            final Matrix4f matrix = worldMatrices[i];
            if (bone.parentIndex < 0) {
                matrix.set(root);
            }
            else {
                matrix.set(worldMatrices[bone.parentIndex]);
            }

            final float[] pose = composed.values[i];
            matrix.translate(pose[0] / 16f, pose[1] / 16f, pose[2] / 16f);
            if (pose[3] != 0f || pose[4] != 0f || pose[5] != 0f) {
                matrix.rotateZYX((float) Math.toRadians(pose[5]), (float) Math.toRadians(pose[4]), (float) Math.toRadians(pose[3]));
            }
            if (pose[6] != 1f || pose[7] != 1f || pose[8] != 1f) {
                matrix.scale(pose[6], pose[7], pose[8]);
            }

            worldVisibility[i] = bone.visible && (bone.parentIndex < 0 || worldVisibility[bone.parentIndex]);
            if (!worldVisibility[i] || !requested.contains(bone.name)) {
                continue;
            }

            final Vector3f position = matrix.getTranslation(new Vector3f());
            final Matrix3f rotationAndScale = new Matrix3f(matrix);
            final float scaleX = columnLength(rotationAndScale, 0);
            final float scaleY = columnLength(rotationAndScale, 1);
            final float scaleZ = columnLength(rotationAndScale, 2);
            if (!finitePositive(scaleX) || !finitePositive(scaleY) || !finitePositive(scaleZ)) {
                continue;
            }

            transforms.update(bone.name, new Vec3(position.x(), position.y(), position.z()), rotationAndScale, scaleX, scaleY, scaleZ);
        }

    }

    private static boolean finitePositive(float value) {
        return Float.isFinite(value) && value > 1.0E-6f;
    }

    private static float columnLength(Matrix3f matrix, int column) {
        return switch (column) {
            case 0 -> (float) Math.sqrt(matrix.m00 * matrix.m00 + matrix.m01 * matrix.m01 + matrix.m02 * matrix.m02);
            case 1 -> (float) Math.sqrt(matrix.m10 * matrix.m10 + matrix.m11 * matrix.m11 + matrix.m12 * matrix.m12);
            case 2 -> (float) Math.sqrt(matrix.m20 * matrix.m20 + matrix.m21 * matrix.m21 + matrix.m22 * matrix.m22);
            default -> throw new IllegalArgumentException("[KnightLib] column must be 0..2");
        };

    }

    private static Definition loadDefinition(AssetKey key) {
        final JsonObject geometry = PackagedAssetReader.readJson(key.geometry);
        final Map<String, KnightLibAnimation> animations = key.animations == null ? Map.of() : AnimationLookup.withAliases(GeoAnimationParser.parse(PackagedAssetReader.readJson(key.animations)));
        return parseDefinition(geometry, animations);
    }

    private static Definition parseDefinition(JsonObject root, Map<String, KnightLibAnimation> animations) {
        if (!root.has("minecraft:geometry") || !root.get("minecraft:geometry").isJsonArray() || root.getAsJsonArray("minecraft:geometry").isEmpty()) {
            throw new IllegalArgumentException("[KnightLib] Geometry file has no minecraft:geometry entry");
        }

        final JsonObject geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        final Map<String, RawBone> raw = new LinkedHashMap<>();
        if (geometry.has("bones")) {
            for (final JsonElement element : geometry.getAsJsonArray("bones")) {
                final JsonObject object = element.getAsJsonObject();
                final String name = object.get("name").getAsString();
                final String parent = object.has("parent") ? object.get("parent").getAsString() : "";
                final Vector3f pivot = parseVector(object, "pivot").mul(-1f, 1f, 1f);
                final Vector3f rotation = parseVector(object, "rotation").mul(-1f, -1f, 1f);
                final boolean visible = !(object.has("neverRender") && object.get("neverRender").getAsBoolean());
                final float defaultInflate = object.has("inflate") ? object.get("inflate").getAsFloat() : 0f;
                if (name.isBlank() || name.equals("__root") || raw.containsKey(name)) {
                    throw new IllegalArgumentException("[KnightLib] Duplicate, reserved or empty bone name '" + name + "'");
                }
                if (!Float.isFinite(defaultInflate)) {
                    throw new IllegalArgumentException("[KnightLib] Bone inflate must be finite");
                }

                raw.put(name, new RawBone(name, parent, pivot, rotation, visible, parseBounds(object, pivot, defaultInflate)));
            }

        }

        for (final RawBone bone : raw.values()) {
            if (!bone.parent.isEmpty() && !raw.containsKey(bone.parent)) {
                throw new IllegalArgumentException("[KnightLib] Bone '" + bone.name + "' references unknown parent '" + bone.parent + "'");
            }

        }

        final List<RawBone> ordered = new ArrayList<>(raw.size());
        final Set<String> visiting = new HashSet<>();
        final Set<String> visited = new HashSet<>();
        for (final RawBone bone : raw.values()) {
            visit(bone, raw, visiting, visited, ordered);
        }

        final Map<String, Integer> indices = new LinkedHashMap<>();
        final List<Bone> bones = new ArrayList<>(ordered.size());
        for (final RawBone rawBone : ordered) {
            final int parentIndex = rawBone.parent.isEmpty() ? -1 : indices.get(rawBone.parent);
            final Vector3f parentPivot = parentIndex < 0 ? new Vector3f() : raw.get(rawBone.parent).pivot;
            indices.put(rawBone.name, bones.size());
            bones.add(new Bone(rawBone.name, parentIndex,
                    rawBone.pivot.x() - parentPivot.x(), rawBone.pivot.y() - parentPivot.y(),
                    rawBone.pivot.z() - parentPivot.z(), rawBone.rotation.x(), rawBone.rotation.y(),
                    rawBone.rotation.z(), rawBone.visible, rawBone.bounds.halfExtents(),
                    rawBone.bounds.centerOffset())
            );

        }

        return new Definition(List.copyOf(bones), Map.copyOf(indices), Map.copyOf(animations));
    }

    private static void visit(RawBone bone, Map<String, RawBone> all, Set<String> visiting, Set<String> visited, List<RawBone> ordered) {
        if (visited.contains(bone.name)) {
            return;
        }
        if (!visiting.add(bone.name)) {
            throw new IllegalArgumentException("[KnightLib] Bone tree cycle contains '" + bone.name + "'");
        }
        if (!bone.parent.isEmpty()) {
            visit(all.get(bone.parent), all, visiting, visited, ordered);
        }

        visiting.remove(bone.name);
        visited.add(bone.name);
        ordered.add(bone);
    }

    private static BoneBounds parseBounds(JsonObject bone, Vector3f bonePivot, float defaultInflate) {
        if (!bone.has("cubes") || bone.getAsJsonArray("cubes").isEmpty()) {
            return new BoneBounds(new Vec3(0.1, 0.1, 0.1), Vec3.ZERO);
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (final JsonElement element : bone.getAsJsonArray("cubes")) {
            final JsonObject cube = element.getAsJsonObject();
            final Vector3f origin = parseVector(cube, "origin");
            final Vector3f size = parseVector(cube, "size");

            final float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : defaultInflate;
            if (!Float.isFinite(inflate)) {
                throw new IllegalArgumentException("[KnightLib] Cube inflate must be finite");
            }

            final float x0 = -(origin.x() + size.x()) - inflate;
            final float y0 = origin.y() - inflate;
            final float z0 = origin.z() - inflate;
            final float sx = size.x() + inflate * 2f;
            final float sy = size.y() + inflate * 2f;
            final float sz = size.z() + inflate * 2f;

            final Matrix4f cubeTransform = new Matrix4f();
            if (cube.has("rotation")) {
                final Vector3f pivot = parseVector(cube, "pivot").mul(-1f, 1f, 1f);
                final Vector3f rotation = parseVector(cube, "rotation").mul(-1f, -1f, 1f);
                cubeTransform.translate(pivot).rotateZYX((float) Math.toRadians(rotation.z()), (float) Math.toRadians(rotation.y()), (float) Math.toRadians(rotation.x()))
                        .translate(-pivot.x(), -pivot.y(), -pivot.z());
            }

            for (int mask = 0; mask < 8; mask++) {
                final Vector3f point = cubeTransform.transformPosition(
                        x0 + ((mask & 1) == 0 ? 0f : sx),
                        y0 + ((mask & 2) == 0 ? 0f : sy),
                        z0 + ((mask & 4) == 0 ? 0f : sz), new Vector3f()
                );
                point.sub(bonePivot).mul(1f / 16f);

                minX = Math.min(minX, point.x());
                minY = Math.min(minY, point.y());
                minZ = Math.min(minZ, point.z());
                maxX = Math.max(maxX, point.x());
                maxY = Math.max(maxY, point.y());
                maxZ = Math.max(maxZ, point.z());
            }

        }

        return new BoneBounds(
                new Vec3((maxX - minX) * 0.5d, (maxY - minY) * 0.5d, (maxZ - minZ) * 0.5d),
                new Vec3((maxX + minX) * 0.5d, (maxY + minY) * 0.5d, (maxZ + minZ) * 0.5d)
        );

    }

    private record BoneBounds(
            Vec3 halfExtents,
            Vec3 centerOffset
    ) {
        ;;
    }

    private static Vector3f parseVector(JsonObject object, String key) {
        if (!object.has(key)) {
            return new Vector3f();
        }

        final JsonArray array = object.getAsJsonArray(key);
        if (array.size() < 3) {
            throw new IllegalArgumentException("[KnightLib] Vector '" + key + "' needs three components");
        }

        final Vector3f value = new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
        if (!Float.isFinite(value.x()) || !Float.isFinite(value.y()) || !Float.isFinite(value.z())) {
            throw new IllegalArgumentException("[KnightLib] Geometry vector '" + key + "' must be finite");
        }

        return value;
    }

    private record AssetKey(
            ResourceLocation geometry,
            @Nullable ResourceLocation animations
    ) {
        ;;
    }

    private record Definition(
            List<Bone> bones,
            Map<String, Integer> indices,
            Map<String, KnightLibAnimation> animations
    ) {
        ;;
    }

    private record Bone(
            String name,
            int parentIndex,
            float x,
            float y,
            float z,
            float rotX,
            float rotY,
            float rotZ,
            boolean visible,
            Vec3 halfExtents,
            Vec3 centerOffset
    ) {
        ;;
    }

    private record RawBone(
            String name,
            String parent,
            Vector3f pivot,
            Vector3f rotation,
            boolean visible,
            BoneBounds bounds
    ) {
        ;;
    }

    private static final class Pose {

        final float[][] values;

        Pose(int count) {
            this.values = new float[count][9];
        }

        static Pose rest(List<Bone> bones) {
            final Pose pose = new Pose(bones.size());
            for (int i = 0; i < bones.size(); i++) {
                final Bone bone = bones.get(i);
                final float[] value = pose.values[i];
                value[0] = bone.x;
                value[1] = bone.y;
                value[2] = bone.z;
                value[3] = bone.rotX;
                value[4] = bone.rotY;
                value[5] = bone.rotZ;
                value[6] = 1f;
                value[7] = 1f;
                value[8] = 1f;
            }

            return pose;
        }

        void copyFrom(Pose source) {
            for (int i = 0; i < values.length; i++) {
                System.arraycopy(source.values[i], 0, values[i], 0, 9);
            }

        }


    }

}