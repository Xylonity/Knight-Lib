package dev.xylonity.knightlib.api.entity.hitbox;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Defines a hitbox attached to a named KnightLib model bone.
 * <p>
 * Each hitbox has a size (half-extents or derived from the bone geometry), an optional offset from
 * the bone pivot, a target filter and its own optional behaviors
 * <p>
 * <pre>{@code
 * // Explicit size
 * BoneHitbox axeTip = BoneHitbox.create("axe_tip", 0.4, 0.4, 0.8).damageOnContact(8).damageOwner();
 *
 * // Autosized from the configured model cubes
 * BoneHitbox axeTip2 = BoneHitbox.create("axeTip2").cooldown(10);
 * }</pre>
 */
public class BoneHitbox {

    private final String boneName;
    private @Nullable Vec3 baseHalfExtents;
    private @Nullable Vec3 halfExtents;
    private final boolean autoSize;

    private Vec3 offset = Vec3.ZERO;
    private Vec3 autoCenterOffset = Vec3.ZERO;
    private int cooldownTicks = 10;
    private boolean enabled = true;
    private final List<ActivationWindow> activationWindows = new ArrayList<>();
    private @Nullable BooleanSupplier condition;
    private Predicate<Entity> filter = entity -> entity instanceof LivingEntity;

    private @Nullable OBB currentOBB;
    private @Nullable OBB previousOBB;
    private @Nullable ContactHandler contactHandler;
    private @Nullable AttackHandler attackHandler;
    private final Map<Integer, Integer> hitCooldowns = new HashMap<>();

    private BoneHitbox(String boneName, @Nullable Vec3 halfExtents, boolean autoSize) {
        if (boneName == null || boneName.isBlank() || boneName.length() > 64) {
            throw new IllegalArgumentException("[KnightLib] boneName must contain 1..64 characters");
        }
        if (halfExtents != null) {
            validateHalfExtents(halfExtents);
        }

        this.boneName = boneName;
        this.baseHalfExtents = halfExtents;
        this.halfExtents = halfExtents;
        this.autoSize = autoSize;
    }

    /**
     * Creates a bone hitbox with explicit half-extents (in blocks)
     *
     * @param boneName the name of the model bone to attach to
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
     * Creates a bone hitbox whose size is automatically derived from the bone's cube geometry
     */
    public static BoneHitbox create(String boneName) {
        return new BoneHitbox(boneName, null, true);
    }

    /**
     * Sets the cooldown (in ticks) before this hitbox can hit the same entity again
     */
    public BoneHitbox cooldown(int ticks) {
        this.cooldownTicks = Math.max(0, ticks);
        return this;
    }

    /**
     * Offset from the bone pivot (in blocks), in the bone's local coordinate space
     */
    public BoneHitbox offset(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("[KnightLib] offset components must be finite");
        }

        this.offset = new Vec3(x, y, z);

