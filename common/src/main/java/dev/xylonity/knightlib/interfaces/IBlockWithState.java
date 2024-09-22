package dev.xylonity.knightlib.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IBlockWithState {

    /**
     * Sets a custom state for the block.
     *
     * @param level The level where the block is located.
     * @param pos The position of the block.
     * @param state The custom state to set.
     */
    void setCustomState(Level level, BlockPos pos, int state);

    /**
     * Retrieves the current state of the block.
     *
     * @param level The level where the block is located.
     * @param pos The position of the block.
     * @return The current custom state of the block.
     */
    int getCustomState(Level level, BlockPos pos);

    /**
     * Toggles between the available states of the block.
     *
     * @param level The level where the block is located.
     * @param pos The position of the block.
     */
    void toggleState(Level level, BlockPos pos);

}
