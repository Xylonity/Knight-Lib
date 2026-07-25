package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.network.ClientPacketDispatcher;

import java.util.function.Consumer;

/**
 * Fired on the physical client to install handlers used by {@link ClientPacketDispatcher}
 */
public final class ClientPacketHandlerRegistrationEvent extends KnightLibEvent {

    public <T> void register(Class<T> messageType, Consumer<T> handler) {
        ClientPacketDispatcher.register(messageType, handler);
    }

    @Override
    public boolean isSticky() {
        return true;
    }

}
