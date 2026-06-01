package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fired when an entity successfully crafts an item
 */
public class PlayerItemCraftedEvent extends KnightLibEvent {

    private final Player player;
    private final ItemStack crafted;
    private final Container craftMatrix;

    public PlayerItemCraftedEvent(Player player, ItemStack crafted, Container craftMatrix) {
        this.player = player;
        this.crafted = crafted;
        this.craftMatrix = craftMatrix;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getCrafted() {
        return crafted;
    }

    public Container getCraftMatrix() {
        return craftMatrix;
    }

}