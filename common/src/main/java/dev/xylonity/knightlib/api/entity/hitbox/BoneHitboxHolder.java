package dev.xylonity.knightlib.api.entity.hitbox;

import dev.xylonity.knightlib.client.entity.layer.BoneHitboxLayer;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for entities that expose a {@link BoneHitboxManager}.
 * Implement this on any entity that needs bone-tracked hitboxes.
 * <p>
 * The entity must also:
 *    - Create a {@link BoneHitboxManager} and register hitboxes via {@link BoneHitboxManager#add}
 *    - Call {@link BoneHitboxManager#tick()} from its server-side tick
 *    - Add a {@link BoneHitboxLayer} to its GeckoLib renderer
 *
 * <pre>{@code
 * public class TestEntity extends Monster implements GeoEntity, BoneHitboxHolder {
 *
 *     private final BoneHitboxManager hitboxManager = new BoneHitboxManager(this);
 *
 *     public TestEntity(...) {
 *         hitboxManager.add(BoneHitbox.create("axe_tip".cooldown(10));
 *         hitboxManager.onHit((hitbox, target) -> { ... });
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
