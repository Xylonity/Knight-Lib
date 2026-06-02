package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

    private final Map<ResourceLocation, CustomPacketPayload.Type<KnightLibPayload>> typeById = new ConcurrentHashMap<>();
    private final Map<Class<?>, ResourceLocation> classToId = new ConcurrentHashMap<>();

    @Override
    public KnightLibNetwork createEndpoint(String modId, String protocol) {
        return this;
    }

    @Override
    public <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler) {
        final CustomPacketPayload.Type<KnightLibPayload> payloadType = memoize(type);
        PayloadTypeRegistry.playS2C().register(payloadType, streamCodec(payloadType, type.codec()));

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            registerClientReceiver(payloadType, clientHandler);
        }

    }

    @Override
    public <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler) {
        final CustomPacketPayload.Type<KnightLibPayload> payloadType = memoize(type);
        PayloadTypeRegistry.playC2S().register(payloadType, streamCodec(payloadType, type.codec()));

        ServerPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) ->
                serverHandler.accept((T) payload.message(), context.player()));
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
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            KnightLib.LOGGER.warn("[KnightLib] sendToServer called on the dedicated server for {}", message.getClass().getName());
            return;
        }

        final ResourceLocation id = classToId.get(message.getClass());
        if (id == null) {
            throw new IllegalStateException("[KnightLib] No packet registered for message: " + message.getClass().getName());
        }

        ClientPlayNetworking.send(new KnightLibPayload(typeById.get(id), message));
    }

    @Override
    public <T> void sendTo(ServerPlayer player, PacketType<T> type, T message) {
        if (player == null) {
            return;
        }

        ServerPlayNetworking.send(player, new KnightLibPayload(typeById.get(type.id()), message));
    }

    @Override
    public <T> void sendToAll(MinecraftServer server, PacketType<T> type, T message) {
        if (server == null) {
            return;
        }

        for (final ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, new KnightLibPayload(typeById.get(type.id()), message));
        }
    }

    @Override
    public <T> void sendToPlayers(Level level, PacketType<T> type, T message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (final ServerPlayer player : PlayerLookup.world(serverLevel)) {
            ServerPlayNetworking.send(player, new KnightLibPayload(typeById.get(type.id()), message));
        }
    }

    @Override
    public <T> void sendToTracking(Level level, BlockPos pos, PacketType<T> type, T message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (final ServerPlayer player : PlayerLookup.tracking(serverLevel, pos)) {
            ServerPlayNetworking.send(player, new KnightLibPayload(typeById.get(type.id()), message));
        }
    }

    @Override
    public <T> void sendToTracking(Entity entity, PacketType<T> type, T message) {
        if (!(entity.level() instanceof ServerLevel)) {
            return;
        }

        for (final ServerPlayer player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, new KnightLibPayload(typeById.get(type.id()), message));
        }
    }

    private <T> CustomPacketPayload.Type<KnightLibPayload> memoize(PacketType<T> type) {
        classToId.put(type.clazz(), type.id());
        return typeById.computeIfAbsent(type.id(), CustomPacketPayload.Type::new);
    }

    private <T> StreamCodec<RegistryFriendlyByteBuf, KnightLibPayload> streamCodec(CustomPacketPayload.Type<KnightLibPayload> payloadType, PacketCodec<T> codec) {
        return StreamCodec.of(
                (buf, payload) -> codec.encode((T) payload.message(), buf),
                buf -> new KnightLibPayload(payloadType, codec.decode(buf))
        );

    }

    private <T> void registerClientReceiver(CustomPacketPayload.Type<KnightLibPayload> payloadType, Consumer<T> clientHandler) {
        ClientPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) ->
                clientHandler.accept((T) payload.message()));
    }

    private record KnightLibPayload(
            Type<KnightLibPayload> type,
            Object message
    ) implements CustomPacketPayload {
        ;;
    }

}
