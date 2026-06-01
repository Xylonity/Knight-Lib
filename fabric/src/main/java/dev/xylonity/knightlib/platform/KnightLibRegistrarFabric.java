package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.registry.registrar.KnightLibResourceRegistryFabric;
import net.minecraft.core.Registry;

public class KnightLibRegistrarFabric implements KnightLibRegistrar {

    @Override
    public <T> ResourceRegistry<T> create(Registry<T> type, String modid) {
        return new KnightLibResourceRegistryFabric<>(type, modid);
    }

}