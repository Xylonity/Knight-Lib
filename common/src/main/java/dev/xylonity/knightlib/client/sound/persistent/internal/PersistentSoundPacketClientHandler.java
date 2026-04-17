package dev.xylonity.knightlib.client.sound.persistent.internal;

import dev.xylonity.knightlib.api.sound.persistent.KnightLibPersistentSounds;
import dev.xylonity.knightlib.network.packets.PersistentSoundTickS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Client handler for {@link PersistentSoundTickS2C}
 */
public final class PersistentSoundPacketClientHandler {

    private PersistentSoundPacketClientHandler() {
        ;;
    }

    public static void handle(PersistentSoundTickS2C packet) {
        final Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        final Entity entity = level.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }

        KnightLibPersistentSounds.tick(entity, packet.names());
    }

}
