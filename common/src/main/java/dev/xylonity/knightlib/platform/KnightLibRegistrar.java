package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.api.registrar.ResourceType;
import net.minecraft.core.Registry;

public interface KnightLibRegistrar {
    <T> ResourceRegistry<T> create(Registry<T> type, String modid);
}