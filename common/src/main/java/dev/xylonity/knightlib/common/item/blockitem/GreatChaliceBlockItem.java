package dev.xylonity.knightlib.common.item.blockitem;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GreatChaliceBlockItem extends GenericRenderedBlockItem {

    public GreatChaliceBlockItem(Block block, Properties properties, ResourceLocation rendererId) {
        super(block, properties, rendererId);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemStack, level, lines, flag);
        lines.add(Component.translatable("tooltip.item.knightlib.great_chalice"));
    }

}