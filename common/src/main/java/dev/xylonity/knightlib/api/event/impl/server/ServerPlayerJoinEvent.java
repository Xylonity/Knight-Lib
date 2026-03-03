package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired when a player logs in to the server.
 * This fires once on connection, not per-level
 */
public final class ServerPlayerJoinEvent extends KnightLibEvent {

    private final MinecraftServer server;
    private final ServerPlayer player;

    public ServerPlayerJoinEvent(MinecraftServer server, ServerPlayer player) {
        this.server = server;
        this.player = player;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

}