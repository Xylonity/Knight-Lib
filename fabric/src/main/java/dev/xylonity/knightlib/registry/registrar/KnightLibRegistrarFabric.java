package dev.xylonity.knightlib.registry.registrar;

import dev.xylonity.knightlib.platform.KnightLibRegistrar;

public class KnightLibRegistrarFabric implements KnightLibRegistrar {

    @Override
    public <T> ResourceRegistry<T> create(ResourceType type, String modid) {
        return new KnightLibResourceRegistryFabric<>(type, modid);
    }

}