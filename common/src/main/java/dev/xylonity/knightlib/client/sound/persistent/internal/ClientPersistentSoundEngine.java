package dev.xylonity.knightlib.client.sound.persistent.internal;

import dev.xylonity.knightlib.api.sound.persistent.KnightLibPersistentSounds;
import dev.xylonity.knightlib.api.sound.persistent.impl.PersistentSoundProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Internal implementation of {@link KnightLibPersistentSounds.PersistentSoundEngine}
 */
public final class ClientPersistentSoundEngine implements KnightLibPersistentSounds.PersistentSoundEngine {

    /**
     * Per entity id, one tracker per matching profile
     */
    private final Map<Integer, Map<PersistentSoundProfile<?>, PersistentSoundTracker>> trackers = new HashMap<>();

    @Override
    public void tick(Entity entity, String... names) {
        if (entity == null || names.length == 0) {
            return;
        }

        final Map<PersistentSoundProfile<?>, PersistentSoundTracker> perProfile =
                trackers.computeIfAbsent(entity.getId(), id -> new IdentityHashMap<>());

        Map<PersistentSoundProfile<?>, Set<String>> grouped = null;
        for (final String name : names) {
            final PersistentSoundProfile<?> owner = resolveOwner(entity, name);
            if (owner == null) {
                continue;
            }

            if (grouped == null) {
                grouped = new IdentityHashMap<>();
            }

            grouped.computeIfAbsent(owner, p -> new HashSet<>()).add(name);
        }

        if (grouped == null) {
            return;
        }

        for (final Map.Entry<PersistentSoundProfile<?>, Set<String>> entry : grouped.entrySet()) {
            final PersistentSoundProfile<?> profile = entry.getKey();
            PersistentSoundTracker tracker = perProfile.get(profile);
            if (tracker == null) {
                tracker = new PersistentSoundTracker(profile);
                perProfile.put(profile, tracker);
            }

            tracker.tick(entity, entry.getValue());
        }

    }

    @Override
    public void stopAll(Entity entity) {
        if (entity == null) {
            return;
        }

        final Map<PersistentSoundProfile<?>, PersistentSoundTracker> perProfile = trackers.remove(entity.getId());
        if (perProfile == null) {
            return;
        }

        for (final PersistentSoundTracker tracker : perProfile.values()) {
            tracker.stopAll();
        }

    }

    @Override
    public void clearAll() {
        for (final Map<PersistentSoundProfile<?>, PersistentSoundTracker> perProfile : trackers.values()) {
            for (final PersistentSoundTracker tracker : perProfile.values()) {
                tracker.stopAll();
            }

        }

        trackers.clear();
    }

    @Override
    public void endClientTick() {
        final ClientLevel level = Minecraft.getInstance().level;

        final Iterator<Map.Entry<Integer, Map<PersistentSoundProfile<?>, PersistentSoundTracker>>> iterator = trackers.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Integer, Map<PersistentSoundProfile<?>, PersistentSoundTracker>> entry = iterator.next();
            final Entity entity = level != null ? level.getEntity(entry.getKey()) : null;
            final Map<PersistentSoundProfile<?>, PersistentSoundTracker> perProfile = entry.getValue();

            final Iterator<PersistentSoundTracker> innerIterator = perProfile.values().iterator();
            while (innerIterator.hasNext()) {
                final PersistentSoundTracker tracker = innerIterator.next();
                tracker.sweep(entity);
                if (tracker.isEmpty()) {
                    innerIterator.remove();
                }

            }

            if (perProfile.isEmpty()) {
                iterator.remove();
            }

        }

    }

    private PersistentSoundProfile<?> resolveOwner(Entity entity, String name) {
        for (final PersistentSoundProfile<?> profile : KnightLibPersistentSounds.getProfiles()) {
            if (profile.matches(entity) && profile.getSound(name) != null) {
                return profile;
            }
        }

        return null;
    }

}