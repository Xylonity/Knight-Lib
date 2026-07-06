package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Fired when a player starts tracking an entity (the entity enters the player's view distance)
 */
public final class PlayerStartTrackingEvent extends KnightLibEvent {

    private final ServerPlayer player;
    private final Entity target;

    public PlayerStartTrackingEvent(ServerPlayer player, Entity target) {
        this.player = player;
        this.target = target;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public Entity getTarget() {
        return target;
    }

}
