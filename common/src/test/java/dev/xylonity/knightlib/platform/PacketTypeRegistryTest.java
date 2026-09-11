package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static dev.xylonity.knightlib.platform.PacketTypeRegistry.Direction.CLIENTBOUND;
import static dev.xylonity.knightlib.platform.PacketTypeRegistry.Direction.SERVERBOUND;
import static org.junit.jupiter.api.Assertions.*;

class PacketTypeRegistryTest {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("test", "message");
    private static final PacketCodec<String> CODEC = PacketCodec.of(
            (message, buffer) -> buffer.writeUtf(message), FriendlyByteBuf::readUtf);
    private static final PacketType<String> TYPE = new PacketType<>(ID, String.class, CODEC);

    @Test
    void sameTypeCanBeRegisteredInBothDirections() {
        final PacketTypeRegistry registry = new PacketTypeRegistry();
        registry.register(CLIENTBOUND, TYPE);
        registry.register(SERVERBOUND, TYPE);

        assertSame(TYPE, registry.typeForClass(CLIENTBOUND, String.class));
        assertSame(TYPE, registry.typeForClass(SERVERBOUND, String.class));
        assertDoesNotThrow(() -> registry.validate(CLIENTBOUND, TYPE, "message"));
        assertDoesNotThrow(() -> registry.validate(SERVERBOUND, TYPE, "message"));
    }

    @Test
    void duplicateIdInSameDirectionDoesNotReplaceOriginalType() {
        final PacketTypeRegistry registry = new PacketTypeRegistry();
        registry.register(CLIENTBOUND, TYPE);
        final PacketType<Integer> duplicate = new PacketType<>(ID, Integer.class,
                PacketCodec.of((message, buffer) -> buffer.writeInt(message), FriendlyByteBuf::readInt));

        assertThrows(IllegalStateException.class, () -> registry.register(CLIENTBOUND, duplicate));
        assertSame(TYPE, registry.typeForClass(CLIENTBOUND, String.class));
        assertThrows(IllegalStateException.class, () -> registry.typeForClass(CLIENTBOUND, Integer.class));
    }

    @Test
    void duplicateClassInSameDirectionDoesNotReserveNewId() {
        final PacketTypeRegistry registry = new PacketTypeRegistry();
        registry.register(SERVERBOUND, TYPE);
        final ResourceLocation otherId = ResourceLocation.fromNamespaceAndPath("test", "other");

        assertThrows(IllegalStateException.class, () -> registry.register(SERVERBOUND,
                new PacketType<>(otherId, String.class, CODEC)));
        final PacketType<Integer> other = new PacketType<>(otherId, Integer.class,
                PacketCodec.of((message, buffer) -> buffer.writeInt(message), FriendlyByteBuf::readInt));
        assertDoesNotThrow(() -> registry.register(SERVERBOUND, other));
        assertSame(TYPE, registry.typeForClass(SERVERBOUND, String.class));
    }

    @Test
    void repeatedRegistrationInSameDirectionIsRejected() {
        final PacketTypeRegistry registry = new PacketTypeRegistry();
        registry.register(CLIENTBOUND, TYPE);

        assertThrows(IllegalStateException.class, () -> registry.register(CLIENTBOUND, TYPE));
    }

    @Test
    void registrationDoesNotAllowSendingInTheOppositeDirection() {
        final PacketTypeRegistry registry = new PacketTypeRegistry();
        registry.register(CLIENTBOUND, TYPE);

        assertThrows(IllegalStateException.class, () -> registry.validate(SERVERBOUND, TYPE, "message"));
        assertThrows(IllegalStateException.class, () -> registry.typeForClass(SERVERBOUND, String.class));
        assertThrows(IllegalArgumentException.class, () -> registry.validate(CLIENTBOUND, TYPE, 42));
    }

    @Test
    void codecsRemainIndependentForTheSameIdAndClassInEachDirection() {
        final PacketTypeRegistry registry = new PacketTypeRegistry();
        registry.register(CLIENTBOUND, TYPE);
        registry.register(SERVERBOUND, new PacketType<>(ID, String.class, PacketCodec.of(
                (message, buffer) -> buffer.writeInt(Integer.parseInt(message)),
                buffer -> Integer.toString(buffer.readInt()))));

        for (final PacketTypeRegistry.Direction direction : PacketTypeRegistry.Direction.values()) {
            final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                registry.encode(direction, ID, "42", buffer);
                final PacketTypeRegistry.Decoded decoded = registry.decode(direction, buffer);
                assertEquals(ID, decoded.id());
                assertEquals("42", decoded.message());
                assertEquals(0, buffer.readableBytes());
            }
            finally {
                buffer.release();
            }
        }
    }

}
