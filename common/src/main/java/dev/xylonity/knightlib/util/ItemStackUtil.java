package dev.xylonity.knightlib.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling various ItemStack-related tasks.
 *
 * @author Xylonity
 */
public class ItemStackUtil {

    /**
     * Checks if the player has at least a certain number of a specific item in their inventory.
     *
     * @param player The player to check.
     * @param item The item to check for.
     * @param count The number of items required.
     * @return True if the player has at least the specified number of the item, false otherwise.
     */
    public static boolean hasItemAmount(Player player, Item item, int count) {
        int totalCount = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                totalCount += stack.getCount();
                if (totalCount >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes a specific number of items from the player's inventory.
     *
     * @param player The player from whom the items will be removed.
     * @param item The item to remove.
     * @param count The number of items to remove.
     * @return True if the specified amount was successfully removed, false otherwise.
     */
    public static boolean removeItemFromPlayer(Player player, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int stackCount = stack.getCount();
                if (stackCount > remaining) {
                    stack.shrink(remaining);
                    return true;
                } else {
                    remaining -= stackCount;
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
        return remaining <= 0;
    }

    /**
     * Clears all enchantments from an ItemStack.
     *
     * @param stack The ItemStack to clear enchantments from.
     */
    public static void clearEnchantments(ItemStack stack) {
        if (stack.isEnchanted()) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove("Enchantments");
            }
        }
    }

    /**
     * Sets a custom name for an ItemStack.
     *
     * @param stack The ItemStack to rename.
     * @param name The custom name to set.
     * @return The renamed ItemStack.
     */
    public static ItemStack setCustomName(ItemStack stack, String name) {
        stack.setHoverName(Component.literal(name));
        return stack;
    }

    /**
     * Checks if an ItemStack contains a specific enchantment.
     *
     * @param stack The ItemStack to check.
     * @param enchantment The enchantment to look for.
     * @return True if the stack contains the enchantment, false otherwise.
     */
    public static boolean hasEnchantment(ItemStack stack, Enchantment enchantment) {
        return EnchantmentHelper.getEnchantments(stack).containsKey(enchantment);
    }

    /**
     * Combines enchantments from two ItemStacks of the same item.
     *
     * @param stack1 The first ItemStack.
     * @param stack2 The second ItemStack.
     * @return The ItemStack with combined enchantments.
     */
    public static ItemStack combineEnchantments(ItemStack stack1, ItemStack stack2) {
        if (stack1.getItem() != stack2.getItem()) {
            return stack1;
        }

        var enchantment1 = EnchantmentHelper.getEnchantments(stack1);
        var enchantment2 = EnchantmentHelper.getEnchantments(stack2);

        enchantment2.forEach((enchantment, level) -> {
            enchantment1.merge(enchantment, level, Math::max);
        });

        EnchantmentHelper.setEnchantments(enchantment1, stack1);
        return stack1;
    }

    /**
     * Splits a stack into multiple smaller stacks of a specified size.
     *
     * @param stack The ItemStack to split.
     * @param size The size of each split stack.
     * @return A list of ItemStacks resulting from the split.
     */
    public static List<ItemStack> splitStack(ItemStack stack, int size) {
        List<ItemStack> result = new ArrayList<>();
        int remaining = stack.getCount();

        while (remaining > 0) {
            int splitSize = Math.min(size, remaining);
            ItemStack splitStack = stack.copy();
            splitStack.setCount(splitSize);
            result.add(splitStack);
            remaining -= splitSize;
        }

        return result;
    }

    /**
     * Repairs an ItemStack by a specified amount.
     *
     * @param stack The ItemStack to repair.
     * @param repairAmount The amount of durability to restore.
     * @return True if the item was successfully repaired, false if the item was already fully repaired.
     */
    public static boolean repairItem(ItemStack stack, int repairAmount) {
        if (!stack.isDamageableItem() || stack.getDamageValue() == 0) {
            return false;
        }

        int newDamage = stack.getDamageValue() - repairAmount;
        stack.setDamageValue(Math.max(newDamage, 0));
        return true;
    }

    /**
     * Completely repairs an ItemStack, restoring it to full durability.
     *
     * @param stack The ItemStack to fully repair.
     * @return True if the item was successfully repaired, false if the item is already fully repaired.
     */
    public static boolean fullyRepairItem(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getDamageValue() == 0) {
            return false;
        }
        stack.setDamageValue(0);
        return true;
    }

    /**
     * Checks if an ItemStack can be stacked with another ItemStack.
     * The stacks are combinable if they are of the same item, have the same damage value, and are not enchanted.
     *
     * @param stack1 The first ItemStack.
     * @param stack2 The second ItemStack.
     * @return True if the stacks can be combined, false otherwise.
     */
    public static boolean canStacksBeCombined(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) {
            return false;
        }
        return stack1.getItem() == stack2.getItem() &&
                stack1.getDamageValue() == stack2.getDamageValue() &&
                !stack1.isEnchanted() &&
                !stack2.isEnchanted();
    }

    /**
     * Retrieves the total durability of an ItemStack (current durability + damage).
     *
     * @param stack The ItemStack to check.
     * @return The total durability of the item.
     */
    public static int getTotalDurability(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 0;
        }
        return stack.getMaxDamage();
    }

    /**
     * Retrieves the remaining durability of an ItemStack.
     *
     * @param stack The ItemStack to check.
     * @return The remaining durability of the item.
     */
    public static int getRemainingDurability(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 0;
        }

        return stack.getMaxDamage() - stack.getDamageValue();
    }

    /**
     * Adds custom NBT data to an ItemStack.
     *
     * @param stack The ItemStack to modify.
     * @param key The NBT key to add.
     * @param value The value to assign to the NBT key.
     */
    public static void addCustomNBT(ItemStack stack, String key, String value) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(key, value);
    }

    /**
     * Retrieves a custom NBT data from an ItemStack.
     *
     * @param stack The ItemStack to check.
     * @param key The NBT key to retrieve.
     * @return The value of the NBT key, or null if not found.
     */
    public static String getCustomNBT(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key) ? tag.getString(key) : null;
    }

}