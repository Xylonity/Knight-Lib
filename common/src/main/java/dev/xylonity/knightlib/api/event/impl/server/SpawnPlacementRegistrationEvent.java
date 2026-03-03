package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Event to register custom spawn placement rules for entities
 */
public abstract class SpawnPlacementRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

    public abstract <T extends Mob> void register(
            EntityType<T> entityType,
            SpawnPlacements.Type placementType,
            Heightmap.Types heightmapType,
            SpawnPlacements.SpawnPredicate<T> predicate
    );

}