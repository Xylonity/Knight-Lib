package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.PacketType;
import dev.xylonity.knightlib.network.ServerboundPacketType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface KnightLibNetwork {
    <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler);
    <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler);
    <T> void register(ClientboundPacketType<T> type);
    <T> void register(ServerboundPacketType<T> type);

    <T> void sendTo(ServerPlayer player, PacketType<T> type, T msg);
    <T> void sendToServer(T message);
    <T> void sendToAll(MinecraftServer server, PacketType<T> type, T msg);
    <T> void sendToPlayers(Level level, PacketType<T> type, T msg);
    <T> void sendToTracking(Level level, BlockPos pos, PacketType<T> type, T msg);
    <T> void sendToTracking(Entity entity, PacketType<T> type, T msg);
}
