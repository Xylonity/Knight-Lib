package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.api.event.impl.server.ServerTickEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerWorldLoadEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerWorldUnloadEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

public final class KnightLibFabricServerEvents {

    public static void init() {
        ServerTickEvents.START_SERVER_TICK.register(server ->
                KnightLibEvents.SERVER.dispatch(new ServerTickEvent(server, TickPhase.START))
        );

        ServerTickEvents.END_SERVER_TICK.register(server ->
                KnightLibEvents.SERVER.dispatch(new ServerTickEvent(server, TickPhase.END))
        );

        ServerWorldEvents.UNLOAD.register((server, level) ->
                KnightLibEvents.SERVER.dispatch(new ServerWorldUnloadEvent(server, level))
        );

        ServerWorldEvents.LOAD.register((server, level) ->
                KnightLibEvents.SERVER.dispatch(new ServerWorldLoadEvent(server, level))
        );

    }

}