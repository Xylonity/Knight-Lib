package dev.xylonity.knightlib.client.sound.persistent.internal;

import dev.xylonity.knightlib.api.sound.persistent.impl.PersistentSound;
import dev.xylonity.knightlib.api.sound.persistent.impl.PersistentSoundProfile;
import dev.xylonity.knightlib.client.sound.persistent.PersistentSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Internal manager that manages the runtime sound state for a single entity instance.
 * <br><br>
 * Each tick, the entity specifies which sounds should be active by name.
 */
final class PersistentSoundTracker {

    private final PersistentSoundProfile<?> profile;
    private final Map<String, SoundState> active = new HashMap<>();

    PersistentSoundTracker(PersistentSoundProfile<?> profile) {
        this.profile = profile;
    }

    /**
     * Called once per tick with the set of sound names that should be active.
     */
    void tick(Entity entity, Set<String> requested) {
        for (final String name : requested) {
            final PersistentSound sound = profile.getSound(name);
            if (sound == null) {
                continue;
            }

            SoundState state = active.get(name);
            if (state == null) {
                // New activation
                state = new SoundState();
                active.put(name, state);
                activate(sound, state, entity);
            }
            else if (state.instance != null && state.instance.isFadingOut()) {
                // Was fading out but now requested again
                state.instance.fadeIn(sound.getFadeInTicks());
            }
            else if (state.instance == null || state.instance.isStopped()) {
                // Loop died for some reason
                activate(sound, state, entity);
            }

            state.aliveThisTick = true;
        }

    }

    /**
     * Any sound that's not marked alive during this tick is deactivated. Lets callers that just stop calling tick(...) have their sounds fade out
     * without needing an explicit stop. Entity may be null if it has left the level
     */
    void sweep(Entity entity) {
        final Iterator<Map.Entry<String, SoundState>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, SoundState> entry = iterator.next();
            final SoundState state = entry.getValue();

            // Already stopped
            if (state.instance != null && state.instance.isStopped()) {
                iterator.remove();
                continue;
            }

            if (!state.aliveThisTick) {
                // If the caller didn't tick this sound this tick, fades it out, or either hard-stops the sound
                if (entity != null) {
                    deactivate(entry.getKey(), state, entity);
                }
                else if (state.instance != null) {
                    state.instance.stopImmediately();
                }

                iterator.remove();
            }
            else {
                // Resets for next tick
                state.aliveThisTick = false;
            }

        }

    }

    boolean isEmpty() {
        return active.isEmpty();
    }

    void stopAll() {
        for (final Map.Entry<String, SoundState> entry : active.entrySet()) {
            final SoundState state = entry.getValue();
            if (state.instance != null) {
                state.instance.stopImmediately();
            }

        }

        active.clear();
    }

    private void activate(PersistentSound sound, SoundState state, Entity entity) {
        if (sound.hasOnStart()) {
            playOneShot(sound.getOnStartSound(), sound.getSource(), entity, sound.getOnStartVolume(), sound.getPitch());
        }

        final PersistentSoundInstance instance = new PersistentSoundInstance(
                sound.getEvent(), sound.getSource(),
                entity, sound.getVolume(), sound.getPitch()
        );

        if (sound.getFadeInTicks() > 0) {
            instance.fadeIn(sound.getFadeInTicks());
        }

        state.instance = instance;
        play(instance);
    }

    private void deactivate(String name, SoundState state, Entity entity) {
        final PersistentSound sound = profile.getSound(name);

        if (sound != null && sound.hasOnStop() && entity.isAlive()) {
            playOneShot(sound.getOnStopSound(), sound.getSource(), entity, sound.getOnStopVolume(), sound.getPitch());
        }

        if (state.instance == null || state.instance.isStopped()) {
            return;
        }

        if (sound != null && sound.getFadeOutTicks() > 0) {
            state.instance.fadeOut(sound.getFadeOutTicks());
        }
        else {
            state.instance.stopImmediately();
        }
    }

    private static void play(PersistentSoundInstance instance) {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(instance);
    }

    private static void playOneShot(@Nullable SoundEvent event, SoundSource source, Entity entity, float volume, float pitch) {
        if (event == null) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(new SimpleSoundInstance(
                event, source, volume, pitch,
                SoundInstance.createUnseededRandom(),
                (float) entity.getX(),
                (float) entity.getEyeY(),
                (float) entity.getZ()
        ));

    }

    private static final class SoundState {

        @Nullable
        PersistentSoundInstance instance;

        boolean aliveThisTick;
    }

}