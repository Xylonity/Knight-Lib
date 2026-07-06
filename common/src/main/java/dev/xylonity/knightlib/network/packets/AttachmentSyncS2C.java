package dev.xylonity.knightlib.network.packets;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.entity.data.internal.AttachmentsClient;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Clientbound packet that mirrors a single attachment value of an entity to the client.
 * A null payload means the attachment was removed
 */
public record AttachmentSyncS2C(
        int entityId,
        ResourceLocation typeId,
        @Nullable Tag payload
) {

    public static final ResourceLocation ID = KnightLib.of("attachment_sync");

    public static final ClientboundPacketType<AttachmentSyncS2C> TYPE =
            PacketType.clientbound(
                    ID,
                    AttachmentSyncS2C.class,
                    PacketCodec.of(AttachmentSyncS2C::encode, AttachmentSyncS2C::decode),
                    AttachmentsClient::handle
            );

    public static void encode(AttachmentSyncS2C packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
        buf.writeResourceLocation(packet.typeId());

        final CompoundTag wrapper = new CompoundTag();
        if (packet.payload() != null) {
            wrapper.put("klvalue", packet.payload());
        }

        buf.writeNbt(wrapper);
    }

    public static AttachmentSyncS2C decode(FriendlyByteBuf buf) {
        final int entityId = buf.readVarInt();
        final ResourceLocation typeId = buf.readResourceLocation();

        final CompoundTag wrapper = buf.readNbt();
        final Tag payload = wrapper != null && wrapper.contains("klvalue") ? wrapper.get("klvalue") : null;

        return new AttachmentSyncS2C(entityId, typeId, payload);
    }

}