package dev.xylonity.knightlib.common.event.impl;

import dev.xylonity.knightlib.api.event.impl.server.SpawnPlacementRegistrationEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.ArrayList;
import java.util.List;

public class SpawnPlacementRegistrationEventNeoForge extends SpawnPlacementRegistrationEvent {

    private final List<Registration<?>> registrations = new ArrayList<>();

    @Override
    public <T extends Mob> void register(EntityType<T> entityType, SpawnPlacementType placementType, Heightmap.Types heightmapType, SpawnPlacements.SpawnPredicate<T> predicate) {
        registrations.add(new Registration<>(entityType, placementType, heightmapType, predicate));
    }

    public void applyToForgeEvent(RegisterSpawnPlacementsEvent event) {
        for (Registration<?> registration : registrations) {
            registration.apply(event);
        }

    }

    private record Registration<T extends Mob>(
            EntityType<T> entityType,
            SpawnPlacementType placementType,
            Heightmap.Types heightmapType,
            SpawnPlacements.SpawnPredicate<T> predicate
    ) {
        void apply(final RegisterSpawnPlacementsEvent event) {
            event.register(entityType, placementType, heightmapType, predicate, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }

    }

}
