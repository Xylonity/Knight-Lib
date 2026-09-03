package dev.xylonity.knightlib.api.entity.hitbox.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.animation.KnightLibAnim.Step;
import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import dev.xylonity.knightlib.api.animation.internal.GeoAnimationParser;
import dev.xylonity.knightlib.api.animation.internal.KnightLibAnimationSequence;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.client.animation.molang.MolangContext;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxPoseProvider;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxRig;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server only copy of the geo bone tree and animation evaluator. This basically mirrors {@code KnightLibAnimator}.
 *
 * TODO: refactor common logic
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/loading/object/BakedModelFactory.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/loading/json/typeadapter/BakedAnimationsAdapter.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/animation/AnimationProcessor.java
 */
public final class GeoBoneHitboxRig implements BoneHitboxRig {

    private static final Map<AssetKey, Definition> DEFINITIONS = new ConcurrentHashMap<>();
    private static final Set<String> WARNED_ANIMATIONS = ConcurrentHashMap.newKeySet();

    private final Definition definition;
    private final Pose rest;
    private final Pose working;
    private final Pose composed;
    private final Map<String, ServerClock> clocks = new HashMap<>();
    private final Matrix4f[] worldMatrices;
    private final boolean[] worldVisibility;
    private final MolangContext molang = new MolangContext();
    private final Map<KnightLibAnimation, String> animationNames = new IdentityHashMap<>();
    private final List<ActivePlayback> activePlaybacks = new ArrayList<>();

