package dev.xylonity.knightlib.common.event.impl;

import dev.xylonity.knightlib.api.event.impl.server.SpawnPlacementRegistrationEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;

import java.util.ArrayList;
import java.util.List;

public class SpawnPlacementRegistrationEventNeoForge extends SpawnPlacementRegistrationEvent {

    private final List<Registration<?>> registrations = new ArrayList<>();

    @Override
    public <T extends Mob> void register(EntityType<T> entityType, SpawnPlacements.Type placementType, Heightmap.Types heightmapType, SpawnPlacements.SpawnPredicate<T> predicate) {
        registrations.add(new Registration<>(entityType, placementType, heightmapType, predicate));
    }

    public void applyToForgeEvent(SpawnPlacementRegisterEvent event) {
        for (Registration<?> registration : registrations) {
            registration.apply(event);
        }

    }

    private record Registration<T extends Mob>(
            EntityType<T> entityType,
            SpawnPlacements.Type placementType,
            Heightmap.Types heightmapType,
            SpawnPlacements.SpawnPredicate<T> predicate
    ) {
        void apply(final SpawnPlacementRegisterEvent event) {
            event.register(entityType, placementType, heightmapType, predicate, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }

    }

}