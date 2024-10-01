package dev.xylonity.knightlib;

import dev.xylonity.knightlib.compat.config.KnightLibConfig;
import dev.xylonity.knightlib.compat.datagen.KnightLibLootModifier;
import dev.xylonity.knightlib.compat.datagen.KnightLibLootModifierGenerator;
import dev.xylonity.knightlib.compat.particle.StarsetParticle;
import dev.xylonity.knightlib.compat.registry.KnightLibBlocks;
import dev.xylonity.knightlib.compat.registry.KnightLibParticles;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(KnightLibCommon.MOD_ID)
public class KnightLib {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, KnightLibCommon.MOD_ID);
    private static final String KNIGHTQUEST_MOD_ID = "knightquest";

    public KnightLib(IEventBus modEventBus, ModContainer modContainer) throws NoSuchFieldException {

        KnightLibParticles.PARTICLES.register(modEventBus);
        KnightLibBlocks.BLOCKS.register(modEventBus);
        KnightLibLootModifier.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);

        ITEMS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, KnightLibConfig.SPEC, "knightlib.toml");

        modEventBus.addListener(KnightLibClientEvents::registerParticleFactories);
        modEventBus.addListener(KnightLibLootModifierGenerator.KnightLibRecipeGenerator::gatherData);

        KnightLibCommon.init();
    }

    /**
     * Verifies if the `Knight Quest` mod is loaded.
     *
     * @return true if it is loaded, false if not.
     */
    public static boolean isKnightQuestLoaded() {
        return ModList.get().isLoaded(KNIGHTQUEST_MOD_ID);
    }

    @EventBusSubscriber(modid = KnightLibCommon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KnightLibClientEvents {

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {

            event.registerSpriteSet(KnightLibParticles.STARSET_PARTICLE.get(), StarsetParticle.Provider::new);

        }

    }

}