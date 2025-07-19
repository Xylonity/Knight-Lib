package dev.xylonity.knightlib;

import dev.xylonity.knightlib.config.ConfigComposer;
import dev.xylonity.knightlib.config.KnightLibConfig;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibLootModifier;
import dev.xylonity.knightlib.registry.KnightLibRecipes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(KnightLibCommon.MOD_ID)
public class KnightLib {

    public static final String MOD_ID = KnightLibCommon.MOD_ID;
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, KnightLib.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, KnightLib.MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, KnightLib.MOD_ID);

    public KnightLib(IEventBus modEventBus, ModContainer modContainer) {

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