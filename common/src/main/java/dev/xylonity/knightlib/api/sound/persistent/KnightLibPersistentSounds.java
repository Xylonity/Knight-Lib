package dev.xylonity.knightlib.api.sound.persistent;

import dev.xylonity.knightlib.api.sound.persistent.impl.PersistentSoundProfile;
import dev.xylonity.knightlib.api.sound.persistent.impl.PersistentSoundProfileBuilder;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistent (looping) sound management API, meant for constant or stated-dependant small sound effects (like an idle sound from a robot)
 *
 * <h4>Profile registration (client init):</h4>
 * <pre>{@code
 * KnightLibPersistentSounds.profile(DragonflyEntity.class)
 *     .sound("fly")
 *         .event(ClockworkSounds.DRAGONFLY_FLY)
 *         .volume(0.4f)
 *         .fadeIn(10).fadeOut(15)
 *         .build()
 *     .sound("idle")
 *         .event(ClockworkSounds.DRAGONFLY_IDLE)
 *         .volume(0.4f)
 *         .build()
 *     .submit();
 * }</pre>
 *
 * <h4>Usage from the entity:</h4>
 * <pre>{@code
 * // in tick()
 * if (level.isClientSide()) {
 *      if (getState() == 1) {
 *          KnightLibPersistentSounds.tick(this, "fly");
 *      }
 *      else {
 *          KnightLibPersistentSounds.tick(this, "idle");
 *      }
 * }
 *
 * // In remove()
 * if (level.isClientSide()) {
 *      KnightLibPersistentSounds.stopAll(this);
 * }
 *}</pre>
 */
public final class KnightLibPersistentSounds {

    private static final List<PersistentSoundProfile<?>> PROFILES = new ArrayList<>();
    private static volatile PersistentSoundEngine engine = PersistentSoundEngine.NOOP;

    private KnightLibPersistentSounds() {
        ;;
    }

    /**
     * Creates a builder for a new persistent sound profile.
     * MUST be called during client initialization.
     */
    public static <T extends Entity> PersistentSoundProfileBuilder<T> profile(Class<T> targetClass) {
        return new PersistentSoundProfileBuilder<>(targetClass);
    }

    /**
     * Ticks the named sounds for the given entity.
     * Sounds not in this list that were previously active will be stopped (or faded out).
     */
    public static void tick(Entity entity, String... names) {
        engine.tick(entity, names);
    }

    /**
     * Immediately stops all persistent sounds for the given entity.
     */
    public static void stopAll(Entity entity) {
        engine.stopAll(entity);
    }

    /**
     * Clears all tracked sound state.
     */
    public static void clearAll() {
        engine.clearAll();
    }

    public static void addProfile(PersistentSoundProfile<?> profile) {
        PROFILES.add(profile);
    }

    public static List<PersistentSoundProfile<?>> getProfiles() {
        return Collections.unmodifiableList(PROFILES);
    }

    /**
     * KnightLib updates the engine ONLY on the client via proxy, in order to avoid untimely crashes if any
     * persistent sound is ticked on the server. Internal, do not use
     */
    public static void installEngine(PersistentSoundEngine clientEngine) {
        engine = (clientEngine != null) ? clientEngine : PersistentSoundEngine.NOOP;
    }

    public interface PersistentSoundEngine {

        void tick(Entity entity, String... names);
        void stopAll(Entity entity);
        void clearAll();

        PersistentSoundEngine NOOP = new PersistentSoundEngine() {

            @Override
            public void tick(Entity entity, String... names) {
                ;;
            }

            @Override
            public void stopAll(Entity entity) {
                ;;
            }

            @Override
            public void clearAll() {
                ;;
            }

        };

    }

}