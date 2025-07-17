package dev.xylonity.knightlib;

import dev.xylonity.knightlib.common.event.KnightLibClientEvents;
import dev.xylonity.knightlib.common.event.KnightLibCommonEvents;
import dev.xylonity.knightlib.config.ConfigComposer;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.datagen.KnightLibLootModifierGenerator;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibRecipes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class KnightLib implements ModInitializer, ClientModInitializer {

    public static final String MOD_ID = KnightLibCommon.MOD_ID;

    @Override
    public void onInitialize() {
        KnightLibBlockEntities.init();
        KnightLibEntities.init();
        KnightLibRecipes.init();

        KnightLibLootModifierGenerator.init();

        ConfigComposer.registerConfig(KnightLibConfig.class);

        KnightLibCommonEvents.init();

        KnightLibCommon.init();
    }

    @Override
    public void onInitializeClient() {
        KnightLibClientEvents.init();
    }

}