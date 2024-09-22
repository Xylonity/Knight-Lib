package dev.xylonity.knightlib.interfaces;

import net.minecraft.world.item.ItemStack;

public interface IContainerBlock {

    /**
     * Gets the size of the inventory for the container block.
     *
     * @return The number of slots in the inventory.
     */
    int getInventorySize();

    /**
     * Gets the ItemStack in a specific inventory slot.
     *
     * @param slot The slot index.
     * @return The ItemStack in the given slot.
     */
    ItemStack getItemInSlot(int slot);

    /**
     * Sets an ItemStack in a specific inventory slot.
     *
     * @param slot The slot index.
     * @param stack The ItemStack to place in the slot.
     */
    void setItemInSlot(int slot, ItemStack stack);

}
