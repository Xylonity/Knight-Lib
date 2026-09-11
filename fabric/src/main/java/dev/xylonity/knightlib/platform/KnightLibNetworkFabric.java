package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.network.Network;
import dev.xylonity.knightlib.api.util.ResourceLocations;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
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

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static dev.xylonity.knightlib.platform.PacketTypeRegistry.Direction.CLIENTBOUND;
import static dev.xylonity.knightlib.platform.PacketTypeRegistry.Direction.SERVERBOUND;

@SuppressWarnings("unchecked")
public class KnightLibNetworkFabric implements KnightLibNetwork {

    private final PacketTypeRegistry packetTypes = new PacketTypeRegistry();

    private final Map<Object, Set<ResourceLocation>> unsupportedPackets = new WeakHashMap<>();

    private final String endpointNamespace;
    private final String protocol;
    private final String encodedProtocol;

    public KnightLibNetworkFabric() {
        this(KnightLib.MOD_ID, Network.PROTOCOL);
    }

    private KnightLibNetworkFabric(String endpointNamespace, String protocol) {
        if (endpointNamespace == null || endpointNamespace.isBlank()) {
            throw new IllegalArgumentException("[KnightLib] endpointNamespace cannot be blank");
        }
        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("[KnightLib] protocol cannot be blank");
        }

        this.endpointNamespace = endpointNamespace;
        this.protocol = protocol;
        this.encodedProtocol = encodeProtocol(protocol);
    }

    @Override
    public KnightLibNetwork createEndpoint(String modId, String protocol) {
        return new KnightLibNetworkFabric(modId, protocol);
    }

    @Override
    public <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler) {
        Objects.requireNonNull(clientHandler, "clientHandler");
        packetTypes.register(CLIENTBOUND, type);

        final CustomPacketPayload.Type<KnightLibPayload> payloadType = payloadType(type);
        PayloadTypeRegistry.playS2C().register(payloadType, streamCodec(payloadType, CLIENTBOUND, type));

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) ->
                    clientHandler.accept(type.clazz().cast(payload.message())));
        }

    }

    @Override
    public <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler) {
        Objects.requireNonNull(serverHandler, "serverHandler");
        packetTypes.register(SERVERBOUND, type);

        final CustomPacketPayload.Type<KnightLibPayload> payloadType = payloadType(type);
        PayloadTypeRegistry.playC2S().register(payloadType, streamCodec(payloadType, SERVERBOUND, type));
        ServerPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) ->
                serverHandler.accept(type.clazz().cast(payload.message()), context.player()));
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
        final PacketType<T> type = (PacketType<T>) packetTypes.typeForClass(SERVERBOUND, message.getClass());

        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            KnightLib.LOGGER.warn("sendToServer called on server for {}", type.id());
            return;
        }

        final var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }

        final CustomPacketPayload.Type<KnightLibPayload> payloadType = payloadType(type);
        if (!ClientPlayNetworking.canSend(payloadType)) {
            warnUnsupportedPacket(connection, type, "server", "C2S");
            return;
        }

        ClientPlayNetworking.send(new KnightLibPayload(payloadType, message));
    }

    @Override
    public <T> void sendTo(ServerPlayer player, PacketType<T> type, T message) {
        if (player == null) {
            return;
        }

        sendToPlayer(player, type, message);
    }

    @Override
    public <T> void sendToAll(MinecraftServer server, PacketType<T> type, T message) {
        if (server == null) {
            return;
        }

        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer(player, type, message);
        }

    }

    @Override
    public <T> void sendToPlayers(Level level, PacketType<T> type, T message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (final ServerPlayer player : PlayerLookup.world(serverLevel)) {
            sendToPlayer(player, type, message);
        }

    }

    @Override
    public <T> void sendToTracking(Level level, BlockPos pos, PacketType<T> type, T message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (final ServerPlayer player : PlayerLookup.tracking(serverLevel, pos)) {
            sendToPlayer(player, type, message);
        }

    }

    @Override
    public <T> void sendToTracking(Entity entity, PacketType<T> type, T message) {
        if (!(entity.level() instanceof ServerLevel)) {
            return;
        }

        for (final ServerPlayer player : PlayerLookup.tracking(entity)) {
            sendToPlayer(player, type, message);
        }

    }

    private <T> void sendToPlayer(ServerPlayer player, PacketType<T> type, T message) {
        packetTypes.validate(CLIENTBOUND, type, message);
        final CustomPacketPayload.Type<KnightLibPayload> payloadType = payloadType(type);
        if (!ServerPlayNetworking.canSend(player, payloadType)) {
            warnUnsupportedPacket(player.connection, type, player.getGameProfile().getName(), "S2C");
            return;
        }

        ServerPlayNetworking.send(player, new KnightLibPayload(payloadType, message));
    }

    private void warnUnsupportedPacket(Object connection, PacketType<?> type, String recipient, String direction) {
        synchronized (unsupportedPackets) {
            if (!unsupportedPackets.computeIfAbsent(connection, ignored -> new HashSet<>()).add(type.id())) {
                return;
            }

        }

        KnightLib.LOGGER.warn("Skipping {} packet {} to {}: endpoint {} requires protocol {}, but the receiver has not advertised channel {}. Check that both sides have compatible mod versions "
                + "and registered the packet receiver. Further warnings for this packet on this connection are suppressed.", direction, type.id(), recipient, endpointNamespace, protocol, wireId(type));
    }

    private <T> StreamCodec<RegistryFriendlyByteBuf, KnightLibPayload> streamCodec(CustomPacketPayload.Type<KnightLibPayload> payloadType, PacketTypeRegistry.Direction direction, PacketType<T> type) {
        return StreamCodec.of(
                (buf, payload) -> packetTypes.encodePayload(direction, type.id(), payload.message(), buf),
                buf -> new KnightLibPayload(payloadType, packetTypes.decodePayload(direction, type.id(), buf))
        );

    }

    private CustomPacketPayload.Type<KnightLibPayload> payloadType(PacketType<?> type) {
        return new CustomPacketPayload.Type<>(wireId(type));
    }

    private ResourceLocation wireId(PacketType<?> type) {
        return ResourceLocations.of(endpointNamespace, "network/" + encodedProtocol + "/" + type.id().getNamespace() + "/" + type.id().getPath());
    }

    private static String encodeProtocol(String protocol) {
        final byte[] bytes = protocol.getBytes(StandardCharsets.UTF_8);
        final StringBuilder encoded = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            encoded.append(Character.forDigit((value >>> 4) & 0xF, 16));
            encoded.append(Character.forDigit(value & 0xF, 16));
        }

        return encoded.toString();
    }

    private record KnightLibPayload(
            Type<KnightLibPayload> type,
            Object message
    ) implements CustomPacketPayload {
        ;;
    }

}
