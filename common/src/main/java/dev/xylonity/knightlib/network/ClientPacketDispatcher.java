package dev.xylonity.knightlib.network;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.event.impl.client.ClientPacketHandlerRegistrationEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Helper dispatch class for client-reference packet message registration.
 *
 * <p>For loader-independent client registration, subscribe to {@link ClientPacketHandlerRegistrationEvent} from common initialization.</p>
 */
public final class ClientPacketDispatcher {

    private static final Map<Class<?>, Consumer<Object>> HANDLERS = new ConcurrentHashMap<>();

    private ClientPacketDispatcher() {
        ;;
    }

    @SuppressWarnings("unchecked")
    public static <T> void register(Class<T> type, Consumer<T> handler) {
        HANDLERS.put(type, (Consumer<Object>) handler);
    }

    public static void dispatch(Object message) {
        final Consumer<Object> handler = HANDLERS.get(message.getClass());
        if (handler == null) {
            KnightLib.LOGGER.warn("[KnightLib] No physical-client handler registered for {}", message.getClass().getName());
            return;
        }

        handler.accept(message);
    }

}
