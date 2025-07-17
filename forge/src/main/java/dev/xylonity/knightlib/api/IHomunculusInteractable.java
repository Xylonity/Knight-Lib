package dev.xylonity.knightlib.api;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Defines how a block should interact with the Homunculus item.
 */
public interface IHomunculusInteractable {

    /**
     * Determines how many items should be consumed from the item stack during the interaction.
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
     * @param context the context of the interaction
     * @return true if the interaction should proceed, false if not
     */
    boolean canInteract(UseOnContext context);

    /**
     * Called immediately before the main interaction logic is processed.
     *
     * @param context the context of the interaction
     */
    default void onPreInteraction(UseOnContext context) {
        ;;
    }

    /**
     * Called after the main interaction logic has been successfully processed.
     * This won't be triggered if the interaction isn't successful
     *
     * @param context the context of the interaction
     */
    default void onPostInteraction(UseOnContext context) {
        ;;
    }

    /**
     * Provides a set of sound effects to play when the interaction occurs.
     * All sounds will be played at the block's location.
     *
     * @return a set of sounds to play
     */
    default @NotNull Set<SoundEvent> getInteractionSounds() {
        return Set.of();
    }

}
