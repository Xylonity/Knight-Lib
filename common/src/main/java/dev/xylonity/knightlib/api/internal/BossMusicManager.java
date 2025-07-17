package dev.xylonity.knightlib.api.internal;

import dev.xylonity.knightlib.api.IBossMusicProvider;
import dev.xylonity.knightlib.api.impl.BossMusicRegistry;
import dev.xylonity.knightlib.mixin.AbstractTickableSoundInstanceAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public final class BossMusicManager {
    private static final Map<Integer, AbstractLoopSound> ACTIVE = new Int2ObjectOpenHashMap<>();

    private BossMusicManager() { ;; }

    public static void clientTick(Minecraft mc) {
        Player player = mc.player;
        if (player == null) return;

        ACTIVE.values().removeIf(AbstractTickableSoundInstance::isStopped);

        for (IBossMusicProvider entity : BossMusicRegistry.getAll()) {
            Entity e = (Entity) entity;
            int id = e.getId();
            if (ACTIVE.containsKey(id)) continue;
            if (!entity.shouldPlayBossMusic(player)) continue;

            AbstractLoopSound sound = new AbstractLoopSound(entity, SoundSource.MUSIC);
            mc.getSoundManager().play(sound);
            ACTIVE.put(id, sound);
        }
    }

    public static void clear() {
        for (AbstractLoopSound sound : ACTIVE.values()) {
            ((AbstractTickableSoundInstanceAccessor) sound).stopAccessor();
        }

        ACTIVE.clear();
    }

}
