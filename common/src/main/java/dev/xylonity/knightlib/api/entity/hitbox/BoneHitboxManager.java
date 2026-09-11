package dev.xylonity.knightlib.api.entity.hitbox;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

/**
 * Manages all bone-attached hitboxes for a single entity.
 * {@link BoneHitboxHolder}
 */
public class BoneHitboxManager {

    /// Server ticks of bone pose history
    private static final int REWIND_TICKS = 5;

    private static final double ATTACK_TOLERANCE = 0.25d;
    private static final int MAX_SWEEP_SAMPLES = 4;
    private static final double MAX_SWEEP_DISTANCE = 10d;

    private final LivingEntity owner;
    private final Map<String, BoneHitbox> hitboxes = new LinkedHashMap<>();
    private final Map<String, Long> lastTransformTicks = new HashMap<>();
    private final Map<String, Integer> timedDisables = new HashMap<>();
    private final ArrayDeque<PoseSnapshot> history = new ArrayDeque<>();
    private boolean active = true;
    private int transformTimeoutTicks;

    private @Nullable BiConsumer<BoneHitbox, Entity> hitCallback;
    private @Nullable BoneHitboxPoseProvider poseProvider;
    private @Nullable BoneHitboxRig rig;
    private ToDoubleFunction<LivingEntity> modelScale = ignored -> 1d;

