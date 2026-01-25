package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import net.minecraft.server.MinecraftServer;

/**
 * Fired at the start/end of each server tick (20 times per second)
 */
public final class ServerTickEvent extends KnightLibEvent {

    private final MinecraftServer server;
    private final TickPhase phase;

    public ServerTickEvent(MinecraftServer server, TickPhase phase) {
        this.server = server;
        this.phase = phase;
    }

    public MinecraftServer server() {
        return server;
    }

    public TickPhase phase() {
        return phase;
    }

}
