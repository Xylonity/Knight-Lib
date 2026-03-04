package dev.xylonity.knightlib.client;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.sound.persistent.KnightLibPersistentSounds;
import dev.xylonity.knightlib.client.event.KnightLibClientEvents;
import dev.xylonity.knightlib.client.sound.persistent.internal.ClientPersistentSoundEngine;
import dev.xylonity.knightlib.proxy.IProxy;

public class ClientProxy implements IProxy {

    @Override
    public void registerClientEvents() {
        KnightLibEvents.CLIENT.register(KnightLibClientEvents.class);
    }

    @Override
    public void updatePersistentSoundsEngine() {
        KnightLibPersistentSounds.installEngine(new ClientPersistentSoundEngine());
    }

}
