package dev.xylonity.knightlib.client.sound.music.internal;

import dev.xylonity.knightlib.api.sound.music.IBossMusicProvider;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches entity music providers to optimize performance by avoiding repeated constant player bb searches
 * for nearby entities each tick, which reduces unnecessary complexity on the client.
 */
public final class BossMusicRegistry {

    private static final Map<Integer, IBossMusicProvider> ENTITIES = new ConcurrentHashMap<>();

    private BossMusicRegistry() {
        ;;
    }

    public static void register(IBossMusicProvider provider) {
        final Entity entity = (Entity) provider;
        ENTITIES.put(entity.getId(), provider);
    }

    public static void unregister(IBossMusicProvider provider) {
        final Entity entity = (Entity) provider;
        ENTITIES.remove(entity.getId());
    }

    public static void unregisterById(int entityId) {
        ENTITIES.remove(entityId);
    }

    public static Collection<IBossMusicProvider> getAll() {
        return ENTITIES.values();
    }

    public static void clear() {
        ENTITIES.clear();
    }

}