package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.api.event.impl.server.*;
import dev.xylonity.knightlib.api.loot.EntityLootEntry;
import dev.xylonity.knightlib.api.loot.KnightLibLoot;
import dev.xylonity.knightlib.common.event.impl.EntityAttributeRegistrationEventNeoForge;
import dev.xylonity.knightlib.common.event.impl.SpawnPlacementRegistrationEventNeoForge;
import dev.xylonity.knightlib.datagen.KnightLibLootModifierGenerator;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class KnightLibNeoForgeServerEvents {

    @EventBusSubscriber(modid = KnightLib.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class KnightLibServerModBus {

        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
            DataGenerator generator = event.getGenerator();
            PackOutput packOutput = generator.getPackOutput();

            generator.addProvider(event.includeServer(), new KnightLibLootModifierGenerator(packOutput, event.getLookupProvider()));
        }

        @SubscribeEvent
        public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
            EntityAttributeRegistrationEventNeoForge attributeEvent = new EntityAttributeRegistrationEventNeoForge();
            KnightLibEvents.SERVER.dispatch(attributeEvent);

            attributeEvent.applyToForgeEvent(event);
        }

        @SubscribeEvent
        public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
            SpawnPlacementRegistrationEventNeoForge spawnEvent = new SpawnPlacementRegistrationEventNeoForge();
            KnightLibEvents.SERVER.dispatch(spawnEvent);

            spawnEvent.applyToForgeEvent(event);
        }

    }

    @EventBusSubscriber(modid = KnightLib.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static class KnightLibServerForgeBus {

        @SubscribeEvent
        public static void onServerTickPre(ServerTickEvent.Pre event) {
            KnightLibEvents.SERVER.dispatch(new dev.xylonity.knightlib.api.event.impl.server.ServerTickEvent(event.getServer(), TickPhase.START));
        }

        @SubscribeEvent
        public static void onServerTickPost(ServerTickEvent.Post event) {
            KnightLibEvents.SERVER.dispatch(new dev.xylonity.knightlib.api.event.impl.server.ServerTickEvent(event.getServer(), TickPhase.END));
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                KnightLibEvents.SERVER.dispatch(new PlayerChangedDimensionEvent(
                        serverPlayer.server, serverPlayer, event.getFrom(), event.getTo()
                ));
            }

        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            final dev.xylonity.knightlib.api.event.impl.server.LivingDeathEvent deathEvent = new dev.xylonity.knightlib.api.event.impl.server.LivingDeathEvent(event.getEntity(), event.getSource());
            KnightLibEvents.SERVER.dispatch(deathEvent);

            if (deathEvent.isCancelled()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
            dispatchPlayerTick(event, TickPhase.START);
        }

        @SubscribeEvent
        public static void onPlayerTickPost(PlayerTickEvent.Post event) {
            dispatchPlayerTick(event, TickPhase.END);
        }

        private static void dispatchPlayerTick(PlayerTickEvent event, TickPhase phase) {
            if (event.getEntity().level().isClientSide()) {
                return;
            }

            if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            KnightLibEvents.SERVER.dispatch(new ServerPlayerTickEvent(serverPlayer.server, serverPlayer, phase));
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            KnightLibEvents.SERVER.dispatch(new CommandRegistrationEvent(
                    event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()
            ));

        }

        @SubscribeEvent
        public static void onLootTableLoad(LootTableLoadEvent event) {
            final LootTableModifyEvent lootTableModifyEvent = new LootTableModifyEvent(event.getName());
            KnightLibEvents.SERVER.dispatch(lootTableModifyEvent);

            for (LootPool.Builder pool : lootTableModifyEvent.getPendingPools()) {
                event.getTable().addPool(pool.build());
            }

            if (event.getName().getPath().startsWith("entities/")) {
                for (EntityLootEntry entityLootEntry : KnightLibLoot.getEntityEntries()) {
                    event.getTable().addPool(KnightLibLoot.buildPool(entityLootEntry).build());
                }

            }

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

        @SubscribeEvent
        public static void onLivingHurt(LivingIncomingDamageEvent event) {
            final LivingHurtEvent hurtEvent = new LivingHurtEvent(event.getEntity(), event.getSource(), event.getAmount());
            KnightLibEvents.SERVER.dispatch(hurtEvent);

            event.setAmount(hurtEvent.getAmount());
            if (hurtEvent.isCancelled()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLivingUseItemFinish(LivingEntityUseItemEvent.Finish event) {
            final LivingUseItemFinishEvent knightEvent = new LivingUseItemFinishEvent(event.getEntity(), event.getItem(), event.getResultStack());
            KnightLibEvents.SERVER.dispatch(knightEvent);

            event.setResultStack(knightEvent.getResult());
        }

        @SubscribeEvent
        public static void onEntityPlaceBlock(BlockEvent.EntityPlaceEvent event) {
            if (!(event.getLevel() instanceof Level level)) {
                return;
            }

            final EntityPlaceBlockEvent knightEvent = new EntityPlaceBlockEvent(
                    level, event.getPos(), event.getPlacedBlock(), event.getEntity()
            );
            KnightLibEvents.SERVER.dispatch(knightEvent);

            if (knightEvent.isCancelled()) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onPlayerItemCrafted(PlayerEvent.ItemCraftedEvent event) {
            KnightLibEvents.SERVER.dispatch(new PlayerItemCraftedEvent(event.getEntity(), event.getCrafting(), event.getInventory()));
        }

    }

}
