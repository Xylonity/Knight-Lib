package dev.xylonity.knightlib.api.sound.persistent.impl;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Builder for constructing a {@link PersistentSound}.
 */
public final class PersistentSoundBuilder<T> {

    private final PersistentSoundProfileBuilder<T> parent;

    private final String name;
    private Supplier<SoundEvent> event;

    private float volume = 0.5f;
    private float pitch = 1f;

    private SoundSource source = SoundSource.NEUTRAL;

    private int fadeInTicks = 0;
    private int fadeOutTicks = 0;

    private @Nullable Supplier<SoundEvent> onStartSound;
    private @Nullable Supplier<SoundEvent> onStopSound;

    private float onStartVolume = 0.5f;
    private float onStopVolume = 0.5f;

    PersistentSoundBuilder(PersistentSoundProfileBuilder<T> parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public PersistentSoundBuilder<T> event(Supplier<SoundEvent> event) {
        this.event = event;
        return this;
    }

    public PersistentSoundBuilder<T> volume(float volume) {
        this.volume = volume;
        return this;
    }

    public PersistentSoundBuilder<T> pitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public PersistentSoundBuilder<T> source(SoundSource source) {
        this.source = source;
        return this;
    }

    public PersistentSoundBuilder<T> fadeIn(int ticks) {
        this.fadeInTicks = Math.max(0, ticks);
        return this;
    }

    public PersistentSoundBuilder<T> fadeOut(int ticks) {
        this.fadeOutTicks = Math.max(0, ticks);
        return this;
    }

    /**
     * Oneshot sound played when this persistent sound activates
     */
    public PersistentSoundBuilder<T> onStart(Supplier<SoundEvent> sound) {
        this.onStartSound = sound;
        return this;
    }

    public PersistentSoundBuilder<T> onStart(Supplier<SoundEvent> sound, float volume) {
        this.onStartSound = sound;
        this.onStartVolume = volume;
        return this;
    }

    /**
     * Oneshot sound played when this persistent sound deactivates.
     */
    public PersistentSoundBuilder<T> onStop(Supplier<SoundEvent> sound) {
        this.onStopSound = sound;
        return this;
    }

    public PersistentSoundBuilder<T> onStop(Supplier<SoundEvent> sound, float volume) {
        this.onStopSound = sound;
        this.onStopVolume = volume;
        return this;
    }

    public PersistentSoundProfileBuilder<T> build() {
        if (event == null) {
            throw new IllegalStateException("[KnightLib] PersistentSound '" + name + "' requires an event.");
        }

        final PersistentSound sound = new PersistentSound(
                name, event, volume, pitch, source,
                fadeInTicks, fadeOutTicks,
                onStartSound, onStopSound,
                onStartVolume, onStopVolume
        );

        parent.addSound(sound);

        return parent;
    }

}