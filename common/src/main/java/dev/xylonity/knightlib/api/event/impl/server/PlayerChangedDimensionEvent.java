package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Fired after a player has changed dimension
 */
public class PlayerChangedDimensionEvent extends KnightLibEvent {

    private final MinecraftServer server;
    private final ServerPlayer player;
    private final ResourceKey<Level> origin;
    private final ResourceKey<Level> destination;

    public PlayerChangedDimensionEvent(MinecraftServer server, ServerPlayer player, ResourceKey<Level> origin, ResourceKey<Level> destination) {
        this.server = server;
        this.player = player;
        this.origin = origin;
        this.destination = destination;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public ResourceKey<Level> getOrigin() {
        return origin;
    }

    public ResourceKey<Level> getDestination() {
        return destination;
    }

}