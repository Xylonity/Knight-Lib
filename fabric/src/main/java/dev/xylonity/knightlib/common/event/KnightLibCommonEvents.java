package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.api.TickScheduler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class KnightLibCommonEvents {

    public static void init() {

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TickScheduler.clean();

            for (ServerLevel level : server.getAllLevels()) {
                TickScheduler.incrementTick(level);
                TickScheduler.processServerTasks(level);
                TickScheduler.processCommonTasks(level);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Level level = client.level;
            if (level == null) return;

            TickScheduler.clean();
            TickScheduler.incrementTick(level);
            TickScheduler.processClientTasks(level);
            TickScheduler.processCommonTasks(level);
        });

        ServerWorldEvents.UNLOAD.register((server, world) -> TickScheduler.markForClean(world));
    }

}