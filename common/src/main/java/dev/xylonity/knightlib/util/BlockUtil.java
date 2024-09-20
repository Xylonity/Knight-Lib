package dev.xylonity.knightlib.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for handling various block-related tasks.
 *
 * @author Xylonity
 */
public class BlockUtil {

    /**
     * Checks if a block at a given position is air.
     *
     * @param level The level where the block is located.
     * @param pos The position of the block to check.
     * @return True if the block is air, false otherwise.
     */
    public static boolean isAirBlock(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir();
    }

    /**
     * Replaces a block at a specific position with another block.
     *
     * @param level The level where the block is located.
     * @param pos The position of the block to be replaced.
     * @param newBlock The block that will replace the existing block.
     */
    public static void replaceBlock(ServerLevel level, BlockPos pos, Block newBlock) {
        level.setBlockAndUpdate(pos, newBlock.defaultBlockState());
    }

    /**
     * Drops a block as an item at its current position.
     *
     * @param level The level where the block is located.
     * @param pos The position of the block to drop.
     */
    public static void dropBlockAsItem(ServerLevel level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        Block.dropResources(blockState, level, pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    /**
     * Checks if a block at a specific position is solid (i.e., it has a solid top face).
     *
     * @param level The level where the block is located.
     * @param pos The position of the block to check.
     * @return True if the block is solid, false otherwise.
     */
    public static boolean isBlockSolid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender(level, pos);
    }

    /**
     * Tries to place a block at a specific position, ensuring the position is valid for placement.
     *
     * @param level The level where the block will be placed.
     * @param pos The position where the block will be placed.
     * @param block The block to place.
     * @param player The player placing the block (used for placing logic, can be null).
     * @return True if the block was placed successfully, false otherwise.
     */
    public static boolean tryPlaceBlock(ServerLevel level, BlockPos pos, Block block, Player player) {
        if (level.getBlockState(pos).isAir()) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            return true;
        }
        return false;
    }

    /**
     * Retrieves the hardness of a block at a specific position.
     *
     * @param level The level where the block is located.
     * @param pos The position of the block to check.
     * @return The hardness of the block.
     */
    public static float getBlockHardness(ServerLevel level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        return blockState.getDestroySpeed(level, pos);
    }

    /**
     * Checks if a block is exposed to direct sunlight (i.e., no blocks above it).
     *
     * @param level The level where the block is located.
     * @param pos The position of the block to check.
     * @return True if the block is exposed to direct sunlight, false otherwise.
     */
    public static boolean isBlockExposedToSunlight(ServerLevel level, BlockPos pos) {
        return level.canSeeSky(pos);
    }

