package dev.xylonity.knightlib;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.RegisterPostShadersEvent;
import dev.xylonity.knightlib.client.ClientProxy;
import dev.xylonity.knightlib.client.event.KnightLibClientEvents;
import dev.xylonity.knightlib.client.event.KnightLibFabricClientEvents;
import dev.xylonity.knightlib.client.shader.post.PostShaderManager;
import dev.xylonity.knightlib.registry.KnightLibPackets;
import net.fabricmc.api.ClientModInitializer;

public class KnightLibFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KnightLib.PROXY = new ClientProxy();

        KnightLibFabricClientEvents.init();
        KnightLibPackets.registerS2C();

        KnightLibEvents.CLIENT.dispatch(new RegisterPostShadersEvent(PostShaderManager::register));

        KnightLibEvents.CLIENT.register(KnightLibClientEvents.class);
    }

}