        return this;
    }

    /**
     * Declares an activation window tied to the animation timeline. The hitbox only collides
     * while an animation is playing (ticks at 20 per sec)
     */
    public BoneHitbox activeDuring(String animation, float startTick, float endTick) {
        if (animation == null || animation.isBlank()) {
            throw new IllegalArgumentException("[KnightLib] animation must not be blank");
        }
        if (!Float.isFinite(startTick) || !Float.isFinite(endTick) || startTick < 0f || endTick <= startTick) {
            throw new IllegalArgumentException("[KnightLib] window must be 0 <= startTick < endTick");
        }

        activationWindows.add(new ActivationWindow(animation, startTick, endTick));

        return this;
    }

    /**
     * Additional activation condition evaluated every collision pass
     */
    public BoneHitbox enabledWhen(BooleanSupplier condition) {
        this.condition = Objects.requireNonNull(condition, "condition");
        return this;
    }

    /**
     * Predicate to filter which entities can be hit
     */
    public BoneHitbox filter(Predicate<Entity> filter) {
        this.filter = Objects.requireNonNull(filter, "filter");
        return this;
    }

    /**
     * Defines what this hitbox does when its animated volume touches another entity
     */
    public BoneHitbox onContact(ContactHandler handler) {
        this.contactHandler = Objects.requireNonNull(handler, "handler");
        return this;
    }

    /**
     * Defines what happens on the server when a player attacks this animated volume
     */
    public BoneHitbox onAttacked(AttackHandler handler) {
        this.attackHandler = Objects.requireNonNull(handler, "handler");
        return this;
    }

    /**
     * Contact behavior that deals a fixed amount of damage
     */
    public BoneHitbox damageOnContact(float amount) {
        validateDamage(amount);
        return onContact((hitbox, owner, target) -> {
            if (target instanceof LivingEntity living) {
                living.hurt(owner.damageSources().mobAttack(owner), amount);
            }

        });

    }

    /**
     * Attacked behavior that performs the attack through the player
     */
    public BoneHitbox damageOwner() {
        return onAttacked((hitbox, owner, attacker) -> attacker.attack(owner));
    }

    public BoneHitbox damageOwner(float amount) {
        validateDamage(amount);
        return onAttacked((hitbox, owner, attacker) -> owner.hurt(owner.damageSources().playerAttack(attacker), amount));
    }

    public String getBoneName() {
        return boneName;
    }

    /**
     * Returns the effective half-extents (base * bone scale), or {@code null} if not yet computed
     */
    public @Nullable Vec3 getHalfExtents() {
        return halfExtents;
    }

    /**
     * Returns the base half-extents before bone scale is applied
     */
    public @Nullable Vec3 getBaseHalfExtents() {
        return baseHalfExtents;
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

    List<ActivationWindow> getActivationWindows() {
        return Collections.unmodifiableList(activationWindows);
    }

    boolean testCondition() {
        return condition == null || condition.getAsBoolean();
    }

    public Predicate<Entity> getFilter() {
        return filter;
    }

    public boolean reactsOnContact() {
        return contactHandler != null;
    }

    public boolean reactsWhenAttacked() {
        return attackHandler != null;
    }

    public @Nullable OBB getCurrentOBB() {
        return currentOBB;
    }

    /**
     * Returns the OBB captured at the end of the previous server tick
     */
    public @Nullable OBB getPreviousOBB() {
        return previousOBB;
    }

    void capturePreviousOBB() {
        this.previousOBB = currentOBB;
    }

    /**
     * Sets the trusted base half-extents before animated bone scale is applied
     */
    public void setBaseHalfExtents(Vec3 baseHalfExtents) {
        validateHalfExtents(baseHalfExtents);
        this.baseHalfExtents = baseHalfExtents;
        this.halfExtents = baseHalfExtents;
    }

    /**
     * Sets the effective half-extents directly (already scaled)
     */
    public void setHalfExtents(@Nullable Vec3 halfExtents) {
        if (halfExtents != null) {
            validateHalfExtents(halfExtents);
        }

        this.halfExtents = halfExtents;
    }

    void clearAutoSize() {
        if (autoSize) {
            this.baseHalfExtents = null;
            this.halfExtents = null;
            this.currentOBB = null;
            this.previousOBB = null;
            this.autoCenterOffset = Vec3.ZERO;
        }

    }

    void setAutoCenterOffset(Vec3 offset) {
        this.autoCenterOffset = Objects.requireNonNull(offset, "offset");
    }

    /**
     * Applies bone scale to the base half-extents, updating the effective size
     *
     * @param scaleX scale factor along the bone's local X axis
     * @param scaleY scale factor along the bone's local Y axis
     * @param scaleZ scale factor along the bone's local Z axis
     */
    public void applyScale(float scaleX, float scaleY, float scaleZ) {
        if (baseHalfExtents == null) {
            return;
        }
        if (!Float.isFinite(scaleX) || !Float.isFinite(scaleY) || !Float.isFinite(scaleZ)) {
            throw new IllegalArgumentException("[KnightLib] scale components must be finite");
        }

        this.halfExtents = new Vec3(
                baseHalfExtents.x * Math.abs(scaleX),
                baseHalfExtents.y * Math.abs(scaleY),
                baseHalfExtents.z * Math.abs(scaleZ)
        );

    }

    /**
     * Updates the OBB from the bone's world-space position and rotation (the offset is applied in the bone's
     * local coordinate space before converting to world space)
     */
    public boolean updateFromBoneTransform(Vec3 boneWorldPos, Matrix3f boneWorldRotation) {
        return updateFromBoneTransform(boneWorldPos, boneWorldRotation, 1f, 1f, 1f);
    }

    public boolean updateFromBoneTransform(Vec3 boneWorldPos, Matrix3f boneWorldRotation, float scaleX, float scaleY, float scaleZ) {
        if (!Float.isFinite(scaleX) || !Float.isFinite(scaleY) || !Float.isFinite(scaleZ)) {
            throw new IllegalArgumentException("[KnightLib] scale components must be finite");
        }
        if (!hasUsableHalfExtents()) {
            currentOBB = null;
            return false;
        }


        Vec3 worldOffset = Vec3.ZERO;
        final Vec3 localOffset = offset.add(autoCenterOffset);
        if (localOffset.lengthSqr() > 0) {
            final float ox = (float) localOffset.x * scaleX;
            final float oy = (float) localOffset.y * scaleY;
            final float oz = (float) localOffset.z * scaleZ;
            worldOffset = new Vec3(
                    boneWorldRotation.m00() * ox + boneWorldRotation.m10() * oy + boneWorldRotation.m20() * oz,
                    boneWorldRotation.m01() * ox + boneWorldRotation.m11() * oy + boneWorldRotation.m21() * oz,
                    boneWorldRotation.m02() * ox + boneWorldRotation.m12() * oy + boneWorldRotation.m22() * oz
            );

        }

        this.currentOBB = new OBB(boneWorldPos.add(worldOffset), halfExtents, boneWorldRotation);

        return true;
    }

    private boolean hasUsableHalfExtents() {
        return halfExtents != null && halfExtents.x > 1.0E-6 && halfExtents.y > 1.0E-6 && halfExtents.z > 1.0E-6;
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

    void handleContact(LivingEntity owner, Entity target) {
        if (contactHandler != null) {
            contactHandler.handle(this, owner, target);
        }

    }

    void handleAttack(LivingEntity owner, ServerPlayer attacker) {
        if (attackHandler != null) {
            attackHandler.handle(this, owner, attacker);
        }

    }

    private static void validateDamage(float amount) {
        if (!Float.isFinite(amount) || amount < 0f) {
            throw new IllegalArgumentException("[KnightLib] damage must be finite and non-negative");
        }

    }

    private static void validateHalfExtents(Vec3 value) {
        Objects.requireNonNull(value, "halfExtents");
        if (!Double.isFinite(value.x) || !Double.isFinite(value.y) || !Double.isFinite(value.z) || value.x <= 0 || value.y <= 0 || value.z <= 0) {
            throw new IllegalArgumentException("[KnightLib] half-extents must be finite and positive");
        }

    }

    public record ActivationWindow(
            String animation,
            float startTick,
            float endTick
    ) {
        ;;
    }

    @FunctionalInterface
    public interface ContactHandler {
        void handle(BoneHitbox hitbox, LivingEntity owner, Entity target);
    }

    @FunctionalInterface
    public interface AttackHandler {
        void handle(BoneHitbox hitbox, LivingEntity owner, ServerPlayer attacker);
    }

}