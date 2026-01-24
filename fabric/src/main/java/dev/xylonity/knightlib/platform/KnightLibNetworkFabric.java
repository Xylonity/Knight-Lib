package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class KnightLibNetworkFabric implements KnightLibNetwork {

    private final Map<Class<?>, PacketType<?>> classToType = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Boolean> clientHandlers = new ConcurrentHashMap<>();

    @Override
    public KnightLibNetwork createEndpoint(String modId, String protocol) {
        return this;
    }

    @Override
    public <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler) {
        memoize(type);

        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }

        if (clientHandlers.putIfAbsent(type.id(), Boolean.TRUE) != null) {
            return;
        }

        ClientPlayNetworking.registerGlobalReceiver(
                type.id(), (client, handler, buf, sender) -> {
                    T msg = type.codec().decode(buf);
                    client.execute(() -> clientHandler.accept(msg));
                }

        );

    }

    @Override
    public <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler) {
        memoize(type);

        ServerPlayNetworking.registerGlobalReceiver(
                type.id(), (server, player, handler, buf, sender) -> {
                    T message = type.codec().decode(buf);
                    server.execute(() -> serverHandler.accept(message, player));
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
        PacketType<T> type = (PacketType<T>) classToType.get(message.getClass());
        if (type == null) {
            throw new IllegalStateException("[KnightLib] No packet registered for message: " + message.getClass().getName());
        }

        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            KnightLib.LOGGER.warn("sendToServer called on server for {}", type.id());
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        type.codec().encode(message, buf);
        ClientPlayNetworking.send(type.id(), buf);
    }

    @Override
    public <T> void sendTo(ServerPlayer player, PacketType<T> type, T message) {
        if (player == null) {
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        type.codec().encode(message, buf);
        ServerPlayNetworking.send(player, type.id(), buf);
    }

    @Override
    public <T> void sendToAll(MinecraftServer server, PacketType<T> type, T message) {
        if (server == null) {
            return;
        }

        FriendlyByteBuf bufToRelease = new FriendlyByteBuf(Unpooled.buffer());
        type.codec().encode(message, bufToRelease);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf copy = new FriendlyByteBuf(bufToRelease.copy());
            ServerPlayNetworking.send(player, type.id(), copy);
        }

        bufToRelease.release();
    }

    @Override
    public <T> void sendToPlayers(Level level, PacketType<T> type, T message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        FriendlyByteBuf bufToRelease = new FriendlyByteBuf(Unpooled.buffer());
        type.codec().encode(message, bufToRelease);

        for (ServerPlayer player : PlayerLookup.world(serverLevel)) {
            FriendlyByteBuf copy = new FriendlyByteBuf(bufToRelease.copy());
            ServerPlayNetworking.send(player, type.id(), copy);
        }

        bufToRelease.release();
    }

    @Override
    public <T> void sendToTracking(Level level, BlockPos pos, PacketType<T> type, T message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        FriendlyByteBuf bufToRelease = new FriendlyByteBuf(Unpooled.buffer());
        type.codec().encode(message, bufToRelease);

        for (ServerPlayer player : PlayerLookup.tracking(serverLevel, pos)) {
            FriendlyByteBuf copy = new FriendlyByteBuf(bufToRelease.copy());
            ServerPlayNetworking.send(player, type.id(), copy);
        }

        bufToRelease.release();
    }

    @Override
    public <T> void sendToTracking(Entity entity, PacketType<T> type, T message) {
        if (!(entity.level() instanceof ServerLevel)) {
            return;
        }

        FriendlyByteBuf bufToRelease = new FriendlyByteBuf(Unpooled.buffer());
        type.codec().encode(message, bufToRelease);

        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            FriendlyByteBuf copy = new FriendlyByteBuf(bufToRelease.copy());
            ServerPlayNetworking.send(player, type.id(), copy);
        }

        bufToRelease.release();
    }

    private <T> void memoize(PacketType<T> type) {
        classToType.put(type.clazz(), type);
    }

}