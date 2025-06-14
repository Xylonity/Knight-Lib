package dev.xylonity.knightlib;

import dev.xylonity.knightlib.compat.config.KnightLibConfig;
import dev.xylonity.knightlib.compat.datagen.KnightLibLootModifier;
import dev.xylonity.knightlib.compat.particle.StarsetParticle;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(KnightLibCommon.MOD_ID)
public class KnightLib {

    public static final String MOD_ID = KnightLibCommon.MOD_ID;
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, KnightLib.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, KnightLib.MOD_ID);

    public KnightLib() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        KnightLibParticles.PARTICLES.register(modEventBus);
        KnightLibLootModifier.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
        KnightLibBlockEntities.BLOCK_ENTITY.register(modEventBus);
        KnightLibRecipes.SERIALIZERS.register(modEventBus);
        KnightLibRecipes.TYPES.register(modEventBus);
        KnightLibEntities.ENTITY.register(modEventBus);

        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, KnightLibConfig.SPEC, "knightlib.toml");

        KnightLibCommon.init();
    }

    @Mod.EventBusSubscriber(modid = KnightLibCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class KnightLibClientEvents {

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {

            event.registerSpriteSet(KnightLibParticles.STARSET_PARTICLE.get(), StarsetParticle.Provider::new);

        }

    }

}