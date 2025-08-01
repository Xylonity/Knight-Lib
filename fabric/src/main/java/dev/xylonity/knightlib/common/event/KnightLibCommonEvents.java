package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.api.scheduler.TickScheduler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.level.ServerLevel;

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

        ServerWorldEvents.UNLOAD.register((server, world) -> TickScheduler.markForClean(world));
    }

}