package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Fired when an item tooltip is being gathered, allowing modification of tooltip lines.
 * The tooltip list is mutable, so observers can add, remove, or reorder lines
 */
public final class ItemStackTooltipEvent extends KnightLibEvent {

    private final ItemStack itemStack;
    private final List<Component> tooltipLines;
    private final TooltipFlag flag;

    public ItemStackTooltipEvent(ItemStack itemStack, List<Component> tooltipLines, TooltipFlag flag) {
        this.itemStack = itemStack;
        this.tooltipLines = tooltipLines;
        this.flag = flag;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Mutable list of tooltip components.
     */
    public List<Component> getTooltipLines() {
        return tooltipLines;
    }

    public TooltipFlag getFlag() {
        return flag;
    }

}