    public GeoBoneHitboxRig(ResourceLocation geometry, @Nullable ResourceLocation animations) {
        final AssetKey key = new AssetKey(Objects.requireNonNull(geometry, "geometry"), animations);
        this.definition = DEFINITIONS.computeIfAbsent(key, GeoBoneHitboxRig::loadDefinition);
        this.rest = Pose.rest(definition.bones);
        this.working = new Pose(definition.bones.size());
        this.composed = new Pose(definition.bones.size());
        this.worldMatrices = new Matrix4f[definition.bones.size()];
        this.worldVisibility = new boolean[definition.bones.size()];
        for (int i = 0; i < worldMatrices.length; i++) {
            worldMatrices[i] = new Matrix4f();
        }
        for (final Map.Entry<String, KnightLibAnimation> entry : definition.animations.entrySet()) {
            animationNames.put(entry.getValue(), entry.getKey());
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

        activePlaybacks.clear();
        if (owner instanceof final KnightLibAnimatable animatable) {
            animate(animatable.getAnimationHandler(), owner, time);
        }
        else {
            composed.copyFrom(rest);
        }

        emitWorldPose(rootPosition, bodyYaw, boneNames, modelScale, transforms);
    }

    @Override
    public boolean isAnimationWithin(String animationName, float minTick, float maxTick) {
        for (final ActivePlayback playback : activePlaybacks) {
            final String fullName = animationNames.get(playback.animation());
            if (fullName == null || !nameMatches(fullName, animationName)) {
                continue;
            }
            if (playback.tick() >= minTick && playback.tick() < maxTick) {
                return true;
            }

        }

        return false;
    }

    private static boolean nameMatches(String fullName, String requested) {
        if (fullName.equals(requested)) {
            return true;
        }

        final int last = fullName.lastIndexOf('.');
        return last >= 0 && fullName.substring(last + 1).equals(requested);
    }

    private void animate(KnightLibAnimationHandler handler, LivingEntity owner, double now) {
        molang.setEntity(owner);
        molang.setNow(now);

        for (final KnightLibAnimationHandler.Controller controller : handler.controllers()) {
            final ServerClock clock = clocks.computeIfAbsent(controller.name(), ignored -> new ServerClock());
            if (controller.sequence() != clock.sequence) {
                final double commandTime = controller.commandGameTime();
                clock.blendFrom = captureAt(clock, commandTime, controller.steps());
                clock.blendStart = commandTime;
                clock.blendTicks = controller.transitionTicks();
                clock.blendEasing = controller.easing();
                clock.steps = controller.steps();
                clock.start = commandTime;
                clock.speed = controller.speed();
                clock.finished = false;
                clock.sequence = controller.sequence();
            }

            if (!clock.steps.isEmpty() && !clock.finished) {
                final double elapsed = Math.max(0.0, (now - clock.start) * clock.speed);
                final KnightLibAnimationSequence.Playback playback = KnightLibAnimationSequence.sample(clock.steps, this::resolveAnimation, elapsed);
                if (playback.finished()) {
                    final double end = clock.start + playback.endTick() / clock.speed;
                    clock.blendFrom = captureAt(clock, end, List.of());
                    clock.blendStart = end;
                    if (clock.blendEasing == null) {
                        clock.blendEasing = KnightLibEasings.EASE_IN_OUT_QUAD;
                    }

                    clock.steps = List.of();
                    clock.finished = true;
                }
                else if (playback.animation() != null) {
                    activePlaybacks.add(new ActivePlayback(playback.animation(), playback.sampleTick()));
                }

            }

        }

        composed.copyFrom(rest);
        for (final KnightLibAnimationHandler.Controller controller : handler.controllers()) {
            final ServerClock clock = clocks.get(controller.name());
            working.copyFrom(rest);

            evaluate(clock, working, now);

            final BitSet touched = touchedBones(clock);
            composeDelta(composed, working, rest, touched);
        }

    }

    private Snapshot captureAt(ServerClock clock, double time, List<Step> incomingSteps) {
        final BitSet touched = touchedBones(clock);
        for (final Step step : incomingSteps) {
            addAnimationBones(touched, step.animation());
        }

        working.copyFrom(rest);

        evaluate(clock, working, time);

        return new Snapshot(working.copyValues(), touched);
    }

    private void evaluate(@Nullable GeoBoneHitboxRig.ServerClock clock, Pose target, double now) {
        if (clock == null) {
            return;
        }

        molang.setNow(now);
        molang.setControllerSpeed(clock.speed);

        if (!clock.steps.isEmpty()) {
            final double elapsed = Math.max(0.0, (now - clock.start) * clock.speed);
            final KnightLibAnimationSequence.Playback playback = KnightLibAnimationSequence.sample(clock.steps, this::resolveAnimation, elapsed);
            final KnightLibAnimation animation = playback.animation();
            if (animation != null) {
                final float tick = playback.sampleTick();
                molang.setAnimTime(Math.max(tick, 0f) / 20f);
                molang.setTotalAnimTime((float) Math.max(playback.rawTick(), 0.0) / 20f);
                applyChannels(animation, tick, target);
            }

        }

        if (clock.blendFrom != null) {
            final double elapsed = now - clock.blendStart;
            if (clock.blendTicks <= 0 || elapsed >= clock.blendTicks) {
                clock.blendFrom = null;
            }
            else {
                final float alpha = (float) (elapsed / clock.blendTicks);
                final float eased = clock.blendEasing == null ? alpha : clock.blendEasing.apply(alpha);
                blendFrom(target, clock.blendFrom, Mth.clamp(eased, 0f, 1f));
            }

        }

    }

    private void applyChannels(KnightLibAnimation animation, float tick, Pose target) {
        final Vector3f sampled = new Vector3f();
        final Vector3f contribution = new Vector3f();
        for (final Map.Entry<String, KnightLibAnimation.Channels> entry : animation.allChannels().entrySet()) {
            final Integer index = definition.indices.get(entry.getKey());
            if (index == null) {
                continue;
            }

            final float[] bone = target.values[index];
            final KnightLibAnimation.Channels channels = entry.getValue();
            if (sampleAdditive(channels.position(), channels.additionalPositions(), tick, sampled, contribution, false)) {
                bone[0] += -sampled.x();
                bone[1] += sampled.y();
                bone[2] += sampled.z();
            }
            if (sampleAdditive(channels.rotation(), channels.additionalRotations(), tick, sampled, contribution, false)) {
                bone[3] += -sampled.x();
                bone[4] += -sampled.y();
                bone[5] += sampled.z();
            }
            if (sampleAdditive(channels.scale(), channels.additionalScales(), tick, sampled, contribution, true)) {
                bone[6] *= sampled.x();
                bone[7] *= sampled.y();
                bone[8] *= sampled.z();
            }

        }

    }

    private boolean sampleAdditive(List<KnightLibAnimation.Keyframe> primary, List<List<KnightLibAnimation.Keyframe>> additional, float tick, Vector3f result, Vector3f contribution, boolean scale) {
        boolean sampled = KnightLibAnimation.sample(primary, tick, result, molang);
        if (!sampled) {
            result.set(scale ? 1f : 0f);
        }
        for (final List<KnightLibAnimation.Keyframe> track : additional) {
            if (!KnightLibAnimation.sample(track, tick, contribution, molang)) {
                continue;
            }

            sampled = true;
            if (scale) {
                result.add(contribution.x() - 1f, contribution.y() - 1f, contribution.z() - 1f);
            }
            else {
                result.add(contribution);
            }

        }

        return sampled;
    }

    private BitSet touchedBones(@Nullable GeoBoneHitboxRig.ServerClock clock) {
        final BitSet touched = new BitSet(definition.bones.size());
        if (clock == null) {
            return touched;
        }
        if (clock.blendFrom != null) {
            touched.or(clock.blendFrom.touched);
        }

        for (final Step step : clock.steps) {
            addAnimationBones(touched, step.animation());
        }

        return touched;
    }

    private void addAnimationBones(BitSet output, @Nullable String animationName) {
        final KnightLibAnimation animation = resolveAnimation(animationName);
        if (animation == null) {
            return;
        }

        for (final String name : animation.boneNames()) {
            final Integer index = definition.indices.get(name);
            if (index != null) {
                output.set(index);
            }

        }

    }

    private @Nullable KnightLibAnimation resolveAnimation(@Nullable String name) {
        if (name == null) {
            return null;
        }

        final KnightLibAnimation animation = definition.animations.get(name);
        if (animation != null) {
            return animation;
        }

        KnightLibAnimation match = null;
        for (final Map.Entry<String, KnightLibAnimation> entry : definition.animations.entrySet()) {
            final String key = entry.getKey();
            final int last = key.lastIndexOf('.');
            if (last >= 0 && key.substring(last + 1).equals(name)) {
                if (match != null) {
                    return null;
                }

                match = entry.getValue();
            }

        }

        if (match == null && WARNED_ANIMATIONS.add(name)) {
            KnightLib.LOGGER.warn("[KnightLib] Unknown hitbox animation '{}'", name);
        }

        return match;
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

    private static void composeDelta(Pose destination, Pose layer, Pose rest, BitSet touched) {
        for (int i = touched.nextSetBit(0); i >= 0; i = touched.nextSetBit(i + 1)) {
            final float[] result = destination.values[i];
            final float[] value = layer.values[i];
            final float[] base = rest.values[i];
            result[0] += value[0] - base[0];
            result[1] += value[1] - base[1];
            result[2] += value[2] - base[2];
            result[3] += value[3] - base[3];
            result[4] += value[4] - base[4];
            result[5] += value[5] - base[5];
            result[6] *= scaleRatio(value[6], base[6]);
            result[7] *= scaleRatio(value[7], base[7]);
            result[8] *= scaleRatio(value[8], base[8]);
        }

    }

    private static void blendFrom(Pose target, Snapshot snapshot, float currentWeight) {
        for (int i = snapshot.touched.nextSetBit(0); i >= 0; i = snapshot.touched.nextSetBit(i + 1)) {
            final float[] value = target.values[i];
            final float[] from = snapshot.values[i];
            value[0] = Mth.lerp(currentWeight, from[0], value[0]);
            value[1] = Mth.lerp(currentWeight, from[1], value[1]);
            value[2] = Mth.lerp(currentWeight, from[2], value[2]);
            value[3] = Mth.rotLerp(currentWeight, from[3], value[3]);
            value[4] = Mth.rotLerp(currentWeight, from[4], value[4]);
            value[5] = Mth.rotLerp(currentWeight, from[5], value[5]);
            value[6] = Mth.lerp(currentWeight, from[6], value[6]);
            value[7] = Mth.lerp(currentWeight, from[7], value[7]);
            value[8] = Mth.lerp(currentWeight, from[8], value[8]);
        }

    }

    private static float scaleRatio(float value, float rest) {
        return Math.abs(rest) < 1.0E-6f ? 1f : value / rest;
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
        final Map<String, KnightLibAnimation> animations = key.animations == null ? Map.of() : GeoAnimationParser.parse(PackagedAssetReader.readJson(key.animations));
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
                if (name.isBlank() || raw.containsKey(name)) {
                    throw new IllegalArgumentException("[KnightLib] Duplicate or empty bone name '" + name + "'");
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

    private record ActivePlayback(
            KnightLibAnimation animation,
            float tick
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

        float[][] copyValues() {
            final float[][] copy = new float[values.length][9];
            for (int i = 0; i < values.length; i++) {
                System.arraycopy(values[i], 0, copy[i], 0, 9);
            }

            return copy;
        }

    }

    private record Snapshot(
            float[][] values,
            BitSet touched
    ) {

        Snapshot {
            touched = (BitSet) touched.clone();
        }

    }

    private static final class ServerClock {
        long sequence = -1L;
        List<Step> steps = List.of();
        double start;
        float speed = 1f;
        boolean finished;
        Snapshot blendFrom;
        double blendStart;
        int blendTicks;
        KnightLibEasings blendEasing;
    }

}