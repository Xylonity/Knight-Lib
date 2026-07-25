package dev.xylonity.knightlib.network.packets;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPath;
import dev.xylonity.knightlib.network.ClientPacketDispatcher;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Clientbound packet that plays a camera path (cutscene) on the referenced player's client
 */
public record CameraPathS2C(
        @Nullable CameraPath path
) {

    public static final ResourceLocation ID = KnightLib.of("camera_path");

    public static final ClientboundPacketType<CameraPathS2C> TYPE =
            PacketType.clientbound(
                    ID,
                    CameraPathS2C.class,
                    PacketCodec.of(CameraPathS2C::encode, CameraPathS2C::decode),
                    ClientPacketDispatcher::dispatch

            );

    public static CameraPathS2C play(CameraPath path) {
        return new CameraPathS2C(path);
    }

    public static CameraPathS2C stop() {
        return new CameraPathS2C(null);
    }

    public static void encode(CameraPathS2C packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.path() != null);
        if (packet.path() != null) {
            packet.path().encode(buf);
        }

    }

    public static CameraPathS2C decode(FriendlyByteBuf buf) {
        return new CameraPathS2C(buf.readBoolean() ? CameraPath.decode(buf) : null);
    }

}
