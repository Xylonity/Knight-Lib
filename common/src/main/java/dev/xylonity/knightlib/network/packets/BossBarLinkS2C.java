package dev.xylonity.knightlib.network.packets;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.impl.internal.BossBarLinks;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/**
 * Clientbound packet that links a bossbar entry with an entity reference on the client
 */
public record BossBarLinkS2C(
        UUID bossEventId,
        int entityId,
        UUID entityUuid,
        ResourceLocation entityType,
        ResourceLocation dimension
) {

    public static final ResourceLocation ID = KnightLib.of("bossbar_link");

    public static final ClientboundPacketType<BossBarLinkS2C> TYPE =
            PacketType.clientbound(
                    ID,
                    BossBarLinkS2C.class,
                    PacketCodec.of(BossBarLinkS2C::encode, BossBarLinkS2C::decode),
                    message -> BossBarLinks.INSTANCE.put(message.bossEventId(), new BossBarLinks.Ref(message.entityId(), message.entityUuid(), message.entityType(), message.dimension()))
            );

    public static void encode(BossBarLinkS2C packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.bossEventId);
        buf.writeVarInt(packet.entityId);
        buf.writeUUID(packet.entityUuid);
        buf.writeResourceLocation(packet.entityType);
        buf.writeResourceLocation(packet.dimension);
    }

    public static BossBarLinkS2C decode(FriendlyByteBuf buf) {
        return new BossBarLinkS2C(
                buf.readUUID(),
                buf.readVarInt(),
                buf.readUUID(),
                buf.readResourceLocation(),
                buf.readResourceLocation()
        );

    }

}
