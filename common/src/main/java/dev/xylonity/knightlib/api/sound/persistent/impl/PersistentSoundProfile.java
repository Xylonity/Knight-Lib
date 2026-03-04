package dev.xylonity.knightlib.api.sound.persistent.impl;

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Groups named {@link PersistentSound} definitions for an object type.
 * The profile is matched against ticked objects and the entity code decides which
 * sound to play by name.
 *
 * @param <T> the type this profile applies to
 */
public final class PersistentSoundProfile<T> {

    private final Class<T> targetClass;
    private final Predicate<Object> matcher;
    private final Map<String, PersistentSound> sounds;

    PersistentSoundProfile(Class<T> targetClass, Predicate<Object> matcher, Map<String, PersistentSound> sounds) {
        this.targetClass = targetClass;
        this.matcher = matcher;
        this.sounds = Collections.unmodifiableMap(sounds);
    }

    public Class<T> getTargetClass() {
        return targetClass;
    }

    public boolean matches(Object object) {
        return matcher.test(object);
    }

    public PersistentSound getSound(String name) {
        return sounds.get(name);
    }

    public Map<String, PersistentSound> getSounds() {
        return sounds;
    }

}