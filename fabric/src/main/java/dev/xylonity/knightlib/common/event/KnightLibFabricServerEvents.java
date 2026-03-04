package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.interop.TickPhase;
import dev.xylonity.knightlib.api.event.impl.server.*;
import dev.xylonity.knightlib.api.loot.EntityLootEntry;
import dev.xylonity.knightlib.api.loot.KnightLibLoot;
import dev.xylonity.knightlib.common.event.impl.EntityAttributeRegistrationEventFabric;
import dev.xylonity.knightlib.common.event.impl.SpawnPlacementRegistrationEventFabric;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootPool;

public final class KnightLibFabricServerEvents {

    public static void init() {
        ServerTickEvents.START_SERVER_TICK.register(server ->
                KnightLibEvents.SERVER.dispatch(new ServerTickEvent(server, TickPhase.START))
        );

        ServerTickEvents.END_SERVER_TICK.register(server ->
                KnightLibEvents.SERVER.dispatch(new ServerTickEvent(server, TickPhase.END))
        );

        ServerWorldEvents.UNLOAD.register((server, level) ->
                KnightLibEvents.SERVER.dispatch(new ServerWorldUnloadEvent(server, level))
        );

        ServerWorldEvents.LOAD.register((server, level) ->
                KnightLibEvents.SERVER.dispatch(new ServerWorldLoadEvent(server, level))
        );

        EntityAttributeRegistrationEventFabric attributeEvent = new EntityAttributeRegistrationEventFabric();
        KnightLibEvents.SERVER.dispatch(attributeEvent);

        SpawnPlacementRegistrationEventFabric spawnEvent = new SpawnPlacementRegistrationEventFabric();
        KnightLibEvents.SERVER.dispatch(spawnEvent);

        onServerPlayerTickEvents();
        onCommandRegistrationEvents();
        onEntityLevelEvents();
        onPlayerConnectionEvents();
        onLootTableModificationEvent();
    }

    private static void onServerPlayerTickEvents() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                KnightLibEvents.SERVER.dispatch(new ServerPlayerTickEvent(server, player, TickPhase.START));
            }

        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                KnightLibEvents.SERVER.dispatch(new ServerPlayerTickEvent(server, player, TickPhase.END));
            }

        });

    }

    private static void onCommandRegistrationEvents() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            KnightLibEvents.SERVER.dispatch(new CommandRegistrationEvent(dispatcher, registryAccess, environment));
        });

    }

    private static void onEntityLevelEvents() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            KnightLibEvents.SERVER.dispatch(new ServerEntityJoinLevelEvent(level.getServer(), level, entity));
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            KnightLibEvents.SERVER.dispatch(new ServerEntityLeaveLevelEvent(level.getServer(), level, entity));
        });

    }

    private static void onPlayerConnectionEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            KnightLibEvents.SERVER.dispatch(new ServerPlayerJoinEvent(server, player));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            KnightLibEvents.SERVER.dispatch(new ServerPlayerLeaveEvent(server, player));
        });

    }

    private static void onLootTableModificationEvent() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            final LootTableModifyEvent event = new LootTableModifyEvent(id);
            KnightLibEvents.SERVER.dispatch(event);

            for (LootPool.Builder pool : event.getPendingPools()) {
                tableBuilder.withPool(pool);
            }

            if (id.getPath().startsWith("entities/")) {
                for (EntityLootEntry entityLootEntry : KnightLibLoot.getEntityEntries()) {
                    tableBuilder.withPool(KnightLibLoot.buildPool(entityLootEntry));
                }

            }

        });

    }

}