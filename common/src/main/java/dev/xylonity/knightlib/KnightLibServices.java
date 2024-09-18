package dev.xylonity.knightlib;

import dev.xylonity.knightlib.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public class KnightLibServices {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        KnightLibConstants.LOG.debug("Loaded {} for service {}", loadedService, clazz);

        return loadedService;
    }
}