    /**
     * Simulates a block being broken by a player, dropping its resources and removing the block.
     *
     * @param level The level where the block is located.
     * @param pos The position of the block to break.
     * @param player The player who breaks the block.
     * @return True if the block was successfully broken, false otherwise.
     */
    public static boolean breakBlockAsPlayer(ServerLevel level, BlockPos pos, Player player) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState.getDestroySpeed(level, pos) >= 0) {
            blockState.getBlock().playerDestroy(level, player, pos, blockState, null, player.getMainHandItem());
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        return false;
    }

    /**
     * Checks if a block is surrounded by solid blocks on all sides.
     *
     * @param level The level where the block is located.
     * @param pos The position of the block to check.
     * @return True if the block is completely surrounded by solid blocks, false otherwise.
     */
    public static boolean isBlockSurroundedBySolidBlocks(ServerLevel level, BlockPos pos) {
        return isBlockSolid(level, pos.above()) &&
                isBlockSolid(level, pos.below()) &&
                isBlockSolid(level, pos.north()) &&
                isBlockSolid(level, pos.south()) &&
                isBlockSolid(level, pos.west()) &&
                isBlockSolid(level, pos.east());
    }

    /**
     * Scans an area to count how many blocks of a specific type exist within the given region.
     *
     * @param level The level where the blocks are located.
     * @param fromPos The starting position of the area.
     * @param toPos The ending position of the area.
     * @param blockToCount The block type to count.
     * @return The number of blocks of the specified type in the region.
     */
    public static int countBlockInArea(ServerLevel level, BlockPos fromPos, BlockPos toPos, Block blockToCount) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(fromPos, toPos)) {
            if (level.getBlockState(pos).is(blockToCount)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Destroys all blocks in an area, effectively clearing the region.
     *
     * @param level The level where the blocks are located.
     * @param fromPos The starting position of the area.
     * @param toPos The ending position of the area.
     * @param dropItems If true, blocks will drop items when destroyed.
     */
    public static void destroyBlocksInArea(ServerLevel level, BlockPos fromPos, BlockPos toPos, boolean dropItems) {
        for (BlockPos pos : BlockPos.betweenClosed(fromPos, toPos)) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                if (dropItems) {
                    Block.dropResources(state, level, pos);
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Finds the first non-air block from a specified position going downward.
     *
     * @param level The level where the blocks are located.
     * @param pos The position to start searching from.
     * @return The position of the first non-air block below the starting position, or null if no block is found.
     */
    public static BlockPos findFirstSolidBlockBelow(ServerLevel level, BlockPos pos) {
        BlockPos currentPos = pos.below();
        while (currentPos.getY() > level.getMinBuildHeight()) {
            if (!level.getBlockState(currentPos).isAir()) {
                return currentPos;
            }
            currentPos = currentPos.below();
        }
        return null;
    }

    /**
     * Counts how many unique types of blocks exist within a region and returns a Set of those blocks.
     *
     * @param level The level where the blocks are located.
     * @param fromPos The starting position of the area.
     * @param toPos The ending position of the area.
     * @return A set of unique Block types within the area.
     */
    public static Set<Block> getBlocksInArea(ServerLevel level, BlockPos fromPos, BlockPos toPos) {
        return BlockPos.betweenClosedStream(fromPos, toPos)
                .map(pos -> level.getBlockState(pos).getBlock())
                .collect(Collectors.toSet());
    }

    /**
     * Simulates the vanilla placement mechanics, checking if the block can be placed in the given location.
     *
     * @param level The level where the block will be placed.
     * @param pos The position to place the block.
     * @param block The block to place.
     * @param context The context of block placement (such as the player's interaction).
     * @return True if the block was placed successfully, false otherwise.
     */
    public static boolean placeBlockWithValidation(ServerLevel level, BlockPos pos, Block block, BlockPlaceContext context) {
        BlockState blockState = block.defaultBlockState();
        if (blockState.canSurvive(level, pos)) {
            level.setBlock(pos, blockState, 3);
            return true;
        }
        return false;
    }

    /**
     * Replaces the most frequent block in an area with another block using Streams.
     * First finds the most frequent block, then replaces it with the specified block.
     *
     * @param level The level where the blocks are located.
     * @param fromPos The starting position of the area.
     * @param toPos The ending position of the area.
     * @param replacementBlock The block to replace the most frequent block with.
     */
    public static void replaceMostFrequentBlock(ServerLevel level, BlockPos fromPos, BlockPos toPos, Block replacementBlock) {
        Map<Block, Long> blockFrequency = BlockPos.betweenClosedStream(fromPos, toPos)
                .map(pos -> level.getBlockState(pos).getBlock())
                .collect(Collectors.groupingBy(block -> block, Collectors.counting()));

        blockFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).ifPresent(mostFrequentBlock -> BlockPos.betweenClosedStream(fromPos, toPos)
                        .filter(pos -> level.getBlockState(pos).getBlock() == mostFrequentBlock)
                        .forEach(pos -> level.setBlockAndUpdate(pos, replacementBlock.defaultBlockState())));

    }

    /**
     * Returns a Stream of blocks within a region that meet a specific condition.
     *
     * @param level The level where the blocks are located.
     * @param fromPos The starting position of the area.
     * @param toPos The ending position of the area.
     * @param condition A predicate that defines the condition the block must meet.
     * @return A Stream of BlockPos representing the blocks that meet the condition.
     */
    public static Stream<BlockPos> getBlocksWithCondition(ServerLevel level, BlockPos fromPos, BlockPos toPos, Predicate<BlockState> condition) {
        return BlockPos.betweenClosedStream(fromPos, toPos).filter(pos -> condition.test(level.getBlockState(pos)));
    }

    /**
     * Replaces all blocks in an area with another block if the blockstate meets a specific condition.
     *
     * @param level The level where the blocks are located.
     * @param fromPos The starting position of the area.
     * @param toPos The ending position of the area.
     * @param condition A lambda expression that defines the condition to replace a block.
     * @param replacementBlock The block to replace with.
     */
    public static void replaceBlocksWithCondition(ServerLevel level, BlockPos fromPos, BlockPos toPos, Predicate<BlockState> condition, Block replacementBlock) {
        BlockPos.betweenClosedStream(fromPos, toPos)
                .filter(pos -> condition.test(level.getBlockState(pos)))
                .forEach(pos -> level.setBlockAndUpdate(pos, replacementBlock.defaultBlockState()));
    }

}