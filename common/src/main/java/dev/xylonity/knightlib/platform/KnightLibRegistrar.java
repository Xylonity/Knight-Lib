package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.registry.registrar.ResourceRegistry;
import dev.xylonity.knightlib.registry.registrar.ResourceType;

public interface KnightLibRegistrar {
    <T> ResourceRegistry<T> create(ResourceType type, String modid);
}