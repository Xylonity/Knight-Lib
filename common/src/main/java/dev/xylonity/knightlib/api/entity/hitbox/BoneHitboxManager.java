package dev.xylonity.knightlib.api.entity.hitbox;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Manages all bone-attached hitboxes for a single entity.
 * {@link BoneHitboxHolder}
 */
public class BoneHitboxManager {

    private final LivingEntity owner;
    private final Map<String, BoneHitbox> hitboxes = new LinkedHashMap<>();
    private boolean active = true;

    private @Nullable BiConsumer<BoneHitbox, Entity> hitCallback;

    public BoneHitboxManager(LivingEntity owner) {
        this.owner = owner;
    }

    /**
     * Registers a bone hitbox (the bone name must be unique within this manager)
     */
    public BoneHitboxManager add(BoneHitbox hitbox) {
        hitboxes.put(hitbox.getBoneName(), hitbox);
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
        return hitboxes.values();
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
     * Sets the callback that fires when any hitbox collides with an entity
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
     * Called from the client render layer (and from the server after receiving sync packets) to update a bone's world-space transform
     */
    public void updateBoneTransform(String boneName, Vec3 worldPos, Matrix3f worldRotation) {
        final BoneHitbox hitbox = hitboxes.get(boneName);
        if (hitbox != null) {
            hitbox.updateFromBoneTransform(worldPos, worldRotation);
        }

    }

    /**
     * Server-side tick. Checks all active, enabled hitboxes for collisions with nearby entities and fires the hit callback.
     */
    public void tick() {
        if (!active || owner.level().isClientSide) {
            return;
        }

        for (final BoneHitbox hitbox : hitboxes.values()) {
            hitbox.tickCooldowns();

            if (!hitbox.isEnabled()) {
                continue;
            }

            final OBB obb = hitbox.getCurrentOBB();
            if (obb == null) {
                continue;
            }

            final AABB broadPhase = obb.enclosingAABB();
            final List<Entity> entities = owner.level().getEntities(owner, broadPhase, hitbox.getFilter());
            for (final Entity entity : entities) {
                if (entity == owner) {
                    continue;
                }
                if (hitbox.isOnCooldown(entity)) {
                    continue;
                }

                if (obb.intersects(entity.getBoundingBox())) {
                    hitbox.applyCooldown(entity);

                    if (hitCallback != null) {
                        hitCallback.accept(hitbox, entity);
                    }
                }

            }

        }

    }

    /**
     * Enables a specific hitbox by bone name
     */
    public void enable(String boneName) {
        final BoneHitbox hitbox = hitboxes.get(boneName);
        if (hitbox != null) {
            hitbox.setEnabled(true);
        }

    }

    /**
     * Disables a specific hitbox by bone name
     */
    public void disable(String boneName) {
        final BoneHitbox hitbox = hitboxes.get(boneName);
        if (hitbox != null) {
            hitbox.setEnabled(false);
        }

    }

    /**
     * Disables all hitboxes
     */
    public void disableAll() {
        hitboxes.values().forEach(boneHitbox -> boneHitbox.setEnabled(false));
    }

    /**
     * Enables all hitboxes
     */
    public void enableAll() {
        hitboxes.values().forEach(boneHitbox -> boneHitbox.setEnabled(true));
    }

    /**
     * Clears all cooldowns across all hitboxes
     */
    public void clearAllCooldowns() {
        hitboxes.values().forEach(BoneHitbox::clearCooldowns);
    }

    /**
     * Returns the set of bone names that have registered hitboxes (used by the render layer to know which bones need matrix tracking=
     */
    public Set<String> getTrackedBoneNames() {
        return hitboxes.keySet();
    }

    /**
     * Returns the owning entity
     */
    public LivingEntity getOwner() {
        return owner;
    }

}
