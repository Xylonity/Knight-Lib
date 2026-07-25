package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.network.Network;
import dev.xylonity.knightlib.api.util.ResourceLocations;
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
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static dev.xylonity.knightlib.platform.PacketTypeRegistry.Direction.CLIENTBOUND;
import static dev.xylonity.knightlib.platform.PacketTypeRegistry.Direction.SERVERBOUND;

@SuppressWarnings("unchecked")
public class KnightLibNetworkFabric implements KnightLibNetwork {

    private final PacketTypeRegistry packetTypes = new PacketTypeRegistry();

    private final Map<ResourceLocation, Boolean> clientHandlers = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Boolean> serverHandlers = new ConcurrentHashMap<>();

    private final String endpointNamespace;
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

        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }

        final ResourceLocation wireId = wireId(type);
        if (clientHandlers.putIfAbsent(wireId, Boolean.TRUE) != null) {
            return;
        }

        ClientPlayNetworking.registerGlobalReceiver(
                wireId, (client, handler, buf, sender) -> {
                    final T msg = type.clazz().cast(packetTypes.decodePayload(CLIENTBOUND, type.id(), buf));
                    client.execute(() -> clientHandler.accept(msg));
                }

        );

    }

    @Override
    public <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler) {
        Objects.requireNonNull(serverHandler, "serverHandler");
        packetTypes.register(SERVERBOUND, type);

        final ResourceLocation wireId = wireId(type);
        if (serverHandlers.putIfAbsent(wireId, Boolean.TRUE) != null) {
            return;
        }

        ServerPlayNetworking.registerGlobalReceiver(
                wireId, (server, player, handler, buf, sender) -> {
                    final T message = type.clazz().cast(packetTypes.decodePayload(SERVERBOUND, type.id(), buf));
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
        Objects.requireNonNull(message, "message");
        final PacketType<T> type = (PacketType<T>) packetTypes.typeForClass(SERVERBOUND, message.getClass());

        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            KnightLib.LOGGER.warn("sendToServer called on server for {}", type.id());
            return;
        }

        final ResourceLocation wireId = wireId(type);
        try {
            if (!ClientPlayNetworking.canSend(wireId)) {
                return;
            }

        }
        catch (IllegalStateException ignored) {
            return;
        }

        final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packetTypes.encodePayload(SERVERBOUND, type.id(), message, buf);
        ClientPlayNetworking.send(wireId, buf);
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
        final ResourceLocation wireId = wireId(type);
        if (!ServerPlayNetworking.canSend(player, wireId)) {
            return;
        }

        final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packetTypes.encodePayload(CLIENTBOUND, type.id(), message, buf);
        ServerPlayNetworking.send(player, wireId, buf);
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

}
