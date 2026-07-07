package dev.xylonity.knightlib.api.camera.path;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPath;
import dev.xylonity.knightlib.network.packets.CameraPathS2C;
import net.minecraft.server.level.ServerPlayer;

/**
 * Entrypoint to play cutscenes for specific players.
 * <br>
 * The usage is simple, {@code CameraPaths.play(player, path); } from any server logic (a boss spawning,
 * a player crossing a door, a structure trigger, etc.)
 */
public final class CameraPaths {

    private CameraPaths() {
        ;;
    }

    /**
     * Plays a path on the player's client, replacing any active one
     */
    public static void play(ServerPlayer player, CameraPath path) {
        if (player == null || path == null) {
            return;
        }

        KnightLib.NET.sendTo(player, CameraPathS2C.TYPE.base(), CameraPathS2C.play(path));
    }

    /**
     * Stops whatever path is playing on the player's client, if any
     */
    public static void stop(ServerPlayer player) {
        if (player == null) {
            return;
        }

        KnightLib.NET.sendTo(player, CameraPathS2C.TYPE.base(), CameraPathS2C.stop());
    }

}