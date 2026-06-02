package dev.xylonity.knightlib.common.spawn.internal;

import com.mojang.serialization.MapCodec;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.spawn.EntityBiomeSpawnEntry;
import dev.xylonity.knightlib.api.spawn.KnightLibEntityBiomeSpawns;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Internal NeoForge BiomeModifier implementation that applies all spawn entries registered through {@link KnightLibEntityBiomeSpawns}
 */
public class KnightLibSpawnBiomeModifier implements BiomeModifier {

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, KnightLib.MOD_ID);

    private static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<? extends BiomeModifier>> SERIALIZER =
            DeferredHolder.create(
                    NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                    KnightLib.of("knightlib_mob_spawns")
            );

    @Override
    public void modify(Holder<Biome> biomeHolder, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) {
            return;
        }

        for (final EntityBiomeSpawnEntry<?> entry : KnightLibEntityBiomeSpawns.getEntries()) {
            if (entry.getBiomeFilter().test(biomeHolder)) {
                builder.getMobSpawnSettings()
                        .getSpawner(entry.getCategory())
                        .add(new MobSpawnSettings.SpawnerData(
                                entry.getEntityType(),
                                entry.getWeight(),
                                entry.getMinCount(),
                                entry.getMaxCount()
                        ));
            }

        }

    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return SERIALIZER.get();
    }

    public static MapCodec<KnightLibSpawnBiomeModifier> makeCodec() {
        return MapCodec.unit(KnightLibSpawnBiomeModifier::new);
    }

    public static void register() {
        BIOME_MODIFIER_SERIALIZERS.register("knightlib_mob_spawns", KnightLibSpawnBiomeModifier::makeCodec);
    }

}
