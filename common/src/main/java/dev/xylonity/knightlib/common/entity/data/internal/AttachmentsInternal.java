package dev.xylonity.knightlib.common.entity.data.internal;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.entity.data.AttachmentType;
import dev.xylonity.knightlib.api.entity.data.PersistentData;
import dev.xylonity.knightlib.network.packets.AttachmentSyncS2C;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Values are saved in 2 places, serialized inside the entity's persistent data (under {@value #ATTACHMENTS_KEY},
 * and deserialized in a per-entity runtime cache to avoid NBT duplication on hot reloads
 */
public final class AttachmentsInternal {

    // Namespaced because on forge the persistent data root is shared across all mods
    public static final String ATTACHMENTS_KEY = "KnightLibAttachments";

    private AttachmentsInternal() {
        ;;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Entity entity, AttachmentType<T> type) {
        final Map<ResourceLocation, Object> cache = cacheOf(entity);
        final Object cached = cache.get(type.id());
        if (cached != null) {
            return (T) cached;
        }

        T value = null;

        final CompoundTag attachments = attachmentsTag(entity, false);
        if (attachments != null && attachments.contains(type.id().toString())) {
            try {
                value = type.serializer().read(attachments.get(type.id().toString()));
            }
            catch (Exception exception) {
                KnightLib.LOGGER.warn("Failed to deserialize attachment {} on entity {}, falling back to default", type.id(), entity.getUUID(), exception);
            }

        }

        if (value == null) {
            value = type.defaultValue();
        }

        cache.put(type.id(), value);
        return value;
    }

    public static <T> void set(Entity entity, AttachmentType<T> type, T value) {
        if (value == null) {
            remove(entity, type);
            return;
        }

        cacheOf(entity).put(type.id(), value);

        final Tag payload = type.serializer().write(value);
        final CompoundTag compoundTag = attachmentsTag(entity, true);
        if (compoundTag != null) {
            compoundTag.put(type.id().toString(), payload);
        }

        sendSync(entity, type, payload);
    }

    public static boolean has(Entity entity, AttachmentType<?> type) {
        final CompoundTag attachments = attachmentsTag(entity, false);
        return attachments != null && attachments.contains(type.id().toString());
    }

    public static void remove(Entity entity, AttachmentType<?> type) {
        cacheOf(entity).remove(type.id());

        final CompoundTag attachments = attachmentsTag(entity, false);
        if (attachments != null) {
            attachments.remove(type.id().toString());
        }

        sendSync(entity, type, null);
    }

    /**
     * Applies a raw synced payload to a (client) entity
     */
    public static void apply(Entity entity, AttachmentType<?> type, @Nullable Tag payload) {
        final Map<ResourceLocation, Object> cache = cacheOf(entity);

        if (payload == null) {
            cache.remove(type.id());

            final CompoundTag attachments = attachmentsTag(entity, false);
            if (attachments != null) {
                attachments.remove(type.id().toString());
            }

            return;
        }

        final CompoundTag compoundTag = attachmentsTag(entity, true);
        if (compoundTag != null) {
            compoundTag.put(type.id().toString(), payload);
        }

        try {
            cache.put(type.id(), type.serializer().read(payload));
        }
        catch (Exception exception) {
            cache.remove(type.id());
            KnightLib.LOGGER.warn("Failed to deserialize synced attachment {} on entity {}", type.id(), entity.getId(), exception);
        }

    }

    /**
     * Sends every stored synced attachment of entity to a single player. Used when a player starts tracking the entity (or for self sync)
     */
    public static void syncAllTo(Entity entity, ServerPlayer player) {
        final CompoundTag attachments = attachmentsTag(entity, false);
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        for (final String key : attachments.getAllKeys()) {
            final ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) {
                continue;
            }

            final AttachmentType<?> type = AttachmentType.byId(id);
            if (type == null || !type.isSynced()) {
                continue;
            }

            KnightLib.NET.sendTo(player, AttachmentSyncS2C.TYPE.base(), new AttachmentSyncS2C(entity.getId(), id, attachments.get(key)));
        }

    }

    /**
     * Carries attachments over to the freshly created player on respawn
     */
    public static void copyForRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wasDeath) {
        final CompoundTag oldAttachments = attachmentsTag(oldPlayer, false);
        if (oldAttachments == null || oldAttachments.isEmpty()) {
            return;
        }

        final CompoundTag copied = new CompoundTag();
        for (final String key : oldAttachments.getAllKeys()) {
            final ResourceLocation id = ResourceLocation.tryParse(key);
            final AttachmentType<?> type = id == null ? null : AttachmentType.byId(id);

            final boolean keep = !wasDeath || (type != null && type.copiesOnDeath());
            if (keep) {
                final Tag oldAttachmentsKey = oldAttachments.get(key);
                if (oldAttachmentsKey != null) {
                    final Tag tag = copied.put(key, oldAttachmentsKey);
                    if (tag != null) {
                        tag.copy();
                    }

                }


            }

        }

        if (!copied.isEmpty()) {
            PersistentData.get(newPlayer).put(ATTACHMENTS_KEY, copied);
        }

    }

    private static <T> void sendSync(Entity entity, AttachmentType<T> type, @Nullable Tag payload) {
        if (!type.isSynced() || !(entity.level() instanceof ServerLevel)) {
            return;
        }

        final AttachmentSyncS2C message = new AttachmentSyncS2C(entity.getId(), type.id(), payload);

        KnightLib.NET.sendToTracking(entity, AttachmentSyncS2C.TYPE.base(), message);
        if (entity instanceof ServerPlayer player) {
            KnightLib.NET.sendTo(player, AttachmentSyncS2C.TYPE.base(), message);
        }

    }

    @Nullable
    private static CompoundTag attachmentsTag(Entity entity, boolean create) {
        final CompoundTag root = PersistentData.get(entity);
        if (!root.contains(ATTACHMENTS_KEY, CompoundTag.TAG_COMPOUND)) {
            if (!create) {
                return null;
            }

            root.put(ATTACHMENTS_KEY, new CompoundTag());
        }

        return root.getCompound(ATTACHMENTS_KEY);
    }

    private static Map<ResourceLocation, Object> cacheOf(Entity entity) {
        return ((KnightLibPersistentDataAccessor) entity).knightlib$getAttachmentCache();
    }

}