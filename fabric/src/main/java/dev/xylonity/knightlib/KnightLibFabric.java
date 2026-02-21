package dev.xylonity.knightlib;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.common.CommonProxy;
import dev.xylonity.knightlib.common.event.KnightLibFabricServerEvents;
import dev.xylonity.knightlib.common.event.KnightLibServerEvents;
import dev.xylonity.knightlib.config.ConfigComposer;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.datagen.KnightLibLootModifierGenerator;
import dev.xylonity.knightlib.registry.KnightLibPackets;
import dev.xylonity.knightlib.registry.KnightLibRecipes;
import net.fabricmc.api.ModInitializer;

public class KnightLibFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        KnightLib.PROXY = new CommonProxy();

        KnightLibRecipes.init();
        KnightLibLootModifierGenerator.init();

        ConfigComposer.registerConfig(KnightLib.MOD_ID, KnightLibConfig.class);

        KnightLibPackets.registerC2S();

        KnightLibEvents.SERVER.register(KnightLibServerEvents.class);

        KnightLibFabricServerEvents.init();

        KnightLib.init();
    }

}