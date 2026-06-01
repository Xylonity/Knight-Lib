package dev.xylonity.knightlib.api.sound.music;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Client-sided abstraction. Implement this interface on the entity you want to play boss music.
 * The entity is registered and unregistered automatically within the BossMusicRegistry wrapper.
 */
@FunctionalInterface
public interface IBossMusicProvider {

    /**
     * The music must be an instance of AbstractLoopSound
     * @return the music to play
     */
    @NotNull
    SoundEvent getBossMusic();

    /**
     * Predicate if the player selected should play the boss music or not
     * @param listener the player that listens to the music
     */
    default boolean shouldPlayBossMusic(Player listener) {
        final Entity entity = (Entity) this;
        return entity.isAlive() && listener.distanceToSqr(entity) <= getMusicRangeSqr();
    }

    default SoundSource soundSource() {
        return SoundSource.RECORDS;
    }

    /**
     * Entity's internal music range cap
     */
    default double getMusicRangeSqr() {
        return 64 * 64;
    }

    default int getFadeIn() {
        return 40;
    }

    default int getFadeOut() {
        return 40;
    }

}