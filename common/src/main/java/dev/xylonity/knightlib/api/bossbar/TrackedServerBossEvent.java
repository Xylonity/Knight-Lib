package dev.xylonity.knightlib.api.bossbar;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.network.packets.BossBarLinkS2C;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Custom implementation of `ServerBossEvent` that stores the actual entity.
 */
public class TrackedServerBossEvent extends ServerBossEvent {

    private final Entity owner;

    public TrackedServerBossEvent(Entity owner, Component name, BossBarColor color, BossBarOverlay overlay) {
        super(name, color, overlay);
        this.owner = owner;
    }

    @Override
    public void addPlayer(ServerPlayer player) {
        super.addPlayer(player);

        BossBarLinkS2C packet = new BossBarLinkS2C(
                this.getId(),
                owner.getId(),
                owner.getUUID(),
                owner.getType().builtInRegistryHolder().key().location(),
                owner.level().dimension().location()
        );

        KnightLib.NET.sendTo(player, BossBarLinkS2C.TYPE.base(), packet);
    }

}
