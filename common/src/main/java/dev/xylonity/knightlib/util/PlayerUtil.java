package dev.xylonity.knightlib.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility class for handling various player-related tasks, including
 * inventory sorting, transferring items between players, and more.
 *
 * @author Xylonity
 */
public class PlayerUtil {

    /**
     * Heals the player by the specified amount, ensuring the player's health does not exceed the maximum.
     *
     * @param player The player to heal.
     * @param amount The amount of health to restore.
     */
    public static void healPlayer(Player player, float amount) {
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
    }

    /**
     * Teleports the player to the specified coordinates.
     *
     * @param player The player to teleport.
     * @param pos The target position as BlockPos.
     */
    public static void teleportPlayer(Player player, BlockPos pos) {
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    /**
     * Gives a specific amount of the specified item to the player, handling inventory space automatically.
     * This method executes independent of the player's gamemode (if you run a generic `add` call on a player
     * that's in creative gamemode and has full inventory, the item will disappear. This method prevents that).
     *
     * @param player The player to give the item to.
     * @param itemStack The ItemStack to give to the player.
     * @return True if the items were successfully given, false if there was no space.
     */
    public static boolean giveItemToPlayer(Player player, ItemStack itemStack) {
        int freeSpace = calculateFreeSpaceForItem(player, itemStack);

        if (freeSpace >= itemStack.getCount()) {
            player.getInventory().add(itemStack);
            return true;
        }

        if (freeSpace > 0) {
            ItemStack stackToAdd = itemStack.copy();
            stackToAdd.setCount(freeSpace);
            player.getInventory().add(stackToAdd);
        }

        ItemStack remainingStack = itemStack.copy();
        remainingStack.setCount(itemStack.getCount() - freeSpace);
        dropItemNearPlayer(player, remainingStack);

        return false;
    }

    /**
     * Calculates how much space is available in the player's inventory for a specific item.
     * This checks all slots to see if there is space to add more of the given item.
     *
     * @param player The player whose inventory will be checked.
     * @param itemStack The ItemStack to check for available space.
     * @return The total amount of the item that can fit in the inventory.
     */
    private static int calculateFreeSpaceForItem(Player player, ItemStack itemStack) {
        int freeSpace = 0;
        int maxStackSize = itemStack.getMaxStackSize();

        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                freeSpace += maxStackSize;
            } else if (stack == itemStack) {
                freeSpace += maxStackSize - stack.getCount();
            }

            if (freeSpace >= itemStack.getCount()) {
                return itemStack.getCount();
            }
        }

