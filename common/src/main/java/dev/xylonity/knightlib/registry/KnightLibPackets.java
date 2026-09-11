package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.network.packets.AnimationSyncS2C;
import dev.xylonity.knightlib.network.packets.AttachmentSyncS2C;
import dev.xylonity.knightlib.network.packets.BoneHitboxAttackC2S;
import dev.xylonity.knightlib.network.packets.BossBarLinkS2C;
import dev.xylonity.knightlib.network.packets.CameraPathS2C;
import dev.xylonity.knightlib.network.packets.CameraShakeS2C;
import dev.xylonity.knightlib.network.packets.PersistentSoundTickS2C;

public class KnightLibPackets {

    public static void register() {
        KnightLib.NET.register(BossBarLinkS2C.TYPE);
        KnightLib.NET.register(CameraShakeS2C.TYPE);
        KnightLib.NET.register(PersistentSoundTickS2C.TYPE);
        KnightLib.NET.register(AttachmentSyncS2C.TYPE);
        KnightLib.NET.register(CameraPathS2C.TYPE);
        KnightLib.NET.register(AnimationSyncS2C.TYPE);
        KnightLib.NET.register(BoneHitboxAttackC2S.TYPE);
    }

}
