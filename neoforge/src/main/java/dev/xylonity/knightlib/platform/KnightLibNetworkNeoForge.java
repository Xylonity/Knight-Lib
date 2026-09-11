package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.network.Network;
import dev.xylonity.knightlib.api.util.ResourceLocations;
import dev.xylonity.knightlib.network.ClientboundPacketType;
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
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static dev.xylonity.knightlib.platform.PacketTypeRegistry.Direction.CLIENTBOUND;
import static dev.xylonity.knightlib.platform.PacketTypeRegistry.Direction.SERVERBOUND;

@SuppressWarnings("unchecked")
public class KnightLibNetworkNeoForge implements KnightLibNetwork {

    private static final List<KnightLibNetworkNeoForge> ENDPOINTS = new CopyOnWriteArrayList<>();

    private final String protocol;
    private final CustomPacketPayload.Type<ClientboundPayload> clientboundPayloadType;
    private final CustomPacketPayload.Type<ServerboundPayload> serverboundPayloadType;
    private final StreamCodec<RegistryFriendlyByteBuf, ClientboundPayload> clientboundStreamCodec;
    private final StreamCodec<RegistryFriendlyByteBuf, ServerboundPayload> serverboundStreamCodec;

    private final PacketTypeRegistry packetTypes = new PacketTypeRegistry();

    private final Map<ResourceLocation, Consumer<Object>> clientHandlers = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, BiConsumer<Object, ServerPlayer>> serverHandlers = new ConcurrentHashMap<>();

    public KnightLibNetworkNeoForge() {
        this(KnightLib.MOD_ID, Network.PROTOCOL);
    }

    public KnightLibNetworkNeoForge(String channelNamespace, String protocol) {
        if (channelNamespace == null || channelNamespace.isBlank()) {
            throw new IllegalArgumentException("[KnightLib] channelNamespace cannot be blank");
        }
        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("[KnightLib] protocol cannot be blank");
        }

        this.protocol = protocol;
        this.clientboundPayloadType = new CustomPacketPayload.Type<>(ResourceLocations.of(channelNamespace, "network/clientbound"));
        this.serverboundPayloadType = new CustomPacketPayload.Type<>(ResourceLocations.of(channelNamespace, "network/serverbound"));
        this.clientboundStreamCodec = StreamCodec.of(this::encodeClientbound, this::decodeClientbound);
        this.serverboundStreamCodec = StreamCodec.of(this::encodeServerbound, this::decodeServerbound);

        ENDPOINTS.add(this);
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        for (final KnightLibNetworkNeoForge endpoint : ENDPOINTS) {
            final PayloadRegistrar registrar = event.registrar(endpoint.protocol);
            registrar.playToClient(endpoint.clientboundPayloadType, endpoint.clientboundStreamCodec, endpoint::handleClient);
            registrar.playToServer(endpoint.serverboundPayloadType, endpoint.serverboundStreamCodec, endpoint::handleServer);
        }

    }

    @Override
    public KnightLibNetwork createEndpoint(String modId, String protocol) {
        return new KnightLibNetworkNeoForge(modId, protocol);
    }

    @Override
    public <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler) {
        Objects.requireNonNull(clientHandler, "clientHandler");
        packetTypes.register(CLIENTBOUND, type);
        clientHandlers.put(type.id(), message -> clientHandler.accept(type.clazz().cast(message)));
    }

    @Override
    public <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler) {
        Objects.requireNonNull(serverHandler, "serverHandler");
        packetTypes.register(SERVERBOUND, type);
        serverHandlers.put(type.id(), (message, player) -> serverHandler.accept(type.clazz().cast(message), player));
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
        Objects.requireNonNull(message, "message");
        final ResourceLocation packetId = packetTypes.idForClass(SERVERBOUND, message.getClass());

        if (FMLEnvironment.dist != Dist.CLIENT) {
            KnightLib.LOGGER.warn("sendToServer called on server for {}", packetId);
            return;
        }

        PacketDistributor.sendToServer(new ServerboundPayload(serverboundPayloadType, packetId, message));
    }

    @Override
    public <T> void sendTo(ServerPlayer player, PacketType<T> type, T message) {
        if (player == null) {
            return;
        }

        PacketDistributor.sendToPlayer(player, clientboundPayload(type, message));
    }

    @Override
    public <T> void sendToAll(MinecraftServer server, PacketType<T> type, T message) {
        if (server == null) {
            return;
        }

        PacketDistributor.sendToAllPlayers(clientboundPayload(type, message));
    }

    @Override
    public <T> void sendToPlayers(Level level, PacketType<T> type, T message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        PacketDistributor.sendToPlayersInDimension(serverLevel, clientboundPayload(type, message));
    }

    @Override
    public <T> void sendToTracking(Level level, BlockPos blockPos, PacketType<T> type, T message) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.isLoaded(blockPos)) {
            return;
        }

        PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(blockPos), clientboundPayload(type, message));
    }

    @Override
    public <T> void sendToTracking(Entity entity, PacketType<T> type, T message) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }

        PacketDistributor.sendToPlayersTrackingEntity(entity, clientboundPayload(type, message));
    }

    private void encodeClientbound(RegistryFriendlyByteBuf buffer, ClientboundPayload payload) {
        packetTypes.encode(CLIENTBOUND, payload.packetId(), payload.message(), buffer);
    }

    private ClientboundPayload decodeClientbound(RegistryFriendlyByteBuf buffer) {
        final PacketTypeRegistry.Decoded decoded = packetTypes.decode(CLIENTBOUND, buffer);
        return new ClientboundPayload(clientboundPayloadType, decoded.id(), decoded.message());
    }

    private void encodeServerbound(RegistryFriendlyByteBuf buffer, ServerboundPayload payload) {
        packetTypes.encode(SERVERBOUND, payload.packetId(), payload.message(), buffer);
    }

    private ServerboundPayload decodeServerbound(RegistryFriendlyByteBuf buffer) {
        final PacketTypeRegistry.Decoded decoded = packetTypes.decode(SERVERBOUND, buffer);
        return new ServerboundPayload(serverboundPayloadType, decoded.id(), decoded.message());
    }

    private void handleClient(ClientboundPayload payload, IPayloadContext context) {
        final Consumer<Object> handler = clientHandlers.get(payload.packetId());
        if (handler == null) {
            throw new IllegalStateException("[KnightLib] No client handler registered for " + payload.packetId());
        }

        context.enqueueWork(() -> handler.accept(payload.message()));
    }

    private void handleServer(ServerboundPayload payload, IPayloadContext context) {
        final BiConsumer<Object, ServerPlayer> handler = serverHandlers.get(payload.packetId());
        if (handler == null) {
            throw new IllegalStateException("[KnightLib] No server handler registered for " + payload.packetId());
        }
        if (context.player() instanceof ServerPlayer sender) {
            context.enqueueWork(() -> handler.accept(payload.message(), sender));
        }

    }

    private <T> ClientboundPayload clientboundPayload(PacketType<T> type, T message) {
        packetTypes.validate(CLIENTBOUND, type, message);
        return new ClientboundPayload(clientboundPayloadType, type.id(), message);
    }

    private record ClientboundPayload(
            Type<ClientboundPayload> type,
            ResourceLocation packetId,
            Object message
    ) implements CustomPacketPayload {
        ;;
    }

    private record ServerboundPayload(
            Type<ServerboundPayload> type,
            ResourceLocation packetId,
            Object message
    ) implements CustomPacketPayload {
        ;;
    }

}
