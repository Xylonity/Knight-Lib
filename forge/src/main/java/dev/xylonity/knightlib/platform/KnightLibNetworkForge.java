package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.network.Network;
import dev.xylonity.knightlib.api.util.ResourceLocations;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static dev.xylonity.knightlib.platform.PacketTypeRegistry.Direction.CLIENTBOUND;
import static dev.xylonity.knightlib.platform.PacketTypeRegistry.Direction.SERVERBOUND;

public class KnightLibNetworkForge implements KnightLibNetwork {

    private static final int CLIENTBOUND_ID = 0;
    private static final int SERVERBOUND_ID = 1;

    private final SimpleChannel channel;

    private final PacketTypeRegistry packetTypes = new PacketTypeRegistry();

    private final Map<ResourceLocation, Consumer<Object>> clientHandlers = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, BiConsumer<Object, ServerPlayer>> serverHandlers = new ConcurrentHashMap<>();

    public KnightLibNetworkForge() {
        this(KnightLib.MOD_ID, Network.PROTOCOL);
    }

    public KnightLibNetworkForge(String channelNamespace, String protocol) {
        if (channelNamespace == null || channelNamespace.isBlank()) {
            throw new IllegalArgumentException("[KnightLib] channelNamespace cannot be blank");
        }
        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("[KnightLib] protocol cannot be blank");
        }

        this.channel = NetworkRegistry.newSimpleChannel(
                ResourceLocations.of(channelNamespace, "main"),
                () -> protocol,
                protocol::equals,
                protocol::equals
        );

        registerWrappers();
    }

    @Override
    public KnightLibNetwork createEndpoint(String modId, String protocol) {
        return new KnightLibNetworkForge(modId, protocol);
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
        serverHandlers.put(type.id(),
                (message, player) -> serverHandler.accept(type.clazz().cast(message), player));
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
        final Boolean canSend = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            return minecraft != null && minecraft.getConnection() != null;
        });

        if (Boolean.TRUE.equals(canSend)) {
            Objects.requireNonNull(message, "message");
            final ResourceLocation packetId = packetTypes.idForClass(SERVERBOUND, message.getClass());
            channel.sendToServer(new ServerboundWrapper(packetId, message));
        }

    }

    @Override
    public <T> void sendTo(ServerPlayer player, PacketType<T> type, T message) {
        if (player == null) {
            return;
        }

        final ClientboundWrapper wrapper = clientboundWrapper(type, message);
        channel.send(PacketDistributor.PLAYER.with(() -> player), wrapper);
    }

    @Override
    public <T> void sendToAll(MinecraftServer server, PacketType<T> type, T message) {
        if (server == null) {
            return;
        }

        channel.send(PacketDistributor.ALL.noArg(), clientboundWrapper(type, message));
    }

    @Override
    public <T> void sendToPlayers(Level level, PacketType<T> type, T message) {
        if (level == null || level.isClientSide) {
            return;
        }

        final ClientboundWrapper wrapper = clientboundWrapper(type, message);
        for (final ServerPlayer player : level.players().stream().map(ServerPlayer.class::cast).toList()) {
            channel.send(PacketDistributor.PLAYER.with(() -> player), wrapper);
        }

    }

    @Override
    public <T> void sendToTracking(Level level, BlockPos blockPos, PacketType<T> type, T message) {
        if (level == null || level.isClientSide || !level.isLoaded(blockPos)) {
            return;
        }

        final LevelChunk chunk = level.getChunkAt(blockPos);
        if (chunk != null) {
            channel.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), clientboundWrapper(type, message));
        }

    }

    @Override
    public <T> void sendToTracking(Entity entity, PacketType<T> type, T message) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }

        channel.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                clientboundWrapper(type, message));
    }

    private void registerWrappers() {
        channel.registerMessage(
                CLIENTBOUND_ID,
                ClientboundWrapper.class,
                this::encodeClientbound,
                this::decodeClientbound,
                (wrapper, supplier) -> {
                    final NetworkEvent.Context context = supplier.get();
                    final Consumer<Object> handler = clientHandlers.get(wrapper.packetId());
                    if (handler == null) {
                        throw new IllegalStateException("[KnightLib] No client handler registered for " + wrapper.packetId());
                    }

                    context.enqueueWork(() -> handler.accept(wrapper.message()));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        channel.registerMessage(
                SERVERBOUND_ID,
                ServerboundWrapper.class,
                this::encodeServerbound,
                this::decodeServerbound,
                (wrapper, supplier) -> {
                    final NetworkEvent.Context context = supplier.get();
                    final ServerPlayer sender = context.getSender();
                    final BiConsumer<Object, ServerPlayer> handler = serverHandlers.get(wrapper.packetId());
                    if (handler == null) {
                        throw new IllegalStateException("[KnightLib] No server handler registered for " + wrapper.packetId());
                    }
                    if (sender != null) {
                        context.enqueueWork(() -> handler.accept(wrapper.message(), sender));
                    }

                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

    }

    private void encodeClientbound(ClientboundWrapper wrapper, FriendlyByteBuf buffer) {
        packetTypes.encode(CLIENTBOUND, wrapper.packetId(), wrapper.message(), buffer);
    }

    private ClientboundWrapper decodeClientbound(FriendlyByteBuf buffer) {
        final PacketTypeRegistry.Decoded decoded = packetTypes.decode(CLIENTBOUND, buffer);
        return new ClientboundWrapper(decoded.id(), decoded.message());
    }

    private void encodeServerbound(ServerboundWrapper wrapper, FriendlyByteBuf buffer) {
        packetTypes.encode(SERVERBOUND, wrapper.packetId(), wrapper.message(), buffer);
    }

    private ServerboundWrapper decodeServerbound(FriendlyByteBuf buffer) {
        final PacketTypeRegistry.Decoded decoded = packetTypes.decode(SERVERBOUND, buffer);
        return new ServerboundWrapper(decoded.id(), decoded.message());
    }

    private <T> ClientboundWrapper clientboundWrapper(PacketType<T> type, T message) {
        packetTypes.validate(CLIENTBOUND, type, message);
        return new ClientboundWrapper(type.id(), message);
    }

    private record ClientboundWrapper(
            ResourceLocation packetId,
            Object message
    ) {
        ;;
    }

    private record ServerboundWrapper(
            ResourceLocation packetId,
            Object message
    ) {
        ;;
    }

}