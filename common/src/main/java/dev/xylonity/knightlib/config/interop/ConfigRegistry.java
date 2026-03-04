package dev.xylonity.knightlib.config.interop;

import dev.xylonity.knightlib.api.config.AutoConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core registrar that maps modIds to their config classes.
 * Used internally by KnightLib to know which config screens to build
 * and by loader-specific code to hook into the mod menu.
 *
 * <p>{@code ConfigComposer} handles registration, don't interact with this class.</p>
 */
public final class ConfigRegistry {

    private static final ConcurrentHashMap<String, List<Class<?>>> CONFIGS = new ConcurrentHashMap<>();

    public static void register(String modId, Class<?> configClass) {
        CONFIGS.computeIfAbsent(modId, string -> Collections.synchronizedList(new ArrayList<>())).add(configClass);
    }

    /**
     * Returns all config classes registered for a given mod, sorted by {@link AutoConfig#order()}.
     */
    public static List<Class<?>> getConfigs(String modId) {
        List<Class<?>> list = CONFIGS.get(modId);
        if (list == null) {
            return Collections.emptyList();
        }

        List<Class<?>> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparingInt(clazz -> {
            AutoConfig meta = clazz.getAnnotation(AutoConfig.class);
            return meta != null ? meta.order() : 0;
        }));

        return Collections.unmodifiableList(sorted);
    }

    /**
     * Returns all registered mod IDs that have at least one config.
     */
    public static Set<String> getRegisteredMods() {
        return Collections.unmodifiableSet(CONFIGS.keySet());
    }

}