package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.server.EntityPlaceBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemPlaceMixin {

    @Inject(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z", shift = At.Shift.AFTER))
    private void knightlib$onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        final Level level = context.getLevel();
        if (level.isClientSide) {
            return;
        }

        final BlockPos clickedPos = context.getClickedPos();
        final BlockState placed = level.getBlockState(clickedPos);
        final Player player = context.getPlayer();

        final EntityPlaceBlockEvent event = new EntityPlaceBlockEvent(level, clickedPos, placed, player);
        KnightLibEvents.SERVER.dispatch(event);

        if (event.isCancelled()) {
            level.removeBlock(clickedPos, false);
        }
    }

}