package dev.xylonity.knightlib.client.sound.persistent.internal;

import dev.xylonity.knightlib.api.sound.persistent.KnightLibPersistentSounds;
import dev.xylonity.knightlib.api.sound.persistent.impl.PersistentSoundProfile;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Internal implementation of {@link KnightLibPersistentSounds.PersistentSoundEngine}
 */
public final class ClientPersistentSoundEngine implements KnightLibPersistentSounds.PersistentSoundEngine {

    private final Map<Integer, PersistentSoundTracker> trackers = new HashMap<>();

    @Override
    public void tick(Entity entity, String... names) {
        if (entity == null || names.length == 0) {
            return;
        }

        final int id = entity.getId();
        PersistentSoundTracker tracker = trackers.get(id);

        if (tracker == null) {
            PersistentSoundProfile<?> profile = findProfile(entity);
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
        for (PersistentSoundTracker tracker : trackers.values()) {
            tracker.stopAll();
        }

        trackers.clear();
    }

    private PersistentSoundProfile<?> findProfile(Entity entity) {
        for (PersistentSoundProfile<?> profile : KnightLibPersistentSounds.getProfiles()) {
            if (profile.matches(entity)) {
                return profile;
            }

        }

        return null;
    }

}