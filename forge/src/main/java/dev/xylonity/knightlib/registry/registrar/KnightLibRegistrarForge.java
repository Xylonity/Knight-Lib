package dev.xylonity.knightlib.registry.registrar;

import dev.xylonity.knightlib.platform.KnightLibRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unchecked")
public class KnightLibRegistrarForge implements KnightLibRegistrar {

    @Override
    public <T> ResourceRegistry<T> create(ResourceType type, String modid) {
        DeferredRegister<T> dr = switch(type) {
            case BLOCKS -> (DeferredRegister<T>) DeferredRegister.create(ForgeRegistries.BLOCKS, modid);
            case ITEMS -> (DeferredRegister<T>) DeferredRegister.create(ForgeRegistries.ITEMS, modid);
            case ENTITIES -> (DeferredRegister<T>) DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, modid);
            case BLOCK_ENTITIES -> (DeferredRegister<T>) DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, modid);
            case EFFECTS -> (DeferredRegister<T>) DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, modid);
            case SOUNDS -> (DeferredRegister<T>) DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, modid);
            case PARTICLES -> (DeferredRegister<T>) DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, modid);
            case CREATIVE_TAB -> (DeferredRegister<T>) DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modid);
            case MENU -> (DeferredRegister<T>) DeferredRegister.create(ForgeRegistries.MENU_TYPES, modid);
            case ENCHANTMENTS -> (DeferredRegister<T>) DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, modid);
            case DIMENSION -> (DeferredRegister<T>) DeferredRegister.create(Registries.DIMENSION, modid);
            case DIMENSION_TYPE -> (DeferredRegister<T>) DeferredRegister.create(Registries.DIMENSION_TYPE, modid);
            case FLUID -> (DeferredRegister<T>) DeferredRegister.create(Registries.FLUID, modid);
            case BIOME -> (DeferredRegister<T>) DeferredRegister.create(Registries.BIOME, modid);
            case BIOME_SOURCE -> (DeferredRegister<T>) DeferredRegister.create(Registries.BIOME_SOURCE, modid);
            case DAMAGE_TYPE -> (DeferredRegister<T>) DeferredRegister.create(Registries.DAMAGE_TYPE, modid);
            case STRUCTURE -> (DeferredRegister<T>) DeferredRegister.create(Registries.STRUCTURE, modid);
            case STRUCTURE_PIECE -> (DeferredRegister<T>) DeferredRegister.create(Registries.STRUCTURE_PIECE, modid);
            case STRUCTURE_TYPE -> (DeferredRegister<T>) DeferredRegister.create(Registries.STRUCTURE_TYPE, modid);
            case STRUCTURE_PLACEMENT -> (DeferredRegister<T>) DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, modid);
            case STRUCTURE_SET -> (DeferredRegister<T>) DeferredRegister.create(Registries.STRUCTURE_SET, modid);
            case STRUCTURE_POOL_ELEMENT -> (DeferredRegister<T>) DeferredRegister.create(Registries.STRUCTURE_POOL_ELEMENT, modid);
            case STRUCTURE_PROCESSOR -> (DeferredRegister<T>) DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, modid);
            case SENSOR_TYPE -> (DeferredRegister<T>) DeferredRegister.create(Registries.SENSOR_TYPE, modid);
        };

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        dr.register(modBus);

        return new KnightLibResourceRegistryForge<>(dr);
    }

}