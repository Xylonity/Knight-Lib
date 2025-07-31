package dev.xylonity.knightlib;

import dev.xylonity.knightlib.registry.KnightLibLootModifier;
import dev.xylonity.knightlib.config.ConfigComposer;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.registry.KnightLibRecipes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KnightLibCommon.MOD_ID)
public class KnightLib {

    public static final String MOD_ID = KnightLibCommon.MOD_ID;

    public KnightLib() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        KnightLibLootModifier.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
        KnightLibRecipes.SERIALIZERS.register(modEventBus);
        KnightLibRecipes.TYPES.register(modEventBus);

        ConfigComposer.registerConfig(KnightLibConfig.class, modEventBus);

        KnightLibCommon.init();
    }

}