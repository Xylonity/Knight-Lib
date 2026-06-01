package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

/**
 * Fired when a client world is unloaded (leaving server, closing world)
 */
public final class ClientWorldUnloadEvent extends KnightLibEvent {

    private final Minecraft client;
    private final Level level;

    public ClientWorldUnloadEvent(Minecraft client, Level level) {
        this.client = client;
        this.level = level;
    }

    public Minecraft getClient() {
        return client;
    }

    public Level getLevel() {
        return level;
    }

}
