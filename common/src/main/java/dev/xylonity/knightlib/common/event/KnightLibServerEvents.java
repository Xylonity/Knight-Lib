package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.api.event.RegisterEvent;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.api.event.impl.server.ServerTickEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerWorldUnloadEvent;
import dev.xylonity.knightlib.api.scheduler.TickScheduler;
import net.minecraft.server.level.ServerLevel;

public class KnightLibServerEvents {

    @RegisterEvent
    public static void onServerTick(final ServerTickEvent event) {
        if (event.phase() != TickPhase.END) {
            return;
        }

        TickScheduler.clean();

        for (ServerLevel level : event.server().getAllLevels()) {
            TickScheduler.incrementTick(level);
            TickScheduler.processServerTasks(level);
            TickScheduler.processCommonTasks(level);
        }
    }

    @RegisterEvent
    public static void onWorldUnload(final ServerWorldUnloadEvent event) {
        TickScheduler.markForClean(event.level());
    }

}
