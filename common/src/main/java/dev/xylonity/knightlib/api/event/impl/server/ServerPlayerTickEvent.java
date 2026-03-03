package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired at the start/end of each server-side player tick
 */
public final class ServerPlayerTickEvent extends KnightLibEvent {

    private final MinecraftServer server;
    private final ServerPlayer player;
    private final TickPhase phase;

    public ServerPlayerTickEvent(MinecraftServer server, ServerPlayer player, TickPhase phase) {
        this.server = server;
        this.player = player;
        this.phase = phase;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public TickPhase getPhase() {
        return phase;
    }

}