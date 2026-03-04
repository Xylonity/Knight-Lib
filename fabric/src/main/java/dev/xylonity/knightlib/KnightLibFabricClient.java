package dev.xylonity.knightlib;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.RegisterPostShadersEvent;
import dev.xylonity.knightlib.client.ClientProxy;
import dev.xylonity.knightlib.client.event.KnightLibClientEvents;
import dev.xylonity.knightlib.client.event.KnightLibFabricClientEvents;
import dev.xylonity.knightlib.client.shader.post.internal.PostShaderManager;
import dev.xylonity.knightlib.registry.KnightLibPackets;
import net.fabricmc.api.ClientModInitializer;

public class KnightLibFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KnightLib.PROXY = new ClientProxy();

        // Event hooks
        KnightLibFabricClientEvents.init();

        KnightLibPackets.registerS2C();

        // Internal event registrar
        KnightLibEvents.CLIENT.register(KnightLibClientEvents.class);

        KnightLib.PROXY.updatePersistentSoundsEngine();

        // Event hooks
        KnightLibFabricClientEvents.dispatchRegistrationEvents();
    }

}