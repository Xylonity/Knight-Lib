package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;

/**
 * Fired when client connects to the server
 */
public final class ClientLoginEvent extends KnightLibEvent {

    private final Minecraft client;

    public ClientLoginEvent(Minecraft client) {
        this.client = client;
    }

    public Minecraft getClient() {
        return client;
    }

}
