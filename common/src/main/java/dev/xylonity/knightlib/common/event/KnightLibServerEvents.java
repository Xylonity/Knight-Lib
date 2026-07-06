package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.api.event.RegisterEvent;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.api.event.impl.server.PlayerChangedDimensionEvent;
import dev.xylonity.knightlib.api.event.impl.server.PlayerStartTrackingEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerPlayerCloneEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerPlayerJoinEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerPlayerRespawnEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerTickEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerWorldUnloadEvent;
import dev.xylonity.knightlib.api.scheduler.TickScheduler;
import dev.xylonity.knightlib.common.entity.data.internal.AttachmentsInternal;
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

    @RegisterEvent
    public static void onStartTracking(final PlayerStartTrackingEvent event) {
        AttachmentsInternal.syncAllTo(event.getTarget(), event.getPlayer());
    }

    @RegisterEvent
    public static void onPlayerJoin(final ServerPlayerJoinEvent event) {
        AttachmentsInternal.syncAllTo(event.getPlayer(), event.getPlayer());
    }

    @RegisterEvent
    public static void onPlayerRespawn(final ServerPlayerRespawnEvent event) {
        AttachmentsInternal.syncAllTo(event.getPlayer(), event.getPlayer());
    }

    @RegisterEvent
    public static void onPlayerChangedDimension(final PlayerChangedDimensionEvent event) {
        AttachmentsInternal.syncAllTo(event.getPlayer(), event.getPlayer());
    }

    @RegisterEvent
    public static void onPlayerClone(final ServerPlayerCloneEvent event) {
        AttachmentsInternal.copyForRespawn(event.getOldPlayer(), event.getNewPlayer(), event.wasDeath());
    }

}
