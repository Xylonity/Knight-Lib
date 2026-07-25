package dev.xylonity.knightlib.api.animation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Context handed to a client controller while it is being evaluated.
 *
 * <p>Controllers run once per client tick from {@link KnightLibAnimationHandler#tick()}, not once per frame</p>
 *
 * This same state wrapper is reused on every controller of the same handler.
 */
public final class KnightLibAnimationState {

    private final KnightLibAnimationHandler handler;

    private String controller;
    private Entity entity;
    private BlockEntity blockEntity;
    private ItemStack stack;
    private Level level;

    KnightLibAnimationState(KnightLibAnimationHandler handler) {
        this.handler = handler;
    }

    void refresh(Entity entity, BlockEntity blockEntity, ItemStack stack, Level level) {
        this.entity = entity;
        this.blockEntity = blockEntity;
        this.stack = stack;
        this.level = level;
    }

    void controller(String controller) {
        this.controller = controller;
    }

    /**
     * Controller being evaluated by this selector
     */
    public String controller() {
        return controller;
    }

    public Level level() {
        return level;
    }

    /**
     * Entity this animation belongs to. The animated entity itself, or the holder for an item controller. Null for a block entity,
     * and also for an item that is not currently held or worn by anything.
     */
    public @Nullable Entity entity() {
        return entity;
    }

    /**
     * Animated block entity or null for an entity/item controller
     */
    public @Nullable BlockEntity blockEntity() {
        return blockEntity;
    }

    /**
     * Stack being evaluated or null for an entity/block-entity controller
     */
    public @Nullable ItemStack stack() {
        return stack;
    }

    /**
     * Animation this controller is currently playing or null when it is stopped
     */
    public @Nullable String current() {
        return controller == null ? null : handler.getActiveAnimation(controller);
    }

    /**
     * Whether this controller is currently playing an animation.
     */
    public boolean isPlaying(String animation) {
        return animation != null && animation.equals(current());
    }

    /**
     * Vanilla limb-swing position.
     */
    public float limbSwing() {
        return entity instanceof LivingEntity living ? living.walkAnimation.position() : 0f;
    }

    /**
     * Vanilla limb-swing amount, basically how fast the entity is walking.
     */
    public float limbSwingAmount() {
        return entity instanceof LivingEntity living ? living.walkAnimation.speed() : 0f;
    }

    /**
     * Whether the entity is moving.
     */
    public boolean isMoving() {
        final float movementThreshold = 0.01f;
        if (entity instanceof LivingEntity living) {
            return living.walkAnimation.speed() > movementThreshold;
        }
        if (entity == null) {
            return false;
        }

        final Vec3 movement = entity.getDeltaMovement();
        return movement.horizontalDistanceSqr() > movementThreshold * movementThreshold;
    }

}
