package dev.xylonity.knightlib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks an `AbstractTickBlock` tick counter for each block instance to avoid inheriting multiple
 * scheduled ticks into a single block instance, "killing" in some way vanilla parallelism.
 *
 * @author Xylonity
 */
public interface ITickBlockTracker {

    Map<BlockPos, Block> blockTickMap = new HashMap<>();
    Map<BlockPos, Integer> tickCounts = new HashMap<>();

    default void addTickBlock(BlockPos pos, Block block) {
        blockTickMap.put(pos, block);
        tickCounts.put(pos, 0);
    }

    default void removeTickBlock(BlockPos pos) {
        blockTickMap.remove(pos);
        tickCounts.remove(pos);
    }

    default int getTickCount(BlockPos pos) {
        return tickCounts.getOrDefault(pos, 0);
    }

    default void incrementTickCount(BlockPos pos) {
        tickCounts.put(pos, getTickCount(pos) + 1);
    }

    default Block getTickBlock(BlockPos pos) {
        return blockTickMap.get(pos);
    }

    default void resetTickCount(BlockPos pos) {
        tickCounts.put(pos, 0);
    }

}
