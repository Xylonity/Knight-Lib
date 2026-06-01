package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.registry.registrar.KnightLibResourceRegistryNeoForge;
import net.minecraft.core.Registry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

@SuppressWarnings("unchecked")
public class KnightLibRegistrarNeoForge implements KnightLibRegistrar {

    @Override
    public <T> ResourceRegistry<T> create(Registry<T> type, String modid) {
        DeferredRegister<T> dr = DeferredRegister.create(type.key(), modid);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        dr.register(modBus);

        return new KnightLibResourceRegistryNeoForge<>(dr, modid);
    }

}