package dev.xylonity.knightlib.client.entity.data.internal;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.entity.data.AttachmentType;
import dev.xylonity.knightlib.common.entity.data.internal.AttachmentsInternal;
import dev.xylonity.knightlib.network.packets.AttachmentSyncS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-sided attachment sync handling
 */
public final class AttachmentsClient {

    // In case the player isn't loaded yet when it spawns
    private static final Map<Integer, List<AttachmentSyncS2C>> PENDING = new ConcurrentHashMap<>();

    private AttachmentsClient() {
        ;;
    }

    public static void handle(AttachmentSyncS2C message) {
        final ClientLevel level = Minecraft.getInstance().level;

        final Entity entity = level == null ? null : level.getEntity(message.entityId());
        if (entity == null) {
            if (PENDING.size() >= 1024) {
                PENDING.clear();
            }

            PENDING.computeIfAbsent(message.entityId(), id -> new ArrayList<>()).add(message);
            return;
        }

        apply(entity, message);
    }

    /**
     * Launches remaining payloads in case they are parked down
     */
    public static void onEntityJoin(Entity entity) {
        final List<AttachmentSyncS2C> pending = PENDING.remove(entity.getId());
        if (pending == null) {
            return;
        }

        for (final AttachmentSyncS2C message : pending) {
            apply(entity, message);
        }

    }

    public static void clearAll() {
        PENDING.clear();
    }

    private static void apply(Entity entity, AttachmentSyncS2C message) {
        final AttachmentType<?> type = AttachmentType.byId(message.typeId());
        if (type == null) {
            KnightLib.LOGGER.warn("[CLIENT] Received sync for unknown attachment type {}", message.typeId());
            return;
        }

        AttachmentsInternal.apply(entity, type, message.payload());
    }

}