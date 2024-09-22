package dev.xylonity.knightlib.records;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public record PlayerProfileData(
        UUID playerId,
        String playerName,
        int level,
        double experience,
        double health,
        double maxHealth,
        double armor,
        List<ItemStack> inventory,
        BlockPos lastKnownPosition,
        List<String> achievements
) {

    /**
     * Calculates the percentage of experience required to reach the next level.
     *
     * @return The experience percentage.
     */
    public double getExperienceProgress() {
        int expForCurrentLevel = getXpForLevel(level);
        int expForNextLevel = getXpForLevel(level + 1);
        return (experience - expForCurrentLevel) / (expForNextLevel - expForCurrentLevel);
    }

    /**
     * Checks if the player has reached a specific achievement.
     *
     * @param achievement The name of the achievement to check.
     * @return True if the player has the achievement, false otherwise.
     */
    public boolean hasAchievement(String achievement) {
        return achievements.contains(achievement);
    }

    /**
     * Adds a new achievement to the player profile.
     *
     * @param achievement The achievement to add.
     */
    public void addAchievement(String achievement) {
        if (!hasAchievement(achievement)) {
            achievements.add(achievement);
        }
    }

    /**
     * Retrieves the number of items in the player's inventory of a specific type.
     *
     * @param itemName The name of the item to search for.
     * @return The total count of the item in the player's inventory.
     */
    public int getItemCount(String itemName) {
        return inventory.stream()
                .filter(itemStack -> itemStack.getItem().asItem().toString().equals(itemName))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    /**
     * Calculates the player's total armor rating.
     *
     * @return The total armor value.
     */
    public double calculateArmorRating() {
        return armor + inventory.stream()
                .filter(itemStack -> itemStack.getItem() instanceof ArmorItem)
                .mapToDouble(itemStack -> ((ArmorItem) itemStack.getItem()).getDefense())
                .sum();
    }

    /**
     * Calculates the experience needed for the next level.
     *
     * @param level The current level.
     * @return The experience required for the next level.
     */
    private int getXpForLevel(int level) {
        if (level <= 15) {
            return level * level + 6 * level;
        } else if (level <= 30) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    @Override
    public String toString() {
        return playerName + " [Level: " + level + ", Health: " + health + "/" + maxHealth + "]";
    }
}