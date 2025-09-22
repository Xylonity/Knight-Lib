package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.api.network.Network;
import dev.xylonity.knightlib.network.packets.BossBarLinkS2C;

public class KnightLibPackets {

    public static void register() {
        registerS2C();
        registerC2S();
    }

    public static void registerS2C() {
        Network.register(BossBarLinkS2C.TYPE);
    }

    public static void registerC2S() {

    }

}
