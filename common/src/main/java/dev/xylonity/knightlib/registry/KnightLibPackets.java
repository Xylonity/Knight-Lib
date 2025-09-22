package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.api.network.Network;
import dev.xylonity.knightlib.network.packets.BossBarLinkS2C;

public class KnightLibPackets {

    public static void init() {
        Network.register(BossBarLinkS2C.TYPE);
    }

}
