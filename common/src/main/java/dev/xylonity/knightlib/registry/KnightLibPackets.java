package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.network.packets.BoneHitboxSyncC2S;
import dev.xylonity.knightlib.network.packets.BossBarLinkS2C;
import dev.xylonity.knightlib.network.packets.CameraShakeS2C;
import dev.xylonity.knightlib.network.packets.PersistentSoundTickS2C;

public class KnightLibPackets {

    public static void registerAll() {
        registerS2C();
        registerC2S();
    }

    public static void registerS2C() {
        KnightLib.NET.register(BossBarLinkS2C.TYPE);
        KnightLib.NET.register(CameraShakeS2C.TYPE);
        KnightLib.NET.register(PersistentSoundTickS2C.TYPE);
    }

    public static void registerC2S() {
        KnightLib.NET.register(BoneHitboxSyncC2S.TYPE);
    }

}
