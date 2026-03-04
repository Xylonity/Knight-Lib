package dev.xylonity.knightlib;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.client.ClientProxy;
import dev.xylonity.knightlib.common.CommonProxy;
import dev.xylonity.knightlib.common.event.KnightLibServerEvents;
import dev.xylonity.knightlib.common.spawn.internal.KnightLibSpawnBiomeModifier;
import dev.xylonity.knightlib.registry.KnightLibLootModifier;
import dev.xylonity.knightlib.api.config.ConfigComposer;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.registry.KnightLibPackets;
import dev.xylonity.knightlib.registry.KnightLibRecipes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KnightLib.MOD_ID)
public class KnightLibForge {

    public KnightLibForge() {
        KnightLib.PROXY = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        KnightLibLootModifier.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
        KnightLibRecipes.SERIALIZERS.register(modEventBus);
        KnightLibRecipes.TYPES.register(modEventBus);

        ConfigComposer.registerConfig(KnightLib.MOD_ID, KnightLibConfig.class);

        KnightLibPackets.registerAll();

        // Entity biome spawn modifiers hook
        KnightLibSpawnBiomeModifier.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        KnightLibSpawnBiomeModifier.register();

        // Internal event registrar
        KnightLibEvents.SERVER.register(KnightLibServerEvents.class);
        KnightLib.PROXY.registerClientEvents();

        KnightLib.PROXY.updatePersistentSoundsEngine();

        KnightLib.init();
    }

}