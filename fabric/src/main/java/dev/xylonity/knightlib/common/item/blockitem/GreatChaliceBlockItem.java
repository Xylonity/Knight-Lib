package dev.xylonity.knightlib.common.item.blockitem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GreatChaliceBlockItem extends GenericBlockItem {

    public GreatChaliceBlockItem(Block pBlock, Properties pProperties, String name) {
        super(pBlock, pProperties, name);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level level, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemStack, level, list, flag);
        list.add(Component.translatable("tooltip.item.knightlib.great_chalice"));
    }

}
