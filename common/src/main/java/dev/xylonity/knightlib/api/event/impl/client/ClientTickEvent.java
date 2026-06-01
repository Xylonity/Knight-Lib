package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import net.minecraft.client.Minecraft;

/**
 * Fired at the start/end of each client tick (20 times per second)
 */
public final class ClientTickEvent extends KnightLibEvent {

    private final Minecraft client;
    private final TickPhase phase;

    public ClientTickEvent(Minecraft client, TickPhase phase) {
        this.client = client;
        this.phase = phase;
    }

    public Minecraft getClient() {
        return client;
    }

    public TickPhase getPhase() {
        return phase;
    }

}
