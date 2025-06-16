package dev.xylonity.knightlib.common.item;

import dev.xylonity.knightlib.common.api.IHomunculusInteractable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HomunculusItem extends Item {

    public HomunculusItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (level.getBlockState(pos).getBlock() instanceof IHomunculusInteractable actor) {
            if (!actor.canInteract(context)) {
                return InteractionResult.FAIL;
            }

            actor.onPreInteraction(context);

            if (!level.isClientSide) stack.shrink(actor.shrinkItemAmount());

            if (!actor.getInteractionSounds().isEmpty()) {
                for (SoundEvent sound : actor.getInteractionSounds()) {
                    level.playSound(null, pos, sound, SoundSource.BLOCKS, 1f, 1f);
                }
            }

            actor.onPostInteraction(context);

            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level level, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        list.add(Component.translatable("tooltip.item.knightlib.homunculus"));
    }

}
