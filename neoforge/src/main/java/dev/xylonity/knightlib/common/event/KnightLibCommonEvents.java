package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.TickScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = KnightLib.MOD_ID)
public class KnightLibCommonEvents {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onServerTick(ServerTickEvent event) {
        TickScheduler.clean();

        for (ServerLevel level : event.getServer().getAllLevels()) {
            TickScheduler.incrementTick(level);
            TickScheduler.processServerTasks(level);
            TickScheduler.processCommonTasks(level);
        }

    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            TickScheduler.markForClean(level);
        }

    }

}