package dev.xylonity.knightlib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * This class defines the functionality of a block capable of handling synchronized ticking without relying
 * on a BlockEntity. There are various ways to achieve this process, and this implementation provides a
 * functional solution.
 *
 * @author Xylonity
 */
public abstract class AbstractTickBlock extends Block implements ITickBlockTracker {

    public AbstractTickBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(@NotNull BlockState blockState, @NotNull ServerLevel level, @NotNull BlockPos blockPos, @NotNull RandomSource randomSource) {

        // Override this method and implement your custom logic.
        // DO NOT call the super method. Instead, ensure you call this.scheduleTick(level, blockPos).

        this.scheduleTick(level, blockPos);
    }

    @Override
    public void onPlace(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState newState, boolean b) {
        if (!level.isClientSide) {
            removeTickBlock(blockPos);
            scheduleTick(level, blockPos);
        }
    }

    @Override
    public void onRemove(@NotNull BlockState blockState, Level level, @NotNull BlockPos blockPos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide) {
            removeTickBlock(blockPos);
        }

        super.onRemove(blockState, level, blockPos, newState, movedByPiston);
    }

    public void scheduleTick(Level pLevel, BlockPos pPos) {
        if (pLevel instanceof ServerLevel serverLevel) {
            serverLevel.scheduleTick(pPos, this, 1);
        }
    }

}
