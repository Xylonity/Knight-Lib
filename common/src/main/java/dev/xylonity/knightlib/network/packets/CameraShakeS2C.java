package dev.xylonity.knightlib.network.packets;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.camera.CameraShakeManager;
import dev.xylonity.knightlib.api.camera.ShakeSettings;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Clientbound packet that shakes the camera of the referenced player on the client
 */
public record CameraShakeS2C(
        ShakeSettings spec,
        boolean replace
) {

    public static final ResourceLocation ID = new ResourceLocation(KnightLib.MOD_ID, "camera_shake");

    public static final ClientboundPacketType<CameraShakeS2C> TYPE =
            PacketType.clientbound(
                    ID,
                    CameraShakeS2C.class,
                    PacketCodec.of(CameraShakeS2C::encode, CameraShakeS2C::decode),
                    message -> CameraShakeManager.shake(message.spec(), message.replace())
            );

    public static void encode(CameraShakeS2C packet, FriendlyByteBuf buf) {
        packet.spec().encode(buf);
        buf.writeBoolean(packet.replace());
    }

    public static CameraShakeS2C decode(FriendlyByteBuf buf) {
        return new CameraShakeS2C(ShakeSettings.decode(buf), buf.readBoolean());
    }

}
