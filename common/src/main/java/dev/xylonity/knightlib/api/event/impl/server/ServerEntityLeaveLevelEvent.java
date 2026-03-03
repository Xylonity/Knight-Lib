package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Fired when an entity leaves a server-side level
 */
public final class ServerEntityLeaveLevelEvent extends KnightLibEvent {

    private final MinecraftServer server;
    private final ServerLevel level;
    private final Entity entity;

    public ServerEntityLeaveLevelEvent(MinecraftServer server, ServerLevel level, Entity entity) {
        this.server = server;
        this.level = level;
        this.entity = entity;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public Entity getEntity() {
        return entity;
    }

}