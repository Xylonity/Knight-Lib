package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.network.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for the logical packet types owned by one network endpoint
 */
final class PacketTypeRegistry {

    private final Map<ResourceLocation, Class<?>> classesById = new ConcurrentHashMap<>();
    private final Map<Class<?>, ResourceLocation> idsByClass = new ConcurrentHashMap<>();

    private final Map<ResourceLocation, Entry<?>> clientboundById = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Entry<?>> serverboundById = new ConcurrentHashMap<>();

    private final Map<Class<?>, Entry<?>> clientboundByClass = new ConcurrentHashMap<>();
    private final Map<Class<?>, Entry<?>> serverboundByClass = new ConcurrentHashMap<>();

    synchronized <T> void register(Direction direction, PacketType<T> type) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(type, "type");

        final ResourceLocation packetId = Objects.requireNonNull(type.id(), "type.id()");
        final Class<T> packetClass = Objects.requireNonNull(type.clazz(), "type.clazz()");
        Objects.requireNonNull(type.codec(), "type.codec()");

        final Class<?> previousClass = classesById.get(packetId);
        if (previousClass != null) {
            throw new IllegalStateException("[KnightLib] Packet id " + packetId + " is already registered for " + previousClass.getName());
        }

        final ResourceLocation previousId = idsByClass.get(packetClass);
        if (previousId != null) {
            throw new IllegalStateException("[KnightLib] Packet class " + packetClass.getName() + " is already registered as " + previousId);
        }

        final Entry<T> entry = new Entry<>(type);
        entriesById(direction).put(packetId, entry);
        entriesByClass(direction).put(packetClass, entry);
        classesById.put(packetId, packetClass);
        idsByClass.put(packetClass, packetId);
    }

    void encode(Direction direction, ResourceLocation packetId, Object message, FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");

        final Entry<?> entry = requireById(direction, packetId);
        entry.validateMessage(message);
        buffer.writeResourceLocation(packetId);
        entry.encode(message, buffer);
    }

    Decoded decode(Direction direction, FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");

        final ResourceLocation packetId = buffer.readResourceLocation();
        return new Decoded(packetId, decodePayload(direction, packetId, buffer));
    }

    void encodePayload(Direction direction, ResourceLocation packetId, Object message, FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");

        final Entry<?> entry = requireById(direction, packetId);
        entry.validateMessage(message);
        entry.encode(message, buffer);
    }

    Object decodePayload(Direction direction, ResourceLocation packetId, FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        return requireById(direction, packetId).decode(buffer);
    }

    ResourceLocation idForClass(Direction direction, Class<?> packetClass) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(packetClass, "packetClass");

        final Entry<?> entry = entriesByClass(direction).get(packetClass);
        if (entry == null) {
            throw new IllegalStateException("[KnightLib] No " + direction.name + " packet registered for message class " + packetClass.getName());
        }

        return entry.type.id();
    }

    PacketType<?> typeForClass(Direction direction, Class<?> packetClass) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(packetClass, "packetClass");

        final Entry<?> entry = entriesByClass(direction).get(packetClass);
        if (entry == null) {
            throw new IllegalStateException("[KnightLib] No " + direction.name + " packet registered for message class " + packetClass.getName());
        }

        return entry.type;
    }

    void validate(Direction direction, PacketType<?> type, Object message) {
        Objects.requireNonNull(type, "type");

        final Entry<?> entry = requireById(direction, type.id());
        if (entry.type.clazz() != type.clazz()) {
            throw new IllegalArgumentException("[KnightLib] Packet " + type.id()
                    + " is registered for " + entry.type.clazz().getName() + " instead of "
                    + type.clazz().getName());
        }

        entry.validateMessage(message);
    }

    private Entry<?> requireById(Direction direction, ResourceLocation packetId) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(packetId, "packetId");

        final Entry<?> entry = entriesById(direction).get(packetId);
        if (entry == null) {
            throw new IllegalStateException("[KnightLib] Unknown " + direction.name + " packet id " + packetId);
        }

        return entry;
    }

    private Map<ResourceLocation, Entry<?>> entriesById(Direction direction) {
        return direction == Direction.CLIENTBOUND ? clientboundById : serverboundById;
    }

    private Map<Class<?>, Entry<?>> entriesByClass(Direction direction) {
        return direction == Direction.CLIENTBOUND ? clientboundByClass : serverboundByClass;
    }

    private record Entry<T>(
            PacketType<T> type
    ) {

        private void validateMessage(Object message) {
            Objects.requireNonNull(message, "message");
            if (!type.clazz().isInstance(message)) {
                throw new IllegalArgumentException("[KnightLib] Packet " + type.id()
                        + " expects " + type.clazz().getName() + " but received "
                        + message.getClass().getName());
            }

        }

        private void encode(Object message, FriendlyByteBuf buffer) {
            type.codec().encode(type.clazz().cast(message), buffer);
        }

        private Object decode(FriendlyByteBuf buffer) {
            final T message = type.codec().decode(buffer);
            if (message == null) {
                throw new IllegalStateException("[KnightLib] Packet codec for " + type.id()
                        + " decoded a null message");
            }
            if (!type.clazz().isInstance(message)) {
                throw new IllegalStateException("[KnightLib] Packet codec for " + type.id()
                        + " decoded " + message.getClass().getName() + " instead of "
                        + type.clazz().getName());
            }

            return message;
        }

    }

    record Decoded(
            ResourceLocation id,
            Object message
    ) {
        ;;
    }

    enum Direction {
        CLIENTBOUND("clientbound"),
        SERVERBOUND("serverbound");

        private final String name;

        Direction(String name) {
            this.name = name;
        }

    }

}