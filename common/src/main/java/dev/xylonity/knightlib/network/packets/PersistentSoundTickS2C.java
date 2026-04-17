package dev.xylonity.knightlib.network.packets;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.sound.persistent.KnightLibPersistentSounds;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Clientbound packet emitted by {@link KnightLibPersistentSounds#tickShared(Entity, String...)}. Each tracking client resolves
 * the entity by id and feeds the persistent sounds engine just like a local tick() call
 */
public record PersistentSoundTickS2C(
        int entityId,
        String[] names
) {

    public static final ResourceLocation ID = KnightLib.of("persistent_sound_tick");

    public static final ClientboundPacketType<PersistentSoundTickS2C> TYPE =
            PacketType.clientbound(
                    ID,
                    PersistentSoundTickS2C.class,
                    PacketCodec.of(PersistentSoundTickS2C::encode, PersistentSoundTickS2C::decode),
                    PersistentSoundTickS2C::handle
            );

    public static void encode(PersistentSoundTickS2C packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId());
        buf.writeVarInt(packet.names().length);
        for (final String name : packet.names()) {
            buf.writeUtf(name);
        }

    }

    public static PersistentSoundTickS2C decode(FriendlyByteBuf buf) {
        final int entityId = buf.readVarInt();
        final int count = buf.readVarInt();
        final String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = buf.readUtf();
        }

        return new PersistentSoundTickS2C(entityId, names);
    }

    private static void handle(PersistentSoundTickS2C packet) {
        final Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        final Entity entity = level.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }

        KnightLibPersistentSounds.tick(entity, packet.names());
    }

}
