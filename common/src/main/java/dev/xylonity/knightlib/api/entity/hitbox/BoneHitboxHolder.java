package dev.xylonity.knightlib.api.entity.hitbox;

import org.jetbrains.annotations.Nullable;

/**
 * Interface for entities that expose a {@link BoneHitboxManager}.
 * Implement this on any entity that needs bone-tracked hitboxes.
 *
 * <pre>{@code
 * public class TestEntity extends Monster implements KnightLibAnimatable, BoneHitboxHolder {
 *
 *     private final BoneHitboxManager hitboxManager = new BoneHitboxManager(this)
 *         .geo(MODEL, ANIMATIONS);
 *
 *     public TestEntity(...) {
 *         hitboxManager.add(BoneHitbox.create("axe_tip")
 *             .cooldown(10)
 *             .damageOnContact(8)
 *             .damageOwner());
 *     }
 *
 *     @Override
 *     public void tick() {
 *         super.tick();
 *         if (!level().isClientSide) {
 *             hitboxManager.tick();
 *         }
 *
 *     }
 *
 *     @Override
 *     public BoneHitboxManager getBoneHitboxManager() {
 *         return hitboxManager;
 *     }
 *
 * }
 * }</pre>
 */
@FunctionalInterface
public interface BoneHitboxHolder {

    @Nullable
    BoneHitboxManager getBoneHitboxManager();

}