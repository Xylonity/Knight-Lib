package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Fired when a server world is loaded
 */
public final class ServerWorldLoadEvent extends KnightLibEvent {

    private final MinecraftServer server;
    private final ServerLevel level;

    public ServerWorldLoadEvent(MinecraftServer server, ServerLevel level) {
        this.server = server;
        this.level = level;
    }

    public MinecraftServer server() {
        return server;
    }

    public ServerLevel level() {
        return level;
    }

}
