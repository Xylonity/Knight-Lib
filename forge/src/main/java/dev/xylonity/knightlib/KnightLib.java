package dev.xylonity.knightlib;

import dev.xylonity.knightlib.registry.KnightLibLootModifier;
import dev.xylonity.knightlib.config.ConfigComposer;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibRecipes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(KnightLibCommon.MOD_ID)
public class KnightLib {

    public static final String MOD_ID = KnightLibCommon.MOD_ID;
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, KnightLib.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, KnightLib.MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, KnightLib.MOD_ID);

    public KnightLib() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        KnightLibLootModifier.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
        KnightLibBlockEntities.BLOCK_ENTITY.register(modEventBus);
        KnightLibRecipes.SERIALIZERS.register(modEventBus);
        KnightLibRecipes.TYPES.register(modEventBus);
        KnightLibEntities.ENTITY.register(modEventBus);

        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        PARTICLES.register(modEventBus);

        ConfigComposer.registerConfig(KnightLibConfig.class, modEventBus);

        KnightLibCommon.init();
    }

}