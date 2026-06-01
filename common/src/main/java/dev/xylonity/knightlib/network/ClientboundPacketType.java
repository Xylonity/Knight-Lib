package dev.xylonity.knightlib.network;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Automatic wrapper for client bound (S2C) packets. It also carries the default handler (runnable content of the packet).
 * This class is purely declarative (id, codec, handler), so avoid storing state here.
 * @param <T>
 */
public final class ClientboundPacketType<T> {

    private final PacketType<T> base;
    private final Consumer<T> clientHandler;

    public ClientboundPacketType(ResourceLocation id, Class<T> clazz, PacketCodec<T> codec, Consumer<T> clientHandler) {
        this.base = new PacketType<>(id, clazz, codec);
        this.clientHandler = clientHandler;
    }

    /**
     * Returns the pure {@link PacketType} (id, class, codec) with no handler attached
     */
    public PacketType<T> base() {
        return base;
    }

    /**
     * Returns the default client handler (runnable) associated with this packet type
     */
    public Consumer<T> handler() {
        return clientHandler;
    }

}
