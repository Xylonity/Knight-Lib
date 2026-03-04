package dev.xylonity.knightlib.common.spawn.internal;

import com.mojang.serialization.Codec;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.spawn.KnightLibEntityBiomeSpawns;
import dev.xylonity.knightlib.api.spawn.EntityBiomeSpawnEntry;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Internal Forge BiomeModifier implementation that applies all spawn entries registered through {@link KnightLibEntityBiomeSpawns}
 */
public class KnightLibSpawnBiomeModifier implements BiomeModifier {

    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, KnightLib.MOD_ID);

    private static final RegistryObject<Codec<? extends BiomeModifier>> SERIALIZER =
            RegistryObject.create(
                    KnightLib.of("knightlib_mob_spawns"),
                    ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                    KnightLib.MOD_ID
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
    public Codec<? extends BiomeModifier> codec() {
        return SERIALIZER.get();
    }

    public static Codec<KnightLibSpawnBiomeModifier> makeCodec() {
        return Codec.unit(KnightLibSpawnBiomeModifier::new);
    }

    public static void register() {
        BIOME_MODIFIER_SERIALIZERS.register("knightlib_mob_spawns", KnightLibSpawnBiomeModifier::makeCodec);
    }

}