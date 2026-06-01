package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fired when an entity places a block
 */
public class EntityPlaceBlockEvent extends KnightLibEvent {

    private final Level level;
    private final BlockPos clickedPosition;
    private final BlockState placedBlock;
    private final Entity entity;
    private boolean cancelled;

    public EntityPlaceBlockEvent(Level level, BlockPos clickedPosition, BlockState placedBlock, Entity entity) {
        this.level = level;
        this.clickedPosition = clickedPosition;
        this.placedBlock = placedBlock;
        this.entity = entity;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getClickedPosition() {
        return clickedPosition;
    }

    public BlockState getPlacedBlock() {
        return placedBlock;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}