package dev.xylonity.knightlib.api.entity.data;

import dev.xylonity.knightlib.common.entity.data.internal.AttachmentsInternal;
import net.minecraft.world.entity.Entity;

import java.util.function.UnaryOperator;

/**
 * Typed access to per-entity persistent data
 *
 * <p>Usage: {@code final int souls = Attachments.get(entity, CompanionsAttachments.SOULS);}</p>
 * <br><br>
 * The use of set or update is preferred over directly mutating the actual nbt values (as it syncs the data automatically).
 *
 * @see AttachmentType
 */
public final class Attachments {

    private Attachments() {
        ;;
    }

    /**
     * Returns the current value for the given type, or its default if nothing was stored yet.
     * The default is NOT written back
     */
    public static <T> T get(Entity entity, AttachmentType<T> type) {
        return AttachmentsInternal.get(entity, type);
    }

    /**
     * Stores a value on the entity. On the server, synced types are sent to every tracking client
     */
    public static <T> void set(Entity entity, AttachmentType<T> type, T value) {
        AttachmentsInternal.set(entity, type, value);
    }

    public static <T> T update(Entity entity, AttachmentType<T> type, UnaryOperator<T> operator) {
        final T value = operator.apply(get(entity, type));
        set(entity, type, value);
        return value;
    }

    /**
     * Whether an explicit value has been stored for this type (defaults don't count)
     */
    public static boolean has(Entity entity, AttachmentType<?> type) {
        return AttachmentsInternal.has(entity, type);
    }

    /**
     * Removes the stored value (subsequent calls return the default value again)
     */
    public static void remove(Entity entity, AttachmentType<?> type) {
        AttachmentsInternal.remove(entity, type);
    }

    /**
     * Re-serializes the current value and re-sends it to tracking clients
     */
    public static <T> void setDirty(Entity entity, AttachmentType<T> type) {
        AttachmentsInternal.set(entity, type, get(entity, type));
    }

}
