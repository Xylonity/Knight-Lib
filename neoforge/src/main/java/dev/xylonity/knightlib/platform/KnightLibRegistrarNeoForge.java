package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.registry.registrar.KnightLibResourceRegistryNeoForge;
import net.minecraft.core.Registry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

public class KnightLibRegistrarNeoForge implements KnightLibRegistrar {

    @Override
    public <T> ResourceRegistry<T> create(Registry<T> type, String modid) {
        DeferredRegister<T> deferredRegister = DeferredRegister.create(type.key(), modid);

        ModContainer container = ModList.get().getModContainerById(modid).orElseThrow(() ->
                new IllegalStateException("[KnightLib] No mod container found for '" + modid + "'"));

        final IEventBus bus = container.getEventBus();
        if (bus == null) {
            throw new IllegalStateException("[KnightLib] Mod '" + modid + "' has no mod event bus");
        }

        deferredRegister.register(bus);

        return new KnightLibResourceRegistryNeoForge<>(deferredRegister, modid);
    }

}
