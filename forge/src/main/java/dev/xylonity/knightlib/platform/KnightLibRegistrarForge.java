package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.api.registrar.ResourceType;
import dev.xylonity.knightlib.registry.registrar.KnightLibResourceRegistryForge;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("unchecked")
public class KnightLibRegistrarForge implements KnightLibRegistrar {

    @Override
    public <T> ResourceRegistry<T> create(Registry<T> type, String modid) {
        DeferredRegister<T> dr = DeferredRegister.create(type.key(), modid);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        dr.register(modBus);

        return new KnightLibResourceRegistryForge<>(dr, modid);
    }

}