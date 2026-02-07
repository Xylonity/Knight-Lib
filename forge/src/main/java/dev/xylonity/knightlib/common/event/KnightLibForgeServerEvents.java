package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.api.event.impl.server.EntityAttributeRegistrationEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerTickEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerWorldLoadEvent;
import dev.xylonity.knightlib.api.event.impl.server.ServerWorldUnloadEvent;
import dev.xylonity.knightlib.common.event.impl.EntityAttributeRegistrationEventForge;
import dev.xylonity.knightlib.datagen.KnightLibLootModifierGenerator;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class KnightLibForgeServerEvents {

    @Mod.EventBusSubscriber(modid = KnightLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class KnightLibServerModBus {

        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
            DataGenerator generator = event.getGenerator();
            PackOutput packOutput = generator.getPackOutput();

            generator.addProvider(event.includeServer(), new KnightLibLootModifierGenerator(packOutput));
        }

        @SubscribeEvent
        public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
            EntityAttributeRegistrationEventForge attributeEvent = new EntityAttributeRegistrationEventForge();
            KnightLibEvents.SERVER.dispatch(attributeEvent);

            attributeEvent.applyToForgeEvent(event);
        }

    }

    @Mod.EventBusSubscriber(modid = KnightLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class KnightLibServerForgeBus {

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                KnightLibEvents.SERVER.dispatch(new ServerTickEvent(event.getServer(), TickPhase.START));
            }
            else {
                KnightLibEvents.SERVER.dispatch(new ServerTickEvent(event.getServer(), TickPhase.END));
            }

        }

        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            if (event.getLevel() instanceof ServerLevel level) {
                KnightLibEvents.SERVER.dispatch(new ServerWorldUnloadEvent(level.getServer(), level));
            }

        }

        @SubscribeEvent
        public static void onLevelLoad(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel level) {
                KnightLibEvents.SERVER.dispatch(new ServerWorldLoadEvent(level.getServer(), level));
            }

        }

    }

}