    public BoneHitboxManager(LivingEntity owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    /**
     * Registers a bone hitbox (the bone name must be unique within this manager)
     */
    public BoneHitboxManager add(BoneHitbox hitbox) {
        Objects.requireNonNull(hitbox, "hitbox");
        if (hitboxes.putIfAbsent(hitbox.getBoneName(), hitbox) != null) {
            throw new IllegalArgumentException("[KnightLib] Duplicate bone hitbox '" + hitbox.getBoneName() + "'");
        }

        resolveAutoSize(hitbox);

        return this;
    }

    /**
     * Retrieves a hitbox by bone name
     */
    public @Nullable BoneHitbox get(String boneName) {
        return hitboxes.get(boneName);
    }

    /**
     * Returns all registered hitboxes
     */
    public Collection<BoneHitbox> getAll() {
        return Collections.unmodifiableCollection(hitboxes.values());
    }

    /**
     * Sets whether the entire hitbox system is active. When inactive, no collision checks run.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Sets the manager fallback that fires when a hitbox without its own contact behavior collides with an entity.
     *
     * <pre>{@code
     * manager.onHit((hitbox, target) -> {
     *     if (target instanceof LivingEntity living) {
     *         final float damage = hitbox.getBoneName().equals("axe_tip") ? 15 : 8;
     *         living.hurt(damageSources().mobAttack(this), damage);
     *     }
     *
     * });
     * }</pre>
     */
    public BoneHitboxManager onHit(BiConsumer<BoneHitbox, Entity> callback) {
        this.hitCallback = callback;
        return this;
    }

    /**
     * Updates a world-space bone transform (call this through server tick or use a custom provider)
     */
    public void updateBoneTransform(String boneName, Vec3 worldPos, Matrix3f worldRotation) {
        updateBoneTransform(boneName, worldPos, worldRotation, 1f, 1f, 1f);
    }

    /**
     * Transform update including animated bone scale
     */
    public void updateBoneTransform(String boneName, Vec3 worldPos, Matrix3f worldRotation, float scaleX, float scaleY, float scaleZ) {
        final BoneHitbox hitbox = hitboxes.get(boneName);
        if (hitbox != null) {
            validateTransform(worldPos, worldRotation, scaleX, scaleY, scaleZ);
            hitbox.applyScale(scaleX, scaleY, scaleZ);
            if (hitbox.updateFromBoneTransform(worldPos, normalizedRotation(worldRotation), scaleX, scaleY, scaleZ)) {
                lastTransformTicks.put(boneName, owner.level().getGameTime());
            }

        }

    }

    /**
     * Client mirror of the server pose
     */
    public void updateClientPose(float partialTick) {
        if (!owner.level().isClientSide || rig == null || !active) {
            return;
        }

        for (final BoneHitbox hitbox : hitboxes.values()) {
            resolveAutoSize(hitbox);
        }

        final double scale = modelScale.applyAsDouble(owner);
        if (!Double.isFinite(scale) || scale <= 0d || scale > Float.MAX_VALUE) {
            return;
        }

        final float clampedPartial = Math.max(0f, Math.min(1f, partialTick));
        final Vec3 position = new Vec3(
                Mth.lerp(clampedPartial, owner.xo, owner.getX()),
                Mth.lerp(clampedPartial, owner.yo, owner.getY()),
                Mth.lerp(clampedPartial, owner.zo, owner.getZ())
        );
        final float bodyYaw = Mth.rotLerp(clampedPartial, owner.yBodyRotO, owner.yBodyRot);

        rig.updatePose(owner, getTrackedBoneNames(), (float) scale, this::updateBoneTransform, position, bodyYaw, owner.level().getGameTime() + clampedPartial);
    }

    /**
     * Installs a custom pose provider
     */
    public BoneHitboxManager poseProvider(BoneHitboxPoseProvider provider) {
        this.poseProvider = Objects.requireNonNull(provider, "provider");
        return this;
    }

    public void clearPoseProvider() {
        this.poseProvider = null;
    }

    public BoneHitboxManager rig(BoneHitboxRig rig) {
        this.rig = Objects.requireNonNull(rig, "rig");
        for (final BoneHitbox hitbox : hitboxes.values()) {
            hitbox.clearAutoSize();
            resolveAutoSize(hitbox);
        }

        return this;
    }

    public BoneHitboxManager geo(ResourceLocation geometry, ResourceLocation animations) {
        return rig(BoneHitboxRigs.geo(geometry, animations));
    }

    public BoneHitboxManager geo(ResourceLocation geometry) {
        return rig(BoneHitboxRigs.geo(geometry));
    }

    public void clearRig() {
        this.rig = null;
        hitboxes.values().forEach(BoneHitbox::clearAutoSize);
    }

    public BoneHitboxManager modelScale(double scale) {
        if (!Double.isFinite(scale) || scale <= 0d) {
            throw new IllegalArgumentException("modelScale must be finite and positive");
        }

        this.modelScale = ignored -> scale;

        return this;
    }

    public BoneHitboxManager modelScale(ToDoubleFunction<LivingEntity> scale) {
        this.modelScale = Objects.requireNonNull(scale, "scale");
        return this;
    }

    public void tick() {
        if (!active || owner.level().isClientSide) {
            return;
        }

        tickTimedEnables();

        if (rig != null) {
            for (final BoneHitbox hitbox : hitboxes.values()) {
                resolveAutoSize(hitbox);
            }

            final double scale = modelScale.applyAsDouble(owner);
            if (!Double.isFinite(scale) || scale <= 0d || scale > Float.MAX_VALUE) {
                throw new IllegalStateException("[KnightLib] BoneHitbox modelScale must be finite and positive");
            }

            rig.updatePose(owner, getTrackedBoneNames(), (float) scale, this::updateBoneTransform);
        }

        if (poseProvider != null) {
            poseProvider.updatePose(owner, this::updateBoneTransform);
        }

        recordSnapshot();

        for (final BoneHitbox hitbox : hitboxes.values()) {
            if (!hitbox.reactsOnContact() && hitCallback == null) {
                continue;
            }

            hitbox.tickCooldowns();

            if (!isHitboxActive(hitbox)) {
                continue;
            }

            if (!hasFreshTransform(hitbox.getBoneName())) {
                continue;
            }

            final OBB obb = hitbox.getCurrentOBB();
            if (obb == null) {
                continue;
            }

            // Sweeping between the previous and current tick pose keeps fast bones from tunneling through targets
            final List<OBB> sweep = buildSweepSamples(hitbox.getPreviousOBB(), obb);
            AABB broadPhase = null;
            for (final OBB sample : sweep) {
                final AABB sampleBounds = sample.enclosingAABB();
                broadPhase = broadPhase == null ? sampleBounds : broadPhase.minmax(sampleBounds);
            }

            final List<Entity> entities = owner.level().getEntities(owner, broadPhase, hitbox.getFilter());
            for (final Entity entity : entities) {
                if (entity == owner) {
                    continue;
                }
                if (hitbox.isOnCooldown(entity)) {
                    continue;
                }

                for (final OBB sample : sweep) {
                    if (sample.intersects(entity.getBoundingBox())) {
                        hitbox.applyCooldown(entity);

                        if (hitbox.reactsOnContact()) {
                            hitbox.handleContact(owner, entity);
                        }
                        else if (hitCallback != null) {
                            hitCallback.accept(hitbox, entity);
                        }

                        break;
                    }

                }

            }

        }

        for (final BoneHitbox hitbox : hitboxes.values()) {
            hitbox.capturePreviousOBB();
        }

    }

    private boolean isHitboxActive(BoneHitbox hitbox) {
        if (!hitbox.isEnabled() || !hitbox.testCondition()) {
            return false;
        }

        final List<BoneHitbox.ActivationWindow> windows = hitbox.getActivationWindows();
        if (windows.isEmpty()) {
            return true;
        }
        if (rig == null) {
            return false;
        }

        for (final BoneHitbox.ActivationWindow window : windows) {
            if (rig.isAnimationWithin(window.animation(), window.startTick(), window.endTick())) {
                return true;
            }

        }

        return false;
    }

    private void tickTimedEnables() {
        if (timedDisables.isEmpty()) {
            return;
        }

        timedDisables.entrySet().removeIf(entry -> {
            final int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                final BoneHitbox hitbox = hitboxes.get(entry.getKey());
                if (hitbox != null) {
                    hitbox.setEnabled(false);
                }

                return true;
            }

            entry.setValue(remaining);

            return false;
        });

    }

