package dev.xylonity.knightlib.api.entity.hitbox;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Defines a hitbox attached to a specific GeckoLib bone.
 * <p>
 * Each hitbox has a size (half-extents or derived from the bone geometry), an optional offset from the
 * bone pivot, a target filter, and a hit cooldown (hit behavior is not defined here, refer to {@link BoneHitboxManager#onHit}).
 * <p>
 * <pre>{@code
 * // Explicit size
 * BoneHitbox axeTip = BoneHitbox.create("axe_tip", 0.4, 0.4, 0.8)
 *     .cooldown(10);
 *
 * // Autosized from the bone's cubes in the model
 * BoneHitbox axeTip2 = BoneHitbox.create("axeTip2")
 *     .cooldown(10);
 * }</pre>
 */
public class BoneHitbox {

    private final String boneName;
    private @Nullable Vec3 halfExtents;
    private final boolean autoSize;

    private Vec3 offset = Vec3.ZERO;
    private int cooldownTicks = 10;
    private boolean enabled = true;
    private Predicate<Entity> filter = entity -> entity instanceof LivingEntity;

    private @Nullable OBB currentOBB;
    private final Map<Integer, Integer> hitCooldowns = new HashMap<>();

    private BoneHitbox(String boneName, @Nullable Vec3 halfExtents, boolean autoSize) {
        this.boneName = boneName;
        this.halfExtents = halfExtents;
        this.autoSize = autoSize;
    }

    /**
     * Creates a bone hitbox with explicit half-extents (in blocks)
     *
     * @param boneName the name of the GeckoLib bone to attach to
     * @param halfX half-width (X axis)
     * @param halfY half-height (Y axis)
     * @param halfZ half-depth (Z axis)
     */
    public static BoneHitbox create(String boneName, double halfX, double halfY, double halfZ) {
        return new BoneHitbox(boneName, new Vec3(halfX, halfY, halfZ), false);
    }

    /**
     * Creates a bone hitbox with uniform explicit size (in blocks)
     */
    public static BoneHitbox create(String boneName, double halfSize) {
        return create(boneName, halfSize, halfSize, halfSize);
    }

    /**
     * Creates a bone hitbox whose size is automatically derived from the bone's cube geometry in the GeckoLib
     * model. The hitbox will encompass all cubes of the bone.
     * <p>
     * The position is always the bone's pivot point.
     */
    public static BoneHitbox create(String boneName) {
        return new BoneHitbox(boneName, null, true);
    }

    /**
     * Sets the cooldown (in ticks) before this hitbox can hit the same entity again
     */
    public BoneHitbox cooldown(int ticks) {
        this.cooldownTicks = ticks;
        return this;
    }

    /**
     * Offset from the bone pivot (in blocks), in the bone's local coordinate space
     */
    public BoneHitbox offset(double x, double y, double z) {
        this.offset = new Vec3(x, y, z);
        return this;
    }

    /**
     * Predicate to filter which entities can be hit
     */
    public BoneHitbox filter(Predicate<Entity> filter) {
        this.filter = filter;
        return this;
    }

    public String getBoneName() {
        return boneName;
    }

    /**
     * Returns the half-extents, or {@code null} if autosized and not yet computed
     */
    public @Nullable Vec3 getHalfExtents() {
        return halfExtents;
    }

    public boolean isAutoSize() {
        return autoSize;
    }

    public Vec3 getOffset() {
        return offset;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Predicate<Entity> getFilter() {
        return filter;
    }

    public @Nullable OBB getCurrentOBB() {
        return currentOBB;
    }

    /**
     * Sets the half-extents, used by the render layer when autosizing from bone cubes
     */
    public void setHalfExtents(@Nullable Vec3 halfExtents) {
        this.halfExtents = halfExtents;
    }

    /**
     * Updates the OBB from the bone's world-space position and rotation (the offset is applied in the bone's
     * local coordinate space before converting to world space)
     */
    public void updateFromBoneTransform(Vec3 boneWorldPos, Matrix3f boneWorldRotation) {
        if (halfExtents == null) {
            return;
        }

        Vec3 worldOffset = Vec3.ZERO;
        if (offset.lengthSqr() > 0) {
            float ox = (float) offset.x;
            float oy = (float) offset.y;
            float oz = (float) offset.z;
            worldOffset = new Vec3(
                    boneWorldRotation.m00() * ox + boneWorldRotation.m10() * oy + boneWorldRotation.m20() * oz,
                    boneWorldRotation.m01() * ox + boneWorldRotation.m11() * oy + boneWorldRotation.m21() * oz,
                    boneWorldRotation.m02() * ox + boneWorldRotation.m12() * oy + boneWorldRotation.m22() * oz
            );

        }

        this.currentOBB = new OBB(boneWorldPos.add(worldOffset), halfExtents, boneWorldRotation);
    }

    /**
     * Checks if the given entity is on cooldown (recently hit by this hitbox)
     */
    public boolean isOnCooldown(Entity target) {
        Integer remaining = hitCooldowns.get(target.getId());
        return remaining != null && remaining > 0;
    }

    /**
     * Puts the given entity on cooldown
     */
    public void applyCooldown(Entity target) {
        hitCooldowns.put(target.getId(), cooldownTicks);
    }

    /**
     * Ticks down all cooldowns
     */
    public void tickCooldowns() {
        hitCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });

    }

    /**
     * Clears all active cooldowns
     */
    public void clearCooldowns() {
        hitCooldowns.clear();
    }

}
