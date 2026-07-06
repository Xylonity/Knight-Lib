package dev.xylonity.knightlib.api.entity.data;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A typed, persistent piece of data that can be attached to any entity.
 * <br><br>
 * Types must be created once (during mod's init, on both sides) and stored statically:
 * <br><br>
 * {@code
 *
 * // Define the type once, statically, on both sides
 * public static final AttachmentType<Integer> SOULS = AttachmentType.builder(
 *          ResourceLocations.of("examplemod", "souls"), () -> 0, AttachmentSerializer.INT)
 *              .synced()
 *              .copyOnDeath()
 *              .build();
 *
 * @RegisterEvent
 * public static void onKill(final LivingDeathEvent event) {
 *      if (event.getSource().getEntity() instanceof ServerPlayer player) {
 *              final int total = Attachments.update(player, SOULS, souls -> souls + 1);
 *              player.displayClientMessage(Component.literal("Souls: " + total), true);
 *      }
 *
 * }
 *
 * // Anywhere client side this reads the value the server sent:
 * // final int souls = Attachments.get(minecraft.player, AttachmentsExample.SOULS);
 *
 * }
 * <br><br>
 * Values are saved and loaded with the entity automatically. If {@link Builder#synced()} is set,
 * every change made through {@link Attachments} on the server is mirrored to all clients tracking
 * the entity.
 *
 * @see Attachments the access API
 * @see AttachmentSerializer
 */
public final class AttachmentType<T> {

    private static final Map<ResourceLocation, AttachmentType<?>> REGISTRY = new ConcurrentHashMap<>();

    private final ResourceLocation id;
    private final Supplier<T> defaultValue;
    private final AttachmentSerializer<T> serializer;
    private final boolean synced;
    private final boolean copyOnDeath;

    private AttachmentType(ResourceLocation id, Supplier<T> defaultValue, AttachmentSerializer<T> serializer, boolean synced, boolean copyOnDeath) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.serializer = serializer;
        this.synced = synced;
        this.copyOnDeath = copyOnDeath;
    }

    public static <T> Builder<T> builder(ResourceLocation id, Supplier<T> defaultValue, AttachmentSerializer<T> serializer) {
        return new Builder<>(id, defaultValue, serializer);
    }

    /**
     * Internal lookup used by the sync packet handler
     */
    @Nullable
    public static AttachmentType<?> byId(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public ResourceLocation id() {
        return id;
    }

    public T defaultValue() {
        return defaultValue.get();
    }

    public AttachmentSerializer<T> serializer() {
        return serializer;
    }

    public boolean isSynced() {
        return synced;
    }

    public boolean copiesOnDeath() {
        return copyOnDeath;
    }

    public static final class Builder<T> {

        private final ResourceLocation id;
        private final Supplier<T> defaultValue;
        private final AttachmentSerializer<T> serializer;

        private boolean synced = false;
        private boolean copyOnDeath = false;

        private Builder(ResourceLocation id, Supplier<T> defaultValue, AttachmentSerializer<T> serializer) {
            this.id = id;
            this.defaultValue = defaultValue;
            this.serializer = serializer;
        }

        /**
         * Updated the value in a S2C way
         */
        public Builder<T> synced() {
            this.synced = true;
            return this;
        }

        /**
         * Keeps the value when a player dies and respawns
         */
        public Builder<T> copyOnDeath() {
            this.copyOnDeath = true;
            return this;
        }

        public AttachmentType<T> build() {
            if (id == null || defaultValue == null || serializer == null) {
                throw new IllegalArgumentException("[KnightLib] Attachment types require an id, a default value and a serializer");
            }

            final AttachmentType<T> type = new AttachmentType<>(id, defaultValue, serializer, synced, copyOnDeath);
            if (REGISTRY.putIfAbsent(id, type) != null) {
                throw new IllegalStateException("[KnightLib] Duplicate attachment type: " + id);
            }

            return type;
        }

    }

}
