package dev.xylonity.knightlib.api.spawn;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Immutable descriptor for a mob biome spawn entry.
 * <br><br>
 * Spawn placement rules are registered separately via {@link dev.xylonity.knightlib.api.event.impl.server.SpawnPlacementRegistrationEvent}
 */
public final class EntityBiomeSpawnEntry<T extends Mob> {

    private final Predicate<Holder<Biome>> biomeFilter;
    private final Supplier<EntityType<T>> entityType;
    private final MobCategory category;

    private final int weight;
    private final int minCount;
    private final int maxCount;

    public EntityBiomeSpawnEntry(
            Supplier<EntityType<T>> entityType,
            MobCategory category,
            int weight,
            int minCount,
            int maxCount,
            Predicate<Holder<Biome>> biomeFilter
    ) {
        this.entityType = entityType;
        this.category = category;
        this.weight = weight;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.biomeFilter = biomeFilter;
    }

    public EntityType<T> getEntityType() {
        return entityType.get();
    }

    public Supplier<EntityType<T>> getEntityTypeSupplier() {
        return entityType;
    }

    public MobCategory getCategory() {
        return category;
    }

    public int getWeight() {
        return weight;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public Predicate<Holder<Biome>> getBiomeFilter() {
        return biomeFilter;
    }

}