package dev.xylonity.knightlib.client.sound.music.internal;

import dev.xylonity.knightlib.api.sound.music.IBossMusicProvider;
import dev.xylonity.knightlib.client.sound.music.AbstractLoopSound;
import dev.xylonity.knightlib.mixin.AbstractTickableSoundInstanceAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.world.entity.Entity;

import java.util.Map;

public final class BossMusicManager {

    private static final Map<Integer, AbstractTickableSoundInstance> ACTIVE = new Int2ObjectOpenHashMap<>();

    private BossMusicManager() {
        ;;
    }

    public static void clientTick(Minecraft minecraft) {
        final LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        ACTIVE.values().removeIf(AbstractTickableSoundInstance::isStopped);

        for (IBossMusicProvider musicProvider : BossMusicRegistry.getAll()) {
            final Entity entity = (Entity) musicProvider;
            final int id = entity.getId();
            if (ACTIVE.containsKey(id)) {
                continue;
            }
            if (!musicProvider.shouldPlayBossMusic(player)) {
                continue;
            }

            AbstractTickableSoundInstance abstractLoopSound = new AbstractLoopSound(musicProvider, musicProvider.soundSource());

            minecraft.getSoundManager().play(abstractLoopSound);

            ACTIVE.put(id, abstractLoopSound);
        }

    }

    public static void clear() {
        for (AbstractTickableSoundInstance sound : ACTIVE.values()) {
            ((AbstractTickableSoundInstanceAccessor) sound).stopAccessor();
        }

        ACTIVE.clear();
    }

}
