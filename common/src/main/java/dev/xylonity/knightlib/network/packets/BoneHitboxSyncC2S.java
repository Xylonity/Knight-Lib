package dev.xylonity.knightlib.network.packets;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitbox;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxHolder;
import dev.xylonity.knightlib.api.entity.hitbox.BoneHitboxManager;
import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Serverbound packet that syncs bone hitbox transforms from the client renderer to the server.
 */
public record BoneHitboxSyncC2S(
        int entityId,
        Map<String, BoneTransform> transforms
) {

    public static final ResourceLocation ID = KnightLib.of("bone_hitbox");

    public static final ServerboundPacketType<BoneHitboxSyncC2S> TYPE =
            PacketType.serverbound(
                    ID,
                    BoneHitboxSyncC2S.class,
                    PacketCodec.of(BoneHitboxSyncC2S::encode, BoneHitboxSyncC2S::decode),
                    BoneHitboxSyncC2S::handle
            );

    /**
     * Compact representation of a bone's world-space transform
     */
    public record BoneTransform(
            Vec3 position,
            Matrix3f rotation,
            @Nullable Vec3 halfExtents
    ) {

        public BoneTransform(Vec3 position, Matrix3f rotation) {
            this(position, rotation, null);
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeDouble(position.x);
            buf.writeDouble(position.y);
            buf.writeDouble(position.z);
            buf.writeFloat(rotation.m00);
            buf.writeFloat(rotation.m01);
            buf.writeFloat(rotation.m02);
            buf.writeFloat(rotation.m10);
            buf.writeFloat(rotation.m11);
            buf.writeFloat(rotation.m12);
            buf.writeFloat(rotation.m20);
            buf.writeFloat(rotation.m21);
            buf.writeFloat(rotation.m22);
            buf.writeBoolean(halfExtents != null);
            if (halfExtents != null) {
                buf.writeFloat((float) halfExtents.x);
                buf.writeFloat((float) halfExtents.y);
                buf.writeFloat((float) halfExtents.z);
            }

        }

        public static BoneTransform decode(FriendlyByteBuf buf) {
            final Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            final Matrix3f rotationMatrix = new Matrix3f(
                    buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat()
            );
            Vec3 half = null;
            if (buf.readBoolean()) {
                half = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
            }

            return new BoneTransform(position, rotationMatrix, half);
        }

    }

    public static void encode(BoneHitboxSyncC2S packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
        buf.writeVarInt(packet.transforms().size());
        for (final Map.Entry<String, BoneTransform> entry : packet.transforms().entrySet()) {
            buf.writeUtf(entry.getKey(), 64);
            entry.getValue().encode(buf);
        }

    }

    public static BoneHitboxSyncC2S decode(FriendlyByteBuf buf) {
        final int entityId = buf.readVarInt();
        final int count = buf.readVarInt();
        final Map<String, BoneTransform> transforms = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String boneName = buf.readUtf(64);
            transforms.put(boneName, BoneTransform.decode(buf));
        }

        return new BoneHitboxSyncC2S(entityId, transforms);
    }

    private static void handle(BoneHitboxSyncC2S packet, ServerPlayer sender) {
        if (sender == null) {
            return;
        }

        final Entity entity = sender.serverLevel().getEntity(packet.entityId());
        if (entity == null) {
            return;
        }

        if (sender.distanceTo(entity) > 128) {
            return;
        }
        if (!(entity instanceof BoneHitboxHolder holder)) {
            return;
        }

        final BoneHitboxManager manager = holder.getBoneHitboxManager();
        if (manager == null) {
            return;
        }

        for (final Map.Entry<String, BoneTransform> entry : packet.transforms().entrySet()) {
            final BoneTransform boneTransform = entry.getValue();

            double distSquare = boneTransform.position().distanceToSqr(entity.position());
            // Caped max distance between entity and the obb hitbox
            if (distSquare > 400) {
                continue;
            }

            // Syncs auto-sized half-extents from client to server
            if (boneTransform.halfExtents() != null) {
                final BoneHitbox hitbox = manager.get(entry.getKey());
                if (hitbox != null && hitbox.isAutoSize() && hitbox.getHalfExtents() == null) {
                    hitbox.setHalfExtents(boneTransform.halfExtents());
                }

            }

            manager.updateBoneTransform(entry.getKey(), boneTransform.position(), boneTransform.rotation());
        }

    }

}
