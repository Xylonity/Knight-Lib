package dev.xylonity.knightlib.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

/**
 * Automatic wrapper for server bound (C2S) packets. It also carries the default handler (runnable content of the packet).
 * Always check that the sender is not null and validate message contents (entity existence, range checks, etc.) before
 * mutating world state
 * @param <T>
 */
public final class ServerboundPacketType<T> {

    private final PacketType<T> base;
    private final BiConsumer<T, ServerPlayer> serverHandler;

    public ServerboundPacketType(ResourceLocation id, Class<T> clazz, PacketCodec<T> codec, BiConsumer<T, ServerPlayer> serverHandler) {
        this.base = new PacketType<>(id, clazz, codec);
        this.serverHandler = serverHandler;
    }

    /**
     * Returns the pure {@link PacketType} (id, class, codec) with no handler attached
     */
    public PacketType<T> base() {
        return base;
    }

    /**
     * Returns the default server handler (runnable) associated with this packet type
     */
    public BiConsumer<T, ServerPlayer> handler() {
        return serverHandler;
    }

}
