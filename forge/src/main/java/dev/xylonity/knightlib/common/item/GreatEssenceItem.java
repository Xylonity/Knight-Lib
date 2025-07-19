package dev.xylonity.knightlib.common.item;

import dev.xylonity.knightlib.api.IGreatChaliceInteractable;
import dev.xylonity.knightlib.api.impl.GreatChaliceState;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GreatEssenceItem extends Item implements IGreatChaliceInteractable {

    public GreatEssenceItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getChargesToApply() {
        return 4;
    }

    @Override
    public boolean canInteract(GreatChaliceBlockEntity chalice, Level level, Player player) {
        return chalice.getState() == GreatChaliceState.NORMAL || chalice.getState() == GreatChaliceState.EMPTY;
    }

    @Override
    public void onPostInteraction(GreatChaliceBlockEntity chalice, Player player, Level level, BlockHitResult hit) {
        IGreatChaliceInteractable.super.onPostInteraction(chalice, player, level, hit);
        if (chalice.getState() == GreatChaliceState.EMPTY) chalice.setState(GreatChaliceState.NORMAL);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @NotNull TooltipContext pContext, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        pTooltipComponents.add(Component.translatable("tooltip.item.knightlib.great_essence"));
    }

}
