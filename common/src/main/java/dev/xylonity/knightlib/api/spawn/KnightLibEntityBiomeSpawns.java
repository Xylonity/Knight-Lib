package dev.xylonity.knightlib.api.spawn;

import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Entity per-biome spawn registration API.
 * <br><br>
 * The registration process is deferred to {@link EntityBiomeSpawnBuilder}
 */
public final class KnightLibEntityBiomeSpawns {

    private static final List<EntityBiomeSpawnEntry<?>> ENTRIES = new ArrayList<>();
    private static volatile boolean frozen = false;

    private KnightLibEntityBiomeSpawns() {
        ;;
    }

    /**
     * Creates a builder for a new spawn entry.
     *
     * @param entityType the entity in this context
     * @param category the category for spawning
     */
    public static <T extends Mob> EntityBiomeSpawnBuilder<T> builder(Supplier<EntityType<T>> entityType, MobCategory category) {
        return new EntityBiomeSpawnBuilder<>(entityType, category);
    }

    /**
     * Creates a builder for a new spawn entry using a {@link ResourceEntry} (just in case the compiler complains).
     *
     * @param entityEntry the entity in this context
     * @param category the category for spawning
     */
    public static <T extends Mob> EntityBiomeSpawnBuilder<T> builder(ResourceEntry<EntityType<T>> entityEntry, MobCategory category) {
        return new EntityBiomeSpawnBuilder<>(entityEntry, category);
    }

    /**
     * Adds a finalized entry to the registry. Internal
     */
    static <T extends Mob> void addEntry(EntityBiomeSpawnEntry<T> entry) {
        if (frozen) {
            throw new IllegalStateException(
                    "[KnightLib] Cannot register entity biome spawns after the registry has been frozen."
            );

        }

        ENTRIES.add(entry);
    }

    /**
     * Returns all registered spawn entries. Internal
     */
    public static List<EntityBiomeSpawnEntry<?>> getEntries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    /**
     * Prevents further entries from being registered (mainly for fabric, just in case). Internal
     */
    public static void freeze() {
        frozen = true;
    }

}