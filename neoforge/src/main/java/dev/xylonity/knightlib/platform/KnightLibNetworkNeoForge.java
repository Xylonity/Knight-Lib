package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.network.Network;
import dev.xylonity.knightlib.api.util.ResourceLocations;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import net.minecraft.core.BlockPos;
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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class KnightLibNetworkNeoForge implements KnightLibNetwork {

    private final SimpleChannel channel;

    private final AtomicInteger nextS2C = new AtomicInteger(0);
    private final AtomicInteger nextC2S = new AtomicInteger(0);

    public KnightLibNetworkNeoForge() {
        this(KnightLib.MOD_ID, Network.PROTOCOL);
    }

    public KnightLibNetworkNeoForge(String channelNamespace, String protocol) {
        this.channel = NetworkRegistry.newSimpleChannel(
                ResourceLocations.of(channelNamespace, "main"),
                () -> protocol,
                protocol::equals,
                protocol::equals
        );

    }

    @Override
    public KnightLibNetwork createEndpoint(String modId, String protocol) {
        return new KnightLibNetworkNeoForge(modId, protocol);
    }

    @Override
    public <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler) {
        int id = nextS2C.getAndIncrement();

        channel.registerMessage(
                id,
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
        int id = nextC2S.getAndIncrement();

        channel.registerMessage(
                id,
                type.clazz(),
                type.codec()::encode,
                type.codec()::decode,
                (message, sup) -> {
                    NetworkEvent.Context context = sup.get();
                    ServerPlayer sender = context.getSender();
                    if (sender != null) {
                        context.enqueueWork(() -> serverHandler.accept(message, sender));
                    }

                    context.setPacketHandled(true);
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
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            return minecraft != null && minecraft.getConnection() != null;
        });

        if (Boolean.TRUE.equals(canSend)) {
            channel.sendToServer(message);
        }
    }

    @Override
    public <T> void sendTo(ServerPlayer player, PacketType<T> type, T message) {
        if (player == null) {
            return;
        }

        channel.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    @Override
    public <T> void sendToAll(MinecraftServer server, PacketType<T> type, T message) {
        if (server == null) {
            return;
        }

        channel.send(PacketDistributor.ALL.noArg(), message);
    }

    @Override
    public <T> void sendToPlayers(Level level, PacketType<T> type, T message) {
        if (level == null || level.isClientSide) {
            return;
        }

        for (ServerPlayer player : level.players().stream().map(ServerPlayer.class::cast).toList()) {
            channel.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    @Override
    public <T> void sendToTracking(Level level, BlockPos blockPos, PacketType<T> type, T message) {
        if (level == null || level.isClientSide || !level.isLoaded(blockPos)) {
            return;
        }

        LevelChunk chunk = level.getChunkAt(blockPos);
        if (chunk != null) {
            channel.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
        }
    }

    @Override
    public <T> void sendToTracking(Entity entity, PacketType<T> type, T message) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }

        channel.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), message);
    }

}
