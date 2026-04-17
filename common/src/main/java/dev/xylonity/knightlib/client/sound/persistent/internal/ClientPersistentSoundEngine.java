package dev.xylonity.knightlib.client.sound.persistent.internal;

import dev.xylonity.knightlib.api.sound.persistent.KnightLibPersistentSounds;
import dev.xylonity.knightlib.api.sound.persistent.impl.PersistentSoundProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Internal implementation of {@link KnightLibPersistentSounds.PersistentSoundEngine}
 */
public final class ClientPersistentSoundEngine implements KnightLibPersistentSounds.PersistentSoundEngine {

    /**
     * One tracker per entity id
     */
    private final Map<Integer, PersistentSoundTracker> trackers = new HashMap<>();

    @Override
    public void tick(Entity entity, String... names) {
        if (entity == null || names.length == 0) {
            return;
        }

        final int id = entity.getId();
        PersistentSoundTracker tracker = trackers.get(id);

        // First call for this entity locates its matching profile and creates a tracker for it
        if (tracker == null) {
            final PersistentSoundProfile<?> profile = findProfile(entity);
            if (profile == null) {
                return;
            }

            tracker = new PersistentSoundTracker(profile);
            trackers.put(id, tracker);
        }

        tracker.tick(entity, Set.of(names));
    }

    @Override
    public void stopAll(Entity entity) {
        if (entity == null) {
            return;
        }

        final PersistentSoundTracker tracker = trackers.remove(entity.getId());
        if (tracker != null) {
            tracker.stopAll();
        }
    }

    @Override
    public void clearAll() {
        for (final PersistentSoundTracker tracker : trackers.values()) {
            tracker.stopAll();
        }

        trackers.clear();
    }

    @Override
    public void endClientTick() {
        final ClientLevel level = Minecraft.getInstance().level;

        final Iterator<Map.Entry<Integer, PersistentSoundTracker>> iterator = trackers.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Integer, PersistentSoundTracker> entry = iterator.next();
            final Entity entity = level != null ? level.getEntity(entry.getKey()) : null;
            final PersistentSoundTracker tracker = entry.getValue();

            tracker.sweep(entity);
            if (tracker.isEmpty()) {
                iterator.remove();
            }

        }

    }

    private PersistentSoundProfile<?> findProfile(Entity entity) {
        for (final PersistentSoundProfile<?> profile : KnightLibPersistentSounds.getProfiles()) {
            if (profile.matches(entity)) {
                return profile;
            }

        }

        return null;
    }

}