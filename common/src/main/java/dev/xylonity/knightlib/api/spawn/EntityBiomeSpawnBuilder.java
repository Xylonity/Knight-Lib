package dev.xylonity.knightlib.api.spawn;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Builder for constructing {@link EntityBiomeSpawnEntry} instances.
 *
 * <pre>
 * {@code
 * KnightLibSpawns.builder(() -> TestingEntities.YEAH.get(), MobCategory.CREATURE)
 *     .spawnRate(15, 1, 3)
 *     .biomeFilter(biome -> biome.is(BiomeTags.IS_FOREST))
 *     .submit();
 * }</pre>
 *
 * Spawn placement rules are registered separately via {@link dev.xylonity.knightlib.api.event.impl.server.SpawnPlacementRegistrationEvent}
 */
public final class EntityBiomeSpawnBuilder<T extends Mob> {

    private Predicate<Holder<Biome>> biomeFilter = biome -> true;
    private final Supplier<EntityType<T>> entityType;
    private final MobCategory category;

    private int weight = 10;
    private int minCount = 1;
    private int maxCount = 1;

    EntityBiomeSpawnBuilder(Supplier<EntityType<T>> entityType, MobCategory category) {
        this.entityType = entityType;
        this.category = category;
    }

    /**
     * Sets the spawn weight and group size range
     */
    public EntityBiomeSpawnBuilder<T> spawnRate(int weight, int minCount, int maxCount) {
        this.weight = weight;
        this.minCount = minCount;
        this.maxCount = maxCount;
        return this;
    }

    /**
     * Sets the predicate that determines which biomes this entity can spawn in
     */
    public EntityBiomeSpawnBuilder<T> biomeFilter(Predicate<Holder<Biome>> biomeFilter) {
        this.biomeFilter = biomeFilter;
        return this;
    }

    /**
     * Finalizes and registers this spawn entry with KnightLib.
     * MUST be called during mod initialization.
     */
    public EntityBiomeSpawnEntry<T> submit() {
        final EntityBiomeSpawnEntry<T> entry = new EntityBiomeSpawnEntry<>(
                entityType,
                category,
                weight,
                minCount,
                maxCount,
                biomeFilter
        );

        KnightLibEntityBiomeSpawns.addEntry(entry);

        return entry;
    }

}