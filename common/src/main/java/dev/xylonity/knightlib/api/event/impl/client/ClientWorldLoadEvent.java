package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

/**
 * Fired when a client world is loaded (joining server, loading singleplayer)
 */
public final class ClientWorldLoadEvent extends KnightLibEvent {

    private final Minecraft client;
    private final Level level;

    public ClientWorldLoadEvent(Minecraft client, Level level) {
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
