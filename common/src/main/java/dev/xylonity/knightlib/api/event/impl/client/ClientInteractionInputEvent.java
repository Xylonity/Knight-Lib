package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Fired on the client right before vanilla handles an attack or use click it would act on (not while the attack is
 * on its miss cooldown or the hands are busy). Cancelling it skips vanilla's handling of that click (no hit, mining,
 * item use or block placement, mostly)
 */
public final class ClientInteractionInputEvent extends KnightLibEvent {

    private final Minecraft client;
    private final LocalPlayer player;
    private final Type type;
    private boolean cancelled = false;
    private boolean consumed = false;

    public ClientInteractionInputEvent(Minecraft client, LocalPlayer player, Type type) {
        this.client = client;
        this.player = player;
        this.type = type;
    }

    public Minecraft getClient() {
        return client;
    }

    public LocalPlayer getPlayer() {
        return player;
    }

    public Type getType() {
        return type;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Only for a cancelled {@link Type#ATTACK}. Whether the click counts as an attack that happened, which keeps the
     * held attack key from mining on that same tick
     */
    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public enum Type {
        /// Attack key pressed (hit/breaking)
        ATTACK,
        /// Attack key held
        CONTINUE_ATTACK,
        /// Use key pressed or repeated
        USE
    }

}