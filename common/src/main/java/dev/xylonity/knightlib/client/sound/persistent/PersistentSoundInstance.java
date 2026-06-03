package dev.xylonity.knightlib.client.sound.persistent;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * Tickable sound instance that tracks an entity's position and supports volume fade in/out.
 */
public final class PersistentSoundInstance extends AbstractTickableSoundInstance {

    private final Entity entity;
    private final float baseVolume;

    private float targetVolume;
    private float volumeStep;
    private boolean fadingOut;

    public PersistentSoundInstance(SoundEvent event, SoundSource source, Entity entity, float volume, float pitch) {
        super(event, source, SoundInstance.createUnseededRandom());
        this.entity = entity;
        this.baseVolume = volume;
        this.targetVolume = volume;
        this.looping = true;
        this.delay = 0;
        this.volume = volume;
        this.pitch = pitch;
        this.x = (float) entity.getX();
        this.y = (float) entity.getEyeY();
        this.z = (float) entity.getZ();
    }

    @Override
    public void tick() {
        if (entity.isRemoved()) {
            this.stop();
            return;
        }

        this.x = (float) entity.getX();
        this.y = (float) entity.getEyeY();
        this.z = (float) entity.getZ();

        if (this.volume != targetVolume) {
            this.volume += volumeStep;

            boolean overshot = (volumeStep > 0)
                    ? this.volume >= targetVolume
                    : this.volume <= targetVolume;

            if (overshot) {
                this.volume = targetVolume;
            }

            if (fadingOut && this.volume <= 0.0f) {
                this.volume = 0.0f;
                this.stop();
            }

        }

    }

    public void fadeIn(int ticks) {
        if (ticks <= 0) {
            this.volume = baseVolume;
            this.targetVolume = baseVolume;
            return;
        }

        this.volume = 0f;
        this.targetVolume = baseVolume;
        this.volumeStep = baseVolume / ticks;
        this.fadingOut = false;
    }

    public boolean fadeOut(int ticks) {
        if (isStopped()) {
            return false;
        }

        if (ticks <= 0) {
            this.stop();
            return true;
        }

        this.targetVolume = 0.0f;
        this.volumeStep = -(this.volume / ticks);
        this.fadingOut = true;
        return true;
    }

    public boolean isFadingOut() {
        return fadingOut;
    }

    public void stopImmediately() {
        this.stop();
    }

}