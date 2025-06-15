package dev.xylonity.knightlib.common.block;

import dev.xylonity.knightlib.common.api.IGreatChaliceInteractable;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class GreatChaliceBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Stream.of(
            Block.box(0, 7, 0, 2, 16, 16),
            Block.box(14, 7, 0, 16, 16, 16),
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 5, 0, 16, 7, 16),
            Block.box(2, 7, 0, 14, 16, 2),
            Block.box(2, 7, 14, 14, 16, 16),
            Block.box(4, 2, 4, 12, 5, 12)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public GreatChaliceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return KnightLibBlockEntities.GREAT_CHALICE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level pLevel, @NotNull BlockState pState, @NotNull BlockEntityType<T> pBlockEntityType) {
        return pBlockEntityType == KnightLibBlockEntities.GREAT_CHALICE.get() ? GreatChaliceBlockEntity::tick : null;
    }

    @Override
    public InteractionResult use(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull InteractionHand pHand, @NotNull BlockHitResult pHit) {

        if (pLevel.isClientSide) return InteractionResult.PASS;

        if (!(pLevel.getBlockEntity(pPos) instanceof GreatChaliceBlockEntity chalice)) return InteractionResult.PASS;

        ItemStack stack = pPlayer.getItemInHand(pHand);
        if (stack.getItem() instanceof IGreatChaliceInteractable actor) {
            if (!actor.canInteract(chalice, pLevel, pPlayer)) return InteractionResult.PASS;

            actor.onPreInteraction(chalice, pPlayer, pLevel, pHit);

            int total = chalice.getCharges() + actor.getChargesToApply();
            if (total < 0 || total > IGreatChaliceInteractable.MAX_CHARGES) {
                return InteractionResult.PASS;
            }

            chalice.setCharges(total);
            stack.shrink(actor.shrinkItemAmount());

            if (!actor.getRewards().isEmpty()) {
                for (ItemStack e : actor.getRewards()) {
                    Entity entity = new ItemEntity(pLevel, pPos.getX() + 0.5, pPos.getY() + pPlayer.getBbHeight() * 0.5, pPos.getZ(), e);
                    pLevel.addFreshEntity(entity);
                }
            }

            if (!actor.getInteractionSounds().isEmpty()) {
                for (SoundEvent sound : actor.getInteractionSounds()) {
                    pLevel.playSound(null, pPos, sound, SoundSource.BLOCKS, 1f, 1f);
                }
            }

            actor.onPostInteraction(chalice, pPlayer, pLevel, pHit);

            return InteractionResult.SUCCESS;
        }

        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

}
