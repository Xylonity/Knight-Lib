package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired after a player has respawned (the new player instance is already in the level)
 */
public final class ServerPlayerRespawnEvent extends KnightLibEvent {

    private final ServerPlayer player;
    private final boolean endConquered;

    public ServerPlayerRespawnEvent(ServerPlayer player, boolean endConquered) {
        this.player = player;
        this.endConquered = endConquered;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * True when the respawn was triggered by leaving the End alive rather than by death
     */
    public boolean isEndConquered() {
        return endConquered;
    }

}