    private boolean hasFreshTransform(String boneName) {
        final Long lastUpdate = lastTransformTicks.get(boneName);
        return lastUpdate != null && owner.level().getGameTime() - lastUpdate <= transformTimeoutTicks;
    }

    private void recordSnapshot() {
        final Map<String, OBB> attackable = new HashMap<>();
        for (final BoneHitbox hitbox : hitboxes.values()) {
            final OBB obb = hitbox.getCurrentOBB();
            if (obb != null && isHitboxActive(hitbox) && hitbox.reactsWhenAttacked() && hasFreshTransform(hitbox.getBoneName())) {
                attackable.put(hitbox.getBoneName(), obb);
            }

        }

        history.addFirst(new PoseSnapshot(owner.level().getGameTime(), Map.copyOf(attackable)));
        while (history.size() > REWIND_TICKS) {
            history.removeLast();
        }

    }

    private static List<OBB> buildSweepSamples(@Nullable OBB previous, OBB current) {
        if (previous == null) {
            return List.of(current);
        }

        final double displacement = current.getCenter().distanceTo(previous.getCenter());
        final Vec3 halfExtents = current.getHalfExtents();
        final double minHalf = Math.max(0.05d, Math.min(halfExtents.x, Math.min(halfExtents.y, halfExtents.z)));
        if (displacement <= minHalf || displacement > MAX_SWEEP_DISTANCE) {
            return List.of(current);
        }

        final int steps = (int) Math.min(MAX_SWEEP_SAMPLES, Math.ceil(displacement / minHalf));
        final List<OBB> samples = new ArrayList<>(steps);
        final Quaternionf from = new Quaternionf().setFromNormalized(previous.toMatrix());
        final Quaternionf to = new Quaternionf().setFromNormalized(current.toMatrix());
        for (int i = 1; i < steps; i++) {
            final float t = (float) i / steps;
            final Vec3 center = previous.getCenter().lerp(current.getCenter(), t);
            final Matrix3f rotation = new Quaternionf(from).slerp(to, t).get(new Matrix3f());
            samples.add(new OBB(center, halfExtents, rotation));
        }

        samples.add(current);

        return samples;
    }

    public @Nullable RayHit rayTraceAttackable(Vec3 start, Vec3 end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!active) {
            return null;
        }

