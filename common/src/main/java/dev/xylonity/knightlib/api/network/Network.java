package dev.xylonity.knightlib.api.network;

import dev.xylonity.knightlib.KnightLib;
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

public class Network {

    public static final String PROTOCOL = "1";

    public static <T> void registerClientbound(PacketType<T> type, Consumer<T> clientHandler) {
        KnightLib.NETWORK.registerClientbound(type, clientHandler);
    }

    public static <T> void registerServerbound(PacketType<T> type, BiConsumer<T, ServerPlayer> serverHandler) {
        KnightLib.NETWORK.registerServerbound(type, serverHandler);
    }

    public static <T> void register(ClientboundPacketType<T> type) {
        KnightLib.NETWORK.register(type);
    }

    public static <T> void register(ServerboundPacketType<T> type) {
        KnightLib.NETWORK.register(type);
    }

    public static <T> void sendToServer(T msg) {
        KnightLib.NETWORK.sendToServer(msg);
    }

    public static <T> void sendTo(ServerPlayer player, PacketType<T> type, T msg) {
        KnightLib.NETWORK.sendTo(player, type, msg);
    }

    public static <T> void sendToAll(MinecraftServer server, PacketType<T> type, T msg) {
        KnightLib.NETWORK.sendToAll(server, type, msg);
    }

    public static <T> void sendToPlayers(Level level, PacketType<T> type, T msg) {
        KnightLib.NETWORK.sendToPlayers(level, type, msg);
    }

    public static <T> void sendToTracking(Level level, BlockPos pos, PacketType<T> type, T msg) {
        KnightLib.NETWORK.sendToTracking(level, pos, type, msg);
    }

    public static <T> void sendToTracking(Entity entity, PacketType<T> type, T msg) {
        KnightLib.NETWORK.sendToTracking(entity, type, msg);
    }

}
