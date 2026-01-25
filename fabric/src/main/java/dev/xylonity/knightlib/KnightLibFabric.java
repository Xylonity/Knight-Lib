package dev.xylonity.knightlib;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.RegisterPostShadersEvent;
import dev.xylonity.knightlib.client.event.KnightLibClientEvents;
import dev.xylonity.knightlib.client.event.KnightLibFabricClientEvents;
import dev.xylonity.knightlib.client.shader.post.PostShaderManager;
import dev.xylonity.knightlib.common.event.KnightLibFabricServerEvents;
import dev.xylonity.knightlib.common.event.KnightLibServerEvents;
import dev.xylonity.knightlib.config.ConfigComposer;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.datagen.KnightLibLootModifierGenerator;
import dev.xylonity.knightlib.registry.KnightLibPackets;
import dev.xylonity.knightlib.registry.KnightLibRecipes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class KnightLibFabric implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        KnightLibRecipes.init();
        KnightLibLootModifierGenerator.init();

        ConfigComposer.registerConfig(KnightLibConfig.class);

        KnightLibPackets.registerC2S();

        KnightLibEvents.SERVER.register(KnightLibServerEvents.class);

        KnightLibFabricServerEvents.init();

        KnightLib.init();
    }

    @Override
    public void onInitializeClient() {
        KnightLibFabricClientEvents.init();
        KnightLibPackets.registerS2C();

        KnightLibEvents.CLIENT.dispatch(new RegisterPostShadersEvent(PostShaderManager::register));

        KnightLibEvents.CLIENT.register(KnightLibClientEvents.class);
    }

}