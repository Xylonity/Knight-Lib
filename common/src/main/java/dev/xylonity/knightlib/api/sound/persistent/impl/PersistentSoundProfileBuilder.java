package dev.xylonity.knightlib.api.sound.persistent.impl;

import dev.xylonity.knightlib.api.sound.persistent.KnightLibPersistentSounds;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Builder for constructing a {@link PersistentSoundProfile}.
 *
 * @param <T> the target type
 */
public final class PersistentSoundProfileBuilder<T> {

    private final Class<T> targetClass;
    private Predicate<Object> matcher;
    private final Map<String, PersistentSound> sounds = new LinkedHashMap<>();

    public PersistentSoundProfileBuilder(Class<T> targetClass) {
        this.targetClass = targetClass;
        this.matcher = targetClass::isInstance;
    }

    /**
     * Overrides the default instanceof matcher with a custom predicate (for example, when multiple entities of
     * the same instance may play different persistent sounds based on the context)
     */
    public PersistentSoundProfileBuilder<T> matcher(Predicate<Object> matcher) {
        this.matcher = matcher;
        return this;
    }

    /**
     * Starts building a new named persistent sound
     */
    public PersistentSoundBuilder<T> sound(String name) {
        if (sounds.containsKey(name)) {
            throw new IllegalArgumentException("[KnightLib] Duplicated persistent sound name: " + name);
        }

        return new PersistentSoundBuilder<>(this, name);
    }

    void addSound(PersistentSound sound) {
        sounds.put(sound.getName(), sound);
    }

    /**
     * Finalizes and registers this profile
     */
    public PersistentSoundProfile<T> submit() {
        if (sounds.isEmpty()) {
            throw new IllegalStateException("[KnightLib] PersistentSoundProfile for " + targetClass.getSimpleName() + " has no sounds");
        }

        final PersistentSoundProfile<T> profile = new PersistentSoundProfile<>(targetClass, matcher, sounds);
        KnightLibPersistentSounds.addProfile(profile);

        return profile;
    }

}