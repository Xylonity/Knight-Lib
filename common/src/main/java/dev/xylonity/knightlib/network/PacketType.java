package dev.xylonity.knightlib.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Structural definition of a packet. This type does not carry handlers (runnable), so use the specific wrappers (ClientboundPacketType
 * or ServerboundPacketType) when you want types to be self-contained with their default handlers.
 *
 * Packet ids and message classes must each be unique within one {@code NetworkEndpoint}, and they should be registered under common code.
 */
public record PacketType<T>(ResourceLocation id, Class<T> clazz, PacketCodec<T> codec) {

    public static <T> ClientboundPacketType<T> clientbound(ResourceLocation id, Class<T> clazz, PacketCodec<T> codec, Consumer<T> clientHandler) {
        return new ClientboundPacketType<>(id, clazz, codec, clientHandler);
    }

    public static <T> ServerboundPacketType<T> serverbound(ResourceLocation id, Class<T> clazz, PacketCodec<T> codec, BiConsumer<T, ServerPlayer> serverHandler) {
        return new ServerboundPacketType<>(id, clazz, codec, serverHandler);
    }

}
