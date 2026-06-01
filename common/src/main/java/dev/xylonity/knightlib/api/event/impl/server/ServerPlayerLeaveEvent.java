package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired when a player disconnects from the server
 */
public final class ServerPlayerLeaveEvent extends KnightLibEvent {

    private final MinecraftServer server;
    private final ServerPlayer player;

    public ServerPlayerLeaveEvent(MinecraftServer server, ServerPlayer player) {
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