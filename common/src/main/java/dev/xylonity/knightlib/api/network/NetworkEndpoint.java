package dev.xylonity.knightlib.api.network;

import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import dev.xylonity.knightlib.platform.KnightLibNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class NetworkEndpoint {

    private final KnightLibNetwork network;

    public NetworkEndpoint(KnightLibNetwork backend) {
        this.network = backend;
    }

    public <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler) {
        network.registerClientbound(type, clientHandler);
    }

    public <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler) {
        network.registerServerbound(type, serverHandler);
    }

    public <T> void register(ClientboundPacketType<T> type) {
        network.register(type);
    }

    public <T> void register(ServerboundPacketType<T> type) {
        network.register(type);
    }

    public <T> void sendToServer(T msg) {
        network.sendToServer(msg);
    }

    public <T> void sendTo(ServerPlayer player, PacketType<T> type, T msg) {
        network.sendTo(player, type, msg);
    }

    public <T> void sendToAll(MinecraftServer server, PacketType<T> type, T msg) {
        network.sendToAll(server, type, msg);
    }

    public <T> void sendToPlayers(Level level, PacketType<T> type, T msg) {
        network.sendToPlayers(level, type, msg);
    }

    public <T> void sendToTracking(Level level, BlockPos pos, PacketType<T> type, T msg) {
        network.sendToTracking(level, pos, type, msg);
    }

    public <T> void sendToTracking(Entity entity, PacketType<T> type, T msg) {
        network.sendToTracking(entity, type, msg);
    }

}
