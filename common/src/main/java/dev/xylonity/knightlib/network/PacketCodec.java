package dev.xylonity.knightlib.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Encodes/decodes the contract for a packet message {@code T}
 *
 * The read order while decoding must exactly mirror the write order while encoding.
 */
@FunctionalInterface
public interface PacketCodec<T> {

    void encode(T msg, FriendlyByteBuf buf);

    default T decode(FriendlyByteBuf buf) {
        throw new UnsupportedOperationException("[KnightLib] Decode not implemented");
    }

    /**
     * Helper to build a codec for method references
     */
    static <T> PacketCodec<T> of(Encoder<T> encoder, Decoder<T> decoder) {
       return new PacketCodec<>() {

           @Override
           public void encode(T msg, FriendlyByteBuf buf) {
               encoder.encode(msg, buf);
           }

           @Override
           public T decode(FriendlyByteBuf buf) {
               return decoder.decode(buf);
           }

       };

    }

    @FunctionalInterface
    interface Encoder<T> {
        void encode(T msg, FriendlyByteBuf buf);
    }

    @FunctionalInterface
    interface Decoder<T> {
        T decode(FriendlyByteBuf buf);
    }

}
