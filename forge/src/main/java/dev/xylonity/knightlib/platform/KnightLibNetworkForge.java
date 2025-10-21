package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.network.Network;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class KnightLibNetworkForge implements KnightLibNetwork {

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KnightLib.MOD_ID, "main"),
            () -> Network.PROTOCOL,
            Network.PROTOCOL::equals,
            Network.PROTOCOL::equals
    );

    private static final Map<Integer, Class<?>> USED = new ConcurrentHashMap<>();

    @Override
    public <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler) {
        CHANNEL.registerMessage(
                discriminator(type, false),
                type.clazz(),
                type.codec()::encode,
                type.codec()::decode,
                (message, sup) -> {
                    NetworkEvent.Context ctx = sup.get();
                    ctx.enqueueWork(() -> clientHandler.accept(message));
                    ctx.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

    }

    @Override
    public <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler) {
        CHANNEL.registerMessage(
                discriminator(type, true),
                type.clazz(),
                type.codec()::encode,
                type.codec()::decode,
                (message, sup) -> {
                    NetworkEvent.Context ctx = sup.get();
                    ServerPlayer sender = ctx.getSender();
                    if (sender != null) {
                        ctx.enqueueWork(() -> serverHandler.accept(message, sender));
                    }

                    ctx.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

    }

    @Override
    public <T> void register(ClientboundPacketType<T> type) {
        this.registerClientbound(type.base(), type.handler());
    }

    @Override
    public <T> void register(ServerboundPacketType<T> type) {
        this.registerServerbound(type.base(), type.handler());
    }

    @Override
    public <T> void sendToServer(T message) {
        Boolean canSend = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return mc != null && mc.getConnection() != null;
        });

        if (Boolean.TRUE.equals(canSend)) {
            CHANNEL.sendToServer(message);
        }

    }

    @Override
    public <T> void sendTo(ServerPlayer player, PacketType<T> type, T message) {
        if (player == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    @Override
    public <T> void sendToAll(MinecraftServer server, PacketType<T> type, T message) {
        if (server == null) return;
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    @Override
    public <T> void sendToPlayers(Level level, PacketType<T> type, T message) {
        if (level == null || level.isClientSide) return;

        for (ServerPlayer player : level.players().stream().map(ServerPlayer.class::cast).toList()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }

    }

    @Override
    public <T> void sendToTracking(Level level, BlockPos pos, PacketType<T> type, T message) {
        if (level == null || level.isClientSide || !level.isLoaded(pos)) return;

        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk != null) {
            CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
        }

    }

    @Override
    public <T> void sendToTracking(Entity entity, PacketType<T> type, T msg) {
        if (entity == null || entity.level().isClientSide) return;

        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }

    private static int discriminator(PacketType<?> type, boolean c2s) {
        ResourceLocation id = type.id();
        int base = 31 * id.getNamespace().hashCode() + id.getPath().hashCode();
        int discriminator = (base ^ (c2s ? 0x433253 : 0x533243)) & 0x7fffffff;

        Class<?> prev = USED.putIfAbsent(discriminator, type.clazz());
        if (prev != null && prev != type.clazz()) {
            discriminator = (31 * base + type.clazz().getName().hashCode()) & 0x7fffffff;
            USED.put(discriminator, type.clazz());
        }

        return discriminator;
    }

}
