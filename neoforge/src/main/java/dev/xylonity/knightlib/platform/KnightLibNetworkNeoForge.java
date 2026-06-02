package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.network.Network;
import dev.xylonity.knightlib.api.util.ResourceLocations;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class KnightLibNetworkNeoForge implements KnightLibNetwork {

    private static final List<KnightLibNetworkNeoForge> ENDPOINTS = new CopyOnWriteArrayList<>();

    private final String protocol;
    private final CustomPacketPayload.Type<KnightLibPayload> payloadType;
    private final StreamCodec<RegistryFriendlyByteBuf, KnightLibPayload> streamCodec;

    private final Map<ResourceLocation, PacketCodec<?>> codecs = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Consumer<Object>> clientHandlers = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, BiConsumer<Object, ServerPlayer>> serverHandlers = new ConcurrentHashMap<>();
    private final Map<Class<?>, ResourceLocation> classToId = new ConcurrentHashMap<>();

    public KnightLibNetworkNeoForge() {
        this(KnightLib.MOD_ID, Network.PROTOCOL);
    }

    public KnightLibNetworkNeoForge(String channelNamespace, String protocol) {
        this.protocol = protocol;
        this.payloadType = new CustomPacketPayload.Type<>(ResourceLocations.of(channelNamespace, "main"));
        this.streamCodec = StreamCodec.of(this::encodePayload, this::decodePayload);

        ENDPOINTS.add(this);
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        for (final KnightLibNetworkNeoForge endpoint : ENDPOINTS) {
            PayloadRegistrar registrar = event.registrar(endpoint.protocol);
            registrar.playBidirectional(
                    endpoint.payloadType,
                    endpoint.streamCodec,
                    new DirectionalPayloadHandler<>(endpoint::handleClient, endpoint::handleServer)
            );

        }

    }

    @Override
    public KnightLibNetwork createEndpoint(String modId, String protocol) {
        return new KnightLibNetworkNeoForge(modId, protocol);
    }

    @Override
    public <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler) {
        memoize(type);
        clientHandlers.put(type.id(), (Consumer<Object>) clientHandler);
    }

    @Override
    public <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler) {
        memoize(type);
        serverHandlers.put(type.id(), (BiConsumer<Object, ServerPlayer>) serverHandler);
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
        if (FMLEnvironment.dist != Dist.CLIENT) {
            KnightLib.LOGGER.warn("[KnightLib] sendToServer called on the dedicated server for {}", message.getClass().getName());
            return;
        }

        ResourceLocation id = classToId.get(message.getClass());
        if (id == null) {
            throw new IllegalStateException("[KnightLib] No packet registered for message: " + message.getClass().getName());
        }

        PacketDistributor.sendToServer(new KnightLibPayload(payloadType, id, message));
    }

    @Override
    public <T> void sendTo(ServerPlayer player, PacketType<T> type, T message) {
        if (player == null) {
            return;
        }

        PacketDistributor.sendToPlayer(player, new KnightLibPayload(payloadType, type.id(), message));
    }

    @Override
    public <T> void sendToAll(MinecraftServer server, PacketType<T> type, T message) {
        if (server == null) {
            return;
        }

        PacketDistributor.sendToAllPlayers(new KnightLibPayload(payloadType, type.id(), message));
    }

    @Override
    public <T> void sendToPlayers(Level level, PacketType<T> type, T message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        PacketDistributor.sendToPlayersInDimension(serverLevel, new KnightLibPayload(payloadType, type.id(), message));
    }

    @Override
    public <T> void sendToTracking(Level level, BlockPos blockPos, PacketType<T> type, T message) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.isLoaded(blockPos)) {
            return;
        }

        PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(blockPos), new KnightLibPayload(payloadType, type.id(), message));
    }

    @Override
    public <T> void sendToTracking(Entity entity, PacketType<T> type, T message) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }

        PacketDistributor.sendToPlayersTrackingEntity(entity, new KnightLibPayload(payloadType, type.id(), message));
    }

    private <T> void memoize(PacketType<T> type) {
        codecs.put(type.id(), type.codec());
        classToId.put(type.clazz(), type.id());
    }

    private void encodePayload(RegistryFriendlyByteBuf buf, KnightLibPayload payload) {
        buf.writeResourceLocation(payload.packetId());

        PacketCodec<Object> codec = (PacketCodec<Object>) codecs.get(payload.packetId());
        if (codec == null) {
            throw new IllegalStateException("[KnightLib] No codec registered for packet: " + payload.packetId());
        }

        codec.encode(payload.message(), buf);
    }

    private KnightLibPayload decodePayload(RegistryFriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();

        PacketCodec<Object> codec = (PacketCodec<Object>) codecs.get(id);
        if (codec == null) {
            throw new IllegalStateException("[KnightLib] No codec registered for packet: " + id);
        }

        return new KnightLibPayload(payloadType, id, codec.decode(buf));
    }

    private void handleClient(KnightLibPayload payload, IPayloadContext context) {
        Consumer<Object> handler = clientHandlers.get(payload.packetId());
        if (handler != null) {
            context.enqueueWork(() -> handler.accept(payload.message()));
        }

    }

    private void handleServer(KnightLibPayload payload, IPayloadContext context) {
        BiConsumer<Object, ServerPlayer> handler = serverHandlers.get(payload.packetId());
        if (handler != null && context.player() instanceof ServerPlayer sender) {
            context.enqueueWork(() -> handler.accept(payload.message(), sender));
        }

    }

    private record KnightLibPayload(
            Type<KnightLibPayload> type,
            ResourceLocation packetId,
            Object message
    ) implements CustomPacketPayload {
        ;;
    }

}
