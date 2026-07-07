package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired when a new player instance is created from an old one (death respawn or returning
 * from the End)
 */
public final class ServerPlayerCloneEvent extends KnightLibEvent {

    private final ServerPlayer oldPlayer;
    private final ServerPlayer newPlayer;
    private final boolean wasDeath;

    public ServerPlayerCloneEvent(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wasDeath) {
        this.oldPlayer = oldPlayer;
        this.newPlayer = newPlayer;
        this.wasDeath = wasDeath;
    }

    public ServerPlayer getOldPlayer() {
        return oldPlayer;
    }

    public ServerPlayer getNewPlayer() {
        return newPlayer;
    }

    public boolean wasDeath() {
        return wasDeath;
    }

}
