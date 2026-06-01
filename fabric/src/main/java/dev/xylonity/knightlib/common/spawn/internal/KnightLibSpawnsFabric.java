package dev.xylonity.knightlib.common.spawn.internal;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.spawn.EntityBiomeSpawnEntry;
import dev.xylonity.knightlib.api.spawn.KnightLibEntityBiomeSpawns;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * Internal entity biome spawn implementation that registers a single deferred Fabric BiomeModification through {@link KnightLibEntityBiomeSpawns}
 */
public final class KnightLibSpawnsFabric {

    private KnightLibSpawnsFabric() {
        ;;
    }

    /**
     * Registers a single BiomeModification
     */
    public static void register() {
        BiomeModifications.create(KnightLib.of("knightlib_mob_spawns"))
                .add(ModificationPhase.ADDITIONS, KnightLibSpawnsFabric::acceptAll, KnightLibSpawnsFabric::modifyAll);
    }

    private static boolean acceptAll(BiomeSelectionContext context) {
        for (final EntityBiomeSpawnEntry<?> entry : KnightLibEntityBiomeSpawns.getEntries()) {
            if (entry.getBiomeFilter().test(context.getBiomeRegistryEntry())) {
                return true;
            }

        }

        return false;
    }

    private static void modifyAll(BiomeSelectionContext selectionContext, BiomeModificationContext modificationContext) {
        final BiomeModificationContext.SpawnSettingsContext spawnSettings = modificationContext.getSpawnSettings();

        for (final EntityBiomeSpawnEntry<?> entry : KnightLibEntityBiomeSpawns.getEntries()) {
            if (entry.getBiomeFilter().test(selectionContext.getBiomeRegistryEntry())) {
                spawnSettings.addSpawn(
                        entry.getCategory(),
                        new MobSpawnSettings.SpawnerData(
                                entry.getEntityType(),
                                entry.getWeight(),
                                entry.getMinCount(),
                                entry.getMaxCount()
                        )
                );

            }

        }

        KnightLibEntityBiomeSpawns.freeze();
    }

}