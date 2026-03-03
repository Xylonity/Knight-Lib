package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.api.event.impl.server.*;
import dev.xylonity.knightlib.common.event.impl.EntityAttributeRegistrationEventForge;
import dev.xylonity.knightlib.common.event.impl.SpawnPlacementRegistrationEventForge;
import dev.xylonity.knightlib.datagen.KnightLibLootModifierGenerator;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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

        @SubscribeEvent
        public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
            SpawnPlacementRegistrationEventForge spawnEvent = new SpawnPlacementRegistrationEventForge();
            KnightLibEvents.SERVER.dispatch(spawnEvent);

            spawnEvent.applyToForgeEvent(event);
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
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.player.level().isClientSide()) {
                return;
            }

            if (!(event.player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            TickPhase phase = event.phase == TickEvent.Phase.END ? TickPhase.END : TickPhase.START;
            KnightLibEvents.SERVER.dispatch(new ServerPlayerTickEvent(serverPlayer.server, serverPlayer, phase));
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            KnightLibEvents.SERVER.dispatch(new CommandRegistrationEvent(
                    event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()
            ));

        }

        @SubscribeEvent
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide()) {
                return;
            }

            if (event.getLevel() instanceof ServerLevel serverLevel) {
                KnightLibEvents.SERVER.dispatch(new ServerEntityJoinLevelEvent(serverLevel.getServer(), serverLevel, event.getEntity()));
            }

        }

        @SubscribeEvent
        public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
            if (event.getLevel().isClientSide()) {
                return;
            }

            if (event.getLevel() instanceof ServerLevel serverLevel) {
                KnightLibEvents.SERVER.dispatch(new ServerEntityLeaveLevelEvent(serverLevel.getServer(), serverLevel, event.getEntity()));
            }

        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                KnightLibEvents.SERVER.dispatch(new ServerPlayerJoinEvent(serverPlayer.server, serverPlayer));
            }

        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                KnightLibEvents.SERVER.dispatch(new ServerPlayerLeaveEvent(serverPlayer.server, serverPlayer));
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