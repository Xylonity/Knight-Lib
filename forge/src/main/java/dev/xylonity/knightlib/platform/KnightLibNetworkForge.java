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
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class KnightLibNetworkForge implements KnightLibNetwork {

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KnightLib.MOD_ID, "main"),
            () -> Network.PROTOCOL,
            Network.PROTOCOL::equals,
            Network.PROTOCOL::equals
    );

    private static final AtomicInteger IDX = new AtomicInteger(0);

    @Override
    public <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler) {
        CHANNEL.registerMessage(
                IDX.getAndIncrement(),
                type.clazz(),
                type.codec()::encode,
                type.codec()::decode,
                (message, sup) -> {
                    NetworkEvent.Context ctx = sup.get();
                    ctx.enqueueWork(() -> clientHandler.accept(message));
                    ctx.setPacketHandled(true);
                }
        );

    }

    @Override
    public <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler) {
        CHANNEL.registerMessage(
                IDX.getAndIncrement(),
                type.clazz(),
                type.codec()::encode,
                type.codec()::decode,
                (message, sup) -> {
                    NetworkEvent.Context ctx = sup.get();
                    ServerPlayer player = ctx.getSender();
                    if (player != null) {
                        ctx.enqueueWork(() -> serverHandler.accept(message, player));
                    }

                    ctx.setPacketHandled(true);
                }
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
        CHANNEL.sendToServer(message);
    }

    @Override
    public <T> void sendTo(ServerPlayer player, PacketType<T> type, T message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    @Override
    public <T> void sendToAll(MinecraftServer server, PacketType<T> type, T message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    @Override
    public <T> void sendToPlayers(Level level, PacketType<T> type, T message) {
        for (ServerPlayer player : level.players().stream().map(ServerPlayer.class::cast).toList()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }

    }

    @Override
    public <T> void sendToTracking(Level level, BlockPos pos, PacketType<T> type, T message) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (chunk != null) {
            CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
        }

    }

    @Override
    public <T> void sendToTracking(Entity entity, PacketType<T> type, T msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }

}
