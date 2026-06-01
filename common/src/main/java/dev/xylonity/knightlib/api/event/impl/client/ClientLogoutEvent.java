package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;

/**
 * Fired when the local client is disconnecting from the server
 */
public final class ClientLogoutEvent extends KnightLibEvent {

    private final Minecraft client;

    public ClientLogoutEvent(Minecraft client) {
        this.client = client;
    }

    public Minecraft getClient() {
        return client;
    }

}
