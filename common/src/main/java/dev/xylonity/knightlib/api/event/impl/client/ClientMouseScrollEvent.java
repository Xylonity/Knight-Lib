package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;

/**
 * Fired when the mouse wheel scrolls in-game (no screen open). Cancelling it prevents the
 * default handling (hotbar slot switching)
 */
public final class ClientMouseScrollEvent extends KnightLibEvent {

    private final Minecraft client;
    private final double delta;

    private boolean cancelled = false;

    public ClientMouseScrollEvent(Minecraft client, double delta) {
        this.client = client;
        this.delta = delta;
    }

    public Minecraft getClient() {
        return client;
    }

    public double getDelta() {
        return delta;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
