package dev.xylonity.knightlib.common.api;

import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Defines how an item should interact with the Great Chalice.
 */
public interface IGreatChaliceInteractable {

    // Great chalice maximum charges
    int MAX_CHARGES = 12;

    /**
     * Returns the number of charges to add/remove to the chalice.
     *
     * @return the charge delta to apply to the chalice
     */
    int getChargesToApply();

    /**
     * Determines how many items should be consumed from this stack during the interaction.
     *
     * @return the number of items to remove
     */
    default int shrinkItemAmount() {
        return 1;
    }

    /**
     * Determines if this item can interact with the chalice in its current state.
     * Charge amount and the chalice state must be checked here.
     *
     * @param chalice the chalice from the interaction
     * @param level the level where the interaction is done
     * @param player the player performing the interaction
     * @return true if the interaction should proceed, false if not
     */
    boolean canInteract(GreatChaliceBlockEntity chalice, Level level, Player player);

    /**
     * Called immediately before the main interaction logic is processed.
     *
     * @param chalice the chalice from the interaction
     * @param player the player performing the action
     * @param level the level where the interaction is done
     * @param hit the blockhit info
     */
    default void onPreInteraction(GreatChaliceBlockEntity chalice, Player player, Level level, BlockHitResult hit) {
        ;;
    }

    /**
     * Called after the main interaction logic has been successfully processed.
     * This won't be triggered if the interaction isn't successful
     *
     * @param chalice the chalice from the interaction
     * @param player the player performing the action
     * @param level the level where the interaction is done
     * @param hit the blockhit info
     */
    default void onPostInteraction(GreatChaliceBlockEntity chalice, Player player, Level level, BlockHitResult hit) {
        ;;
    }

    /**
     * Provides a set of item stacks to spawn as rewards when the interaction completes.
     * Items will be spawned over the chalice for the player to collect.
     *
     * @return the set of items to spawn
     */
    default @NotNull Set<ItemStack> getRewards() {
        return Set.of();
    }

    /**
     * Provides a set of sound effects to play when the interaction occurs.
     * All sounds will be played at the chalice's location.
     *
     * @return a set of sounds to play
     */
    default @NotNull Set<SoundEvent> getInteractionSounds() {
        return Set.of(SoundEvents.BUCKET_EMPTY);
    }

}
