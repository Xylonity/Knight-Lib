package dev.xylonity.knightlib.api.animation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable context handed to a client controller while its selector is being evaluated.
 *
 * <p>Controllers run once per client tick from {@link KnightLibAnimationHandler#tick()}, not once per frame. This same wrapper is
 * reused per controller so selectors must not keep it.</p>
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/animation/AnimationState.java
 */
public final class KnightLibAnimationState {

    public static final double DEFAULT_MOVEMENT_THRESHOLD = 1.0E-3D;

    private final KnightLibAnimationHandler handler;

    private String controller;
    private Entity entity;
    private BlockEntity blockEntity;
    private ItemStack stack;
    private Level level;
    private Entity sampledEntity;
    private Level sampledLevel;
    private long sampledGameTime = Long.MIN_VALUE;
    private double sampledX;
    private double sampledZ;
    private double blocksPerTick;

    KnightLibAnimationState(KnightLibAnimationHandler handler) {
        this.handler = handler;
    }

    void refresh(Entity entity, BlockEntity blockEntity, ItemStack stack, Level level) {
        this.entity = entity;
        this.blockEntity = blockEntity;
        this.stack = stack;
        this.level = level;
        updateMovementSample(entity, level);
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
        return isMoving(DEFAULT_MOVEMENT_THRESHOLD);
    }

    /**
     * Whether the entity moved by more than {@code minimumBlocksPerTick} during the last sampled
     * tick. Unlike vanilla limb swing, this does not decay for several ticks after stopping.
     */
    public boolean isMoving(double minimumBlocksPerTick) {
        return entity != null && blocksPerTick > Math.max(0.0D, minimumBlocksPerTick);
    }

    /**
     * Horizontal blocks traveled per tick between the last two controller updates
     */
    public double blocksPerTick() {
        return blocksPerTick;
    }

    /**
     * Horizontal movement speed in blocks per second
     */
    public double blocksPerSecond() {
        return blocksPerTick * 20.0D;
    }

    private void updateMovementSample(Entity entity, Level level) {
        if (entity == null || level == null) {
            sampledEntity = null;
            sampledLevel = null;
            sampledGameTime = Long.MIN_VALUE;
            blocksPerTick = 0.0D;
            return;
        }

        final long gameTime = level.getGameTime();
        if (sampledEntity != entity || sampledLevel != level || gameTime < sampledGameTime) {
            sampledEntity = entity;
            sampledLevel = level;
            sampledGameTime = gameTime;
            sampledX = entity.getX();
            sampledZ = entity.getZ();
            blocksPerTick = 0.0D;
            return;
        }

        final long elapsedTicks = gameTime - sampledGameTime;
        if (elapsedTicks == 0L) {
            return;
        }

        final double x = entity.getX();
        final double z = entity.getZ();
        final double dx = x - sampledX;
        final double dz = z - sampledZ;

        blocksPerTick = Math.sqrt(dx * dx + dz * dz) / elapsedTicks;
        sampledGameTime = gameTime;
        sampledX = x;
        sampledZ = z;
    }

}