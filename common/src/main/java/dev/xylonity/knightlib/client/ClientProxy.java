package dev.xylonity.knightlib.client;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.client.event.KnightLibClientEvents;
import dev.xylonity.knightlib.proxy.IProxy;

public class ClientProxy implements IProxy {

    @Override
    public void registerClientEvents() {
        KnightLibEvents.CLIENT.register(KnightLibClientEvents.class);
    }

}