        return freeSpace;
    }

    /**
     * Drops an itemstack near the player in the world. This method generates an ItemEntity to
     * handle pickup delay.
     *
     * @param player The player near whom the item will be dropped.
     * @param itemStack The item stack to drop.
     */
    private static void dropItemNearPlayer(Player player, ItemStack itemStack) {
        Level level = player.level();
        if (!level.isClientSide && !itemStack.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(
                    level,
                    player.getX(),
                    player.getY() + 0.5,
                    player.getZ(),
                    itemStack
            );

            itemEntity.setPickUpDelay(40);

            level.addFreshEntity(itemEntity);
        }
    }

    /**
     * Transfers a specific item from one player to another, handling inventory space and dropping excess items.
     *
     * @param fromPlayer The player from whom the item is transferred.
     * @param toPlayer The player receiving the item.
     * @param itemStack The ItemStack to transfer.
     * @param amount The amount to transfer.
     * @return True if the transfer was successful, false otherwise.
     */
    public static boolean transferItemBetweenPlayers(Player fromPlayer, Player toPlayer, ItemStack itemStack, int amount) {
        ItemStack stackToTransfer = itemStack.copy();
        stackToTransfer.setCount(amount);

        if (fromPlayer.getInventory().contains(stackToTransfer)) {
            fromPlayer.getInventory().removeItem(stackToTransfer);

            boolean addedToReceiver = toPlayer.getInventory().add(stackToTransfer);

            if (!addedToReceiver) {
                dropItemNearPlayer(toPlayer, stackToTransfer);
            }
            return true;
        }
        return false;
    }

    /**
     * Gives an item to the player if certain conditions are met.
     * Example condition: player must be above a certain Y-level and have a minimum experience level.
     *
     * @param player The player to give the item to.
     * @param requiredYLevel The minimum Y-level the player must be at to receive the item.
     * @param requiredExperience The minimum experience level the player must have.
     * @param itemStack The rare item to give if the conditions are met.
     * @return True if the conditions were met and the item was given, false otherwise.
     */
    public static boolean giveItemIfConditionsMet(Player player, int requiredYLevel, int requiredExperience, ItemStack itemStack) {
        if (player.getY() > requiredYLevel && player.experienceLevel >= requiredExperience) {
            return giveItemToPlayer(player, itemStack);
        }
        return false;
    }

    /**
     * Checks if the player has at least the specified number of free inventory slots.
     *
     * @param player The player whose inventory will be checked.
     * @param requiredSlots The number of free slots required.
     * @return True if the player has the required number of free slots, false otherwise.
     */
    public static boolean hasFreeInventorySlots(Player player, int requiredSlots) {
        long freeSlots = player.getInventory().items.stream().filter(ItemStack::isEmpty).count();
        return freeSlots >= requiredSlots;
    }

    /**
     * Sets the player's hunger to the specified amount.
     *
     * @param player The player whose hunger will be set.
     * @param amount The amount of hunger to set (between 0 and 20).
     */
    public static void setPlayerHunger(Player player, int amount) {
        player.getFoodData().setFoodLevel(Math.min(20, Math.max(0, amount)));
    }

    /**
     * Checks if the player is above a specific Y level.
     * Useful for detecting if the player is flying or above certain altitudes.
     *
     * @param player The player to check.
     * @param yLevel The Y level to compare against.
     * @return True if the player is above the specified Y level.
     */
    public static boolean isPlayerAboveYLevel(Player player, double yLevel) {
        return player.getY() > yLevel;
    }

    /**
     * Kills the player instantly regardless of health.
     *
     * @param player The player to kill.
     */
    public static void killPlayer(Player player) {
        player.setHealth(0.0F);
    }

    /**
     * Sends the player flying in a specified direction with a given velocity.
     *
     * @param player The player to launch.
     * @param direction The direction as a vector to launch the player towards.
     * @param velocity The velocity with which to launch the player.
     */
    public static void launchPlayer(Player player, Vec3 direction, double velocity) {
        Vec3 normalizedDirection = direction.normalize();
        player.setDeltaMovement(normalizedDirection.scale(velocity));
        player.hasImpulse = true;
    }

    /**
     * Freezes the player in place.
     *
     * @param player The player to freeze.
     * @param freezeTicks The number of ticks to freeze the player for.
     */
    public static void freezePlayer(Player player, int freezeTicks) {
        player.setTicksFrozen(freezeTicks);
    }

    /**
     * Checks if a player is in the water.
     *
     * @param player The player to check.
     * @return True if the player is in water.
     */
    public static boolean isPlayerInWater(Player player) {
        return player.isInWater();
    }

    /**
     * Grants a specific potion effect to the player for a certain duration.
     *
     * @param player The player to apply the effect to.
     * @param effect The MobEffect (like MobEffects.DAMAGE_BOOST) to apply.
     * @param duration The duration of the effect in ticks (20 ticks = 1 second).
     * @param amplifier The level of the effect (0 for level I, 1 for level II, etc.).
     */
    public static void applyPotionEffect(Player player, Holder<MobEffect> effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    /**
     * Pushes the player away from a specific entity.
     *
     * @param player The player to push.
     * @param entity The entity from which the player will be pushed away.
     * @param force The force with which to push the player.
     */
    public static void pushPlayerAwayFromEntity(Player player, Entity entity, double force) {
        Vec3 direction = player.position().subtract(entity.position()).normalize();
        player.setDeltaMovement(direction.scale(force));
        player.hasImpulse = true;
    }

    /**
     * Makes the player glow with a glowing effect.
     *
     * @param player The player to apply the glowing effect to.
     * @param duration The duration of the glowing effect in ticks (20 ticks = 1 second).
     */
    public static void setPlayerGlowing(Player player, int duration) {
        applyPotionEffect(player, MobEffects.GLOWING, duration, 0);
    }

    /**
     * Returns true if the player is within a certain distance from a specific location.
     *
     * @param player The player to check.
     * @param pos The position to check against.
     * @param maxDistance The maximum distance allowed.
     * @return True if the player is within the distance, false otherwise.
     */
    public static boolean isPlayerWithinDistance(Player player, BlockPos pos, double maxDistance) {
        return player.blockPosition().closerThan(pos, maxDistance);
    }

    /**
     * Gives the player an item stack only if there is enough space in the inventory.
     *
     * @param player The player to give the item to.
     * @param stack The ItemStack to give.
     * @return True if the item was successfully given, false if there was no space.
     */
    public static boolean tryGiveItem(Player player, ItemStack stack) {
        if (player.getInventory().getFreeSlot() != -1) {
            player.getInventory().add(stack);
            return true;
        }
        return false;
    }

    /**
     * Teleports the player to the world spawn point.
     *
     * @param player The player to teleport.
     */
    private static void teleportToWorldSpawn(Player player) {
        BlockPos worldSpawn = player.level().getSharedSpawnPos();
        player.teleportTo(worldSpawn.getX(), worldSpawn.getY(), worldSpawn.getZ());
    }

    /**
     * Finds the closest entity to the player that satisfies a given condition.
     *
     * @param player The player to check from.
     * @param maxDistance The maximum distance within which to search.
     * @param condition The condition that the entity must satisfy.
     * @return The closest entity that satisfies the condition, or null if none found.
     */
    public static Entity findClosestEntity(Player player, double maxDistance, Predicate<Entity> condition) {
        return player.level().getEntities(player, player.getBoundingBox().inflate(maxDistance), condition)
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceTo(player)))
                .orElse(null);
    }

    /**
     * Gets a player's inventory as a formatted string, listing item names and quantities.
     *
     * @param player The player whose inventory will be retrieved.
     * @return A string representation of the player's inventory.
     */
    public static String getFormattedInventoryString(Player player) {
        return player.getInventory().items.stream()
                .filter(itemStack -> !itemStack.isEmpty())
                .map(itemStack -> itemStack.getItem().getDescriptionId() + " x" + itemStack.getCount())
                .collect(Collectors.joining(", "));
    }

    /**
     * Checks if the player's current item matches any item from a provided list of items.
     * Uses functional programming to apply the condition.
     *
     * @param player The player to check.
     * @param itemList The list of items to check against.
     * @return True if the player's current item matches any item from the list.
     */
    public static boolean isPlayerHoldingAnyItemFromList(Player player, List<ItemStack> itemList) {
        return itemList.stream().anyMatch(item -> player.getMainHandItem().getItem().equals(item.getItem()));
    }

    /**
     * Finds all nearby entities of a specific type within a radius and returns them as a list.
     *
     * @param player The player to search around.
     * @param entityType The class of entity to search for.
     * @param radius The radius around the player to search within.
     * @return A list of nearby entities of the specified type.
     */
    public static <T extends Entity> List<T> findEntitiesAroundPlayer(Player player, Class<T> entityType, double radius) {
        return player.level().getEntitiesOfClass(entityType, player.getBoundingBox().inflate(radius));
    }

    /**
     * Teleports the player to a random location within a specified range.
     *
     * @param player The player to teleport.
     * @param range The range within which to randomly teleport.
     */
    public static void randomTeleport(Player player, double range) {
        double randomX = player.getX() + (Math.random() - 0.5) * range * 2;
        double randomZ = player.getZ() + (Math.random() - 0.5) * range * 2;
        double y = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) randomX, (int) randomZ);
        player.teleportTo(randomX, y, randomZ);
    }

    /**
     * Heals the player to full health and fully restores hunger.
     *
     * @param player The player to heal and feed.
     */
    public static void fullyHealAndFeedPlayer(Player player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
    }

    /**
     * Finds the nearest block of a specified type to the player within a given radius.
     *
     * @param player The player from whose position to start the search.
     * @param blockTypes A list of block types to search for.
     * @param radius The search radius.
     * @return The position of the nearest block found, or null if none is found.
     */
    public static BlockPos findNearestBlockOfType(Player player, List<Block> blockTypes, double radius) {
        BlockPos playerPos = player.blockPosition();
        BlockPos nearestBlock = null;
        double closestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset((int) -radius, (int) -radius, (int) -radius),
                playerPos.offset((int) radius, (int) radius, (int) radius))) {
            if (blockTypes.contains(player.level().getBlockState(pos).getBlock())) {
                double distance = playerPos.distSqr(pos);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    nearestBlock = pos;
                }
            }
        }
        return nearestBlock;
    }

    /**
     * Calculates the total value of all items in the player's inventory based on custom item values.
     *
     * @param player The player whose inventory will be evaluated.
     * @param itemValues A map containing custom values for items.
     * @return The total value of the player's inventory.
     */
    public static int calculateInventoryValue(Player player, Map<Item, Integer> itemValues) {
        return player.getInventory().items.stream()
                .filter(itemStack -> !itemStack.isEmpty() && itemValues.containsKey(itemStack.getItem()))
                .mapToInt(itemStack -> itemValues.get(itemStack.getItem()) * itemStack.getCount())
                .sum();
    }

    /**
     * Executes a runnable when a player reaches a specific location.
     *
     * @param player The player being tracked.
     * @param targetPos The position to check.
     * @param radius The radius within which the player must be to trigger the command.
     * @param runnable The command to execute when the condition is met.
     */
    public static void executeRunnableAtLocation(Player player, BlockPos targetPos, double radius, Runnable runnable) {
        if (player.blockPosition().closerThan(targetPos, radius)) {
            runnable.run();
        }
    }

    /**
     * Logs the player's current position and inventory state to the console for debugging.
     *
     * @param player The player whose state will be logged.
     */
    public static void logPlayerState(Player player) {
        System.out.printf("Player Position: [x: %.2f, y: %.2f, z: %.2f]%n", player.getX(), player.getY(), player.getZ());
        System.out.println("Inventory: " + getFormattedInventoryString(player));
    }

    /**
     * Sorts the player's main inventory based on the item name in alphabetical order.
     * Preserves armor and offhand items and ensures no items are lost if the inventory is full.
     *
     * @param player The player whose inventory will be sorted.
     */
    public static void sortInventoryByItemName(Player player) {
        List<ItemStack> sortedItems = player.getInventory().items.stream()
                .filter(itemStack -> !itemStack.isEmpty())
                .sorted(Comparator.comparing(itemStack -> itemStack.getItem().getDescriptionId()))
                .toList();

        ItemStack[] armorBackup = new ItemStack[4];
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                armorBackup[slot.getIndex()] = player.getInventory().armor.get(slot.getIndex());
            }
        }
        ItemStack offhandBackup = player.getInventory().offhand.get(0);

        player.getInventory().clearContent();

        for (int i = 0; i < sortedItems.size() && i < player.getInventory().items.size(); i++) {
            player.getInventory().setItem(i, sortedItems.get(i));
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                player.getInventory().armor.set(slot.getIndex(), armorBackup[slot.getIndex()]);
            }
        }
        player.getInventory().offhand.set(0, offhandBackup);
    }

}