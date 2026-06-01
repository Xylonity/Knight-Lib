package dev.xylonity.knightlib.api.sound.persistent;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.sound.persistent.impl.PersistentSoundProfile;
import dev.xylonity.knightlib.api.sound.persistent.impl.PersistentSoundProfileBuilder;
import dev.xylonity.knightlib.network.packets.PersistentSoundTickS2C;
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
 * <h4>Usage from an entity:</h4>
 * <pre>{@code
 * // in Entity#tick()
 * if (level.isClientSide()) {
 *      if (getState() == 1) {
 *          KnightLibPersistentSounds.tick(this, "fly");
 *      }
 *      else {
 *          KnightLibPersistentSounds.tick(this, "idle");
 *      }
 * }
 *
 * // stopAll is optional: if you simply stop calling tick(), the end of the current client tick will deactivate active sounds
 * if (level.isClientSide()) {
 *      KnightLibPersistentSounds.stopAll(this);
 * }
 * }</pre>
 *
 * <h4>Shared usage (audible to nearby players, in case the source of the sound is caused by the client player, like when using an item):</h4>
 * <pre>{@code
 * // Wherever runs on the server
 * KnightLibPersistentSounds.tickShared(player, "companions:cake");
 * }</pre>
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
     * Ticks the named sounds for the given entity on the local client. Use this when the caller code already runs on
     * every nearby client. Sounds no longer ticked are deactivated by the end of the client tick.
     */
    public static void tick(Entity entity, String... names) {
        engine.tick(entity, names);
    }

    /**
     * When invoked from the server, throws a S2C packet to every client tracking the entity so nearby players can hear
     * the sound at the entity's position. When invoked from the client, ticks locally as {@link #tick} does
     *
     * <p>Calling this on both sides should be safe. The using client ticks locally, the server broadcasts, and the {@code aliveThisTick}
     * flag makes a redundant network packet on the using player a no-op
     */
    public static void tickShared(Entity entity, String... names) {
        if (entity == null || names.length == 0) {
            return;
        }

        if (entity.level().isClientSide()) {
            engine.tick(entity, names);
        }
        else {
            KnightLib.NET.sendToTracking(entity, PersistentSoundTickS2C.TYPE.base(), new PersistentSoundTickS2C(entity.getId(), names));
        }
    }

    /**
     * Immediately stops all persistent sounds for the given entity (local client)
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

    /**
     * Runs the end of client tick sweep. Intended to be invoked once per client tick by the
     * client event bus. Deactivates sounds that were not ticked this round. Internal, do not use
     */
    public static void endClientTick() {
        engine.endClientTick();
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
        void endClientTick();

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

            @Override
            public void endClientTick() {
                ;;
            }

        };

    }

}
