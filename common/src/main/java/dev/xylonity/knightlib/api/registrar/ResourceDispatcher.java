package dev.xylonity.knightlib.api.registrar;

import dev.xylonity.knightlib.KnightLib;

import java.util.ArrayList;
import java.util.List;

public final class ResourceDispatcher {

    private static final List<ResourceRegistry<?>> REGISTRIES = new ArrayList<>();

    private ResourceDispatcher() { ;; }

    /**
     * Creates a ResourceRegistry for a given type and modid
     */
    public static <T> ResourceRegistry<T> create(ResourceType type, String modid) {
        ResourceRegistry<T> reg = KnightLib.REGISTRAR.create(type, modid);
        REGISTRIES.add(reg);
        return reg;
    }

    public static void init() {
        for (ResourceRegistry<?> reg : REGISTRIES) {
            reg.init();
        }

    }

}