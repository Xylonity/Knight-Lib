package dev.xylonity.knightlib.common.api;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Defines how an item should interact with the Great Chalice.
 */
public interface IGreatChaliceInteractable {

    int MAX_CHARGES = 12;

    /**
     * Returns the number of charges to add/remove to the chalice.
     * @return the delta to apply
     */
    int getChargesToApply();

    /**
     * Gets the required chalice states before performing the interaction.
     * @return the chalice state to match prior to interaction
     */
    Set<GreatChaliceState> getRequiredState();

    default int shrinkItemAmount() {
        return 1;
    }

    default int getRequiredCharges() {
        return 0;
    }

    /**
     * Determines whether this interaction should swap the chalice
     * between two states (i.e. from CHAOTIC to RADIANT).
     * @return true if a state swap should occur, false otherwise
     */
    default boolean shouldSwapStates() {
        return false;
    }

    /**
     * Gets the target chalice state to set after the interaction.
     * Only relevant if {@link #shouldSwapStates()} returns true.
     * @return the chalice state to apply after interaction
     */
    default @NotNull GreatChaliceState getTargetState() {
        return GreatChaliceState.EMPTY;
    }

    default @NotNull Set<ItemStack> getRewards() {
        return Set.of();
    }

    default @Nullable SoundEvent getInteractionSound() {
        return SoundEvents.BREWING_STAND_BREW;
    }

}
