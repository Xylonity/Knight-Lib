package dev.xylonity.knightlib.api.sound.persistent.impl;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Immutable descriptor for a named persistent (looping) sound within a {@link PersistentSoundProfile}.
 * The entity attached decides when to play this sound (by name)
 */
public final class PersistentSound {

    private final String name;
    private final Supplier<SoundEvent> event;

    private final float volume;
    private final float pitch;

    private final SoundSource source;

    private final int fadeInTicks;
    private final int fadeOutTicks;

    private final @Nullable Supplier<SoundEvent> onStartSound;
    private final @Nullable Supplier<SoundEvent> onStopSound;

    private final float onStartVolume;
    private final float onStopVolume;

    PersistentSound(
            String name,
            Supplier<SoundEvent> event,
            float volume,
            float pitch,
            SoundSource source,
            int fadeInTicks,
            int fadeOutTicks,
            @Nullable Supplier<SoundEvent> onStartSound,
            @Nullable Supplier<SoundEvent> onStopSound,
            float onStartVolume,
            float onStopVolume
    ) {
        this.name = name;
        this.event = event;
        this.volume = volume;
        this.pitch = pitch;
        this.source = source;
        this.fadeInTicks = fadeInTicks;
        this.fadeOutTicks = fadeOutTicks;
        this.onStartSound = onStartSound;
        this.onStopSound = onStopSound;
        this.onStartVolume = onStartVolume;
        this.onStopVolume = onStopVolume;
    }

    public String getName() {
        return name;
    }

    public SoundEvent getEvent() {
        return event.get();
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public SoundSource getSource() {
        return source;
    }

    public int getFadeInTicks() {
        return fadeInTicks;
    }

    public int getFadeOutTicks() {
        return fadeOutTicks;
    }

    public boolean hasOnStart() {
        return onStartSound != null;
    }

    public boolean hasOnStop() {
        return onStopSound != null;
    }

    public @Nullable SoundEvent getOnStartSound() {
        return onStartSound != null ? onStartSound.get() : null;
    }

    public @Nullable SoundEvent getOnStopSound() {
        return onStopSound != null ? onStopSound.get() : null;
    }

    public float getOnStartVolume() {
        return onStartVolume;
    }

    public float getOnStopVolume() {
        return onStopVolume;
    }

}