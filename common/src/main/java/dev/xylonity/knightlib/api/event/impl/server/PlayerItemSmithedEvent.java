package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fired when a player takes the result item from a smithing table
 */
public class PlayerItemSmithedEvent extends KnightLibEvent {

    private final Player player;
    private final ItemStack result;
    private final Container inputSlots;

    public PlayerItemSmithedEvent(Player player, ItemStack result, Container inputSlots) {
        this.player = player;
        this.result = result;
        this.inputSlots = inputSlots;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getResult() {
        return result;
    }

    public Container getInputSlots() {
        return inputSlots;
    }

}