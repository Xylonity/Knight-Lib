package dev.xylonity.knightlib;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.common.CommonProxy;
import dev.xylonity.knightlib.common.event.KnightLibFabricServerEvents;
import dev.xylonity.knightlib.common.event.KnightLibServerEvents;
import dev.xylonity.knightlib.api.config.ConfigComposer;
import dev.xylonity.knightlib.common.spawn.internal.KnightLibSpawnsFabric;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.datagen.KnightLibLootModifierGenerator;
import dev.xylonity.knightlib.registry.KnightLibPackets;
import dev.xylonity.knightlib.registry.KnightLibRecipeSerializers;
import net.fabricmc.api.ModInitializer;

public class KnightLibFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        KnightLib.PROXY = new CommonProxy();

        KnightLibLootModifierGenerator.init();

        ConfigComposer.registerConfig(KnightLib.MOD_ID, KnightLibConfig.class);

        KnightLibPackets.registerC2S();

        // Internal event registrar
        KnightLibEvents.SERVER.register(KnightLibServerEvents.class);

        // Entity biome spawn modifiers hook
        KnightLibSpawnsFabric.register();

        // Event hooks
        KnightLibFabricServerEvents.init();

        KnightLib.init();
    }

}