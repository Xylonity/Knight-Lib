package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Fired at the start/end of each client-side player tick
 */
public final class ClientPlayerTickEvent extends KnightLibEvent {

    private final Minecraft client;
    private final LocalPlayer player;
    private final TickPhase phase;

    public ClientPlayerTickEvent(Minecraft client, LocalPlayer player, TickPhase phase) {
        this.client = client;
        this.player = player;
        this.phase = phase;
    }

    public Minecraft getClient() {
        return client;
    }

    public LocalPlayer getPlayer() {
        return player;
    }

    public TickPhase getPhase() {
        return phase;
    }

}