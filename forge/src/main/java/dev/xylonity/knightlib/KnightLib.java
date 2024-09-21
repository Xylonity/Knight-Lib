package dev.xylonity.knightlib;

import dev.xylonity.knightlib.knightquest.particle.StarsetParticle;
import dev.xylonity.knightlib.knightquest.registry.KnightLibBlocks;
import dev.xylonity.knightlib.knightquest.registry.KnightLibParticles;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

@Mod(KnightLibCommon.MOD_ID)
public class KnightLib {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, KnightLibCommon.MOD_ID);
    private static final String KNIGHTQUEST_MODID = "knightquest";

    public KnightLib() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        KnightLibParticles.PARTICLES.register(modEventBus);
        KnightLibBlocks.BLOCKS.register(modEventBus);

        ITEMS.register(modEventBus);

        KnightLibCommon.init();
    }

    /**
     * Verifies if the `Knight Quest` mod is loaded.
     *
     * @return true if it is loaded, false if not.
     */
    public static boolean isKnightQuestLoaded() {
        return ModList.get().isLoaded(KNIGHTQUEST_MODID);
    }

    @Mod.EventBusSubscriber(modid = KnightLibCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KnightLibClientEvents {

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {

            event.registerSpriteSet(KnightLibParticles.STARSET_PARTICLE.get(), StarsetParticle.Provider::new);

        }

    }

}