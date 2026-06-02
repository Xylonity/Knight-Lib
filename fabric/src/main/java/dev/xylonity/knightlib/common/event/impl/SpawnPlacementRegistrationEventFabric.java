package dev.xylonity.knightlib.common.event.impl;

import dev.xylonity.knightlib.api.event.impl.server.SpawnPlacementRegistrationEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public class SpawnPlacementRegistrationEventFabric extends SpawnPlacementRegistrationEvent {

    @Override
    public <T extends Mob> void register(EntityType<T> entityType, SpawnPlacementType placementType, Heightmap.Types heightmapType, SpawnPlacements.SpawnPredicate<T> predicate) {
        SpawnPlacements.register(entityType, placementType, heightmapType, predicate);
    }

}