        final Vec3 direction = end.subtract(start);
        final double length = direction.length();
        if (length < 1.0E-8) {
            return null;
        }

        BoneHitbox closestHitbox = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (BoneHitbox hitbox : hitboxes.values()) {
            if (!isHitboxActive(hitbox) || !hitbox.reactsWhenAttacked()) {
                continue;
            }

            final OBB obb = hitbox.getCurrentOBB();
            if (obb == null) {
                continue;
            }

            final double distance = obb.rayIntersects(start, end);
            if (distance >= 0d && distance < closestDistance) {
                closestHitbox = hitbox;
                closestDistance = distance;
            }

        }

        if (closestHitbox == null) {
            return null;
        }

        final Vec3 location = start.add(direction.scale(closestDistance / length));
        return new RayHit(closestHitbox, location, closestDistance);
    }

    /**
     * Validates and executes an attacked-hitbox behavior on the server
     */
    public boolean handleAttack(String boneName, ServerPlayer attacker) {
        Objects.requireNonNull(boneName, "boneName");
        Objects.requireNonNull(attacker, "attacker");
        if (!active || owner.level().isClientSide || attacker.level() != owner.level() || attacker.isSpectator() || !attacker.isAlive() || !owner.isAlive()) {
            return false;
        }

        final BoneHitbox requested = hitboxes.get(boneName);
        if (requested == null || !requested.reactsWhenAttacked()) {
            return false;
        }

        final double reach = attacker.isCreative() ? 6d : 3d;
        final Vec3 start = attacker.getEyePosition();
        final Vec3 view = attacker.getViewVector(1f);
        final Vec3 end = start.add(view.scale(reach));

        final long now = owner.level().getGameTime();
        double closestDistance = Double.POSITIVE_INFINITY;
        for (final PoseSnapshot snapshot : history) {
            if (now - snapshot.gameTime() > REWIND_TICKS) {
                break;
            }

            final OBB obb = snapshot.attackableObbs().get(boneName);
            if (obb == null) {
                continue;
            }

            final double distance = obb.inflate(ATTACK_TOLERANCE).rayIntersects(start, end);
            if (distance >= 0d && distance < closestDistance) {
                closestDistance = distance;
            }

        }

        if (!Double.isFinite(closestDistance)) {
            return false;
        }

        final Vec3 location = start.add(view.scale(closestDistance));
        final BlockHitResult blockHit = owner.level().clip(new ClipContext(
                start,
                location,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                attacker
        ));

        if (blockHit.getType() != HitResult.Type.MISS && blockHit.getLocation().distanceToSqr(start) + 1.0E-6 < location.distanceToSqr(start)) {
            return false;
        }

        requested.handleAttack(owner, attacker);

        return true;
    }

    /**
     * Enables a specific hitbox by bone name
     */
    public void enable(String boneName) {
        final BoneHitbox hitbox = hitboxes.get(boneName);
        if (hitbox != null) {
            timedDisables.remove(boneName);
            hitbox.setEnabled(true);
        }

    }

    /**
     * Enables a hitbox now and automatically disables it {@code ticks} server ticks later
     */
    public void enableFor(String boneName, int ticks) {
        final BoneHitbox hitbox = hitboxes.get(boneName);
        if (hitbox != null && ticks > 0) {
            hitbox.setEnabled(true);
            timedDisables.put(boneName, ticks);
        }

    }

    /**
     * Disables a specific hitbox by bone name
     */
    public void disable(String boneName) {
        final BoneHitbox hitbox = hitboxes.get(boneName);
        if (hitbox != null) {
            timedDisables.remove(boneName);
            hitbox.setEnabled(false);
        }

    }

    /**
     * Disables all hitboxes
     */
    public void disableAll() {
        timedDisables.clear();
        hitboxes.values().forEach(boneHitbox -> boneHitbox.setEnabled(false));
    }

    /**
     * Enables all hitboxes
     */
    public void enableAll() {
        timedDisables.clear();
        hitboxes.values().forEach(boneHitbox -> boneHitbox.setEnabled(true));
    }

    /**
     * Whether any registered hitbox defines an attacked behavior
     */
    public boolean hasAttackableHitboxes() {
        for (final BoneHitbox hitbox : hitboxes.values()) {
            if (hitbox.reactsWhenAttacked()) {
                return true;
            }

        }

        return false;
    }

    public @Nullable Vec3 clipAttackable(Vec3 start, Vec3 end) {
        final RayHit hit = rayTraceAttackable(start, end);
        return hit == null ? null : hit.location();
    }

    /**
     * Clears all cooldowns across all hitboxes
     */
    public void clearAllCooldowns() {
        hitboxes.values().forEach(BoneHitbox::clearCooldowns);
    }

    /**
     * Returns the set of bone names that have registered hitboxes
     */
    public Set<String> getTrackedBoneNames() {
        return Collections.unmodifiableSet(hitboxes.keySet());
    }

    /**
     * Maximum ticks an authoritative transform remains usable without a fresh update
     */
    public BoneHitboxManager transformTimeout(int ticks) {
        this.transformTimeoutTicks = Math.max(0, ticks);
        return this;
    }

    /**
     * Returns the owning entity
     */
    public LivingEntity getOwner() {
        return owner;
    }

    private record PoseSnapshot(
            long gameTime,
            Map<String, OBB> attackableObbs
    ) {
        ;;
    }

    public record RayHit(
            BoneHitbox hitbox,
            Vec3 location,
            double distance
    ) {

        public RayHit {
            Objects.requireNonNull(hitbox, "hitbox");
            Objects.requireNonNull(location, "location");
            if (!Double.isFinite(distance) || distance < 0d) {
                throw new IllegalArgumentException("distance must be finite and non-negative");
            }

        }

    }

    private void resolveAutoSize(BoneHitbox hitbox) {
        if (rig == null || !hitbox.isAutoSize() || hitbox.getBaseHalfExtents() != null) {
            return;
        }

        final Vec3 halfExtents = rig.boneHalfExtents(hitbox.getBoneName());
        if (halfExtents != null && halfExtents.x > 1.0E-6 && halfExtents.y > 1.0E-6 && halfExtents.z > 1.0E-6) {
            hitbox.setBaseHalfExtents(halfExtents);

            final Vec3 centerOffset = rig.boneCenterOffset(hitbox.getBoneName());
            hitbox.setAutoCenterOffset(centerOffset == null ? Vec3.ZERO : centerOffset);
        }

    }

    private static void validateTransform(Vec3 position, Matrix3f rotation, float scaleX, float scaleY, float scaleZ) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(rotation, "rotation");
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z) || !Float.isFinite(scaleX) || !Float.isFinite(scaleY) || !Float.isFinite(scaleZ)) {
            throw new IllegalArgumentException("[KnightLib] Bone transform components must be finite");
        }

        final float[] values = {
                rotation.m00, rotation.m01, rotation.m02,
                rotation.m10, rotation.m11, rotation.m12,
                rotation.m20, rotation.m21, rotation.m22
        };
        for (final float value : values) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("[KnightLib] Bone rotation components must be finite");
            }

        }

    }

    private static Matrix3f normalizedRotation(Matrix3f source) {
        final Vector3f x = source.getColumn(0, new Vector3f());
        final Vector3f y = source.getColumn(1, new Vector3f());
        final Vector3f originalZ = source.getColumn(2, new Vector3f());
        if (x.lengthSquared() < 1.0E-12f || y.lengthSquared() < 1.0E-12f || originalZ.lengthSquared() < 1.0E-12f) {
            throw new IllegalArgumentException("[KnightLib] Bone rotation axes must be non-zero");
        }

        x.normalize();
        y.fma(-x.dot(y), x);
        if (y.lengthSquared() < 1.0E-12f) {
            throw new IllegalArgumentException("[KnightLib] Bone rotation axes must not be parallel");
        }

        y.normalize();
        final Vector3f z = x.cross(y, new Vector3f()).normalize();
        if (z.dot(originalZ) < 0f) {
            z.negate();
        }

        return new Matrix3f().setColumn(0, x).setColumn(1, y).setColumn(2, z);
    }

}