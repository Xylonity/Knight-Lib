package dev.xylonity.knightlib.network.packets;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxHolder;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxManager;
import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Serverbound packet that requests an attack against a client-selected animated bone hitbox. The server resolves
 * the entity and validates the current OBB, attack reach and block visibility before invoking the hitbox behavior.
 */
public record BoneHitboxAttackC2S(
        int entityId,
        String boneName
) {

    public static final ResourceLocation ID = KnightLib.of("bone_hitbox_attack");

    public static final ServerboundPacketType<BoneHitboxAttackC2S> TYPE =
            PacketType.serverbound(
                    ID,
                    BoneHitboxAttackC2S.class,
                    PacketCodec.of(BoneHitboxAttackC2S::encode, BoneHitboxAttackC2S::decode),
                    BoneHitboxAttackC2S::handle
            );

    public BoneHitboxAttackC2S {
        if (boneName == null || boneName.isBlank() || boneName.length() > 64) {
            throw new IllegalArgumentException("[Knightlib] boneName must contain 1..64 characters");
        }

    }

    private static void encode(BoneHitboxAttackC2S packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
        buf.writeUtf(packet.boneName(), 64);
    }

    private static BoneHitboxAttackC2S decode(FriendlyByteBuf buf) {
        return new BoneHitboxAttackC2S(buf.readVarInt(), buf.readUtf(64));
    }

    private static void handle(BoneHitboxAttackC2S packet, ServerPlayer player) {
        final Entity entity = player.level().getEntity(packet.entityId());
        if (!(entity instanceof final BoneHitboxHolder holder)) {
            return;
        }

        final BoneHitboxManager manager = holder.getBoneHitboxManager();
        if (manager != null && manager.getOwner() == entity) {
            manager.handleAttack(packet.boneName(), player);
        }

    }

}