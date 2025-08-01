package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.api.registrar.ResourceType;

public interface KnightLibRegistrar {
    <T> ResourceRegistry<T> create(ResourceType type, String modid);
}