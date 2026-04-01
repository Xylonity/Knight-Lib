package dev.xylonity.knightlib.api.entity.spawn;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;

/**
 * Quick wrapper for managing config entries that define an entity's spawn weight and biomes, in a hardcoded string as follows:
 *
 * {@code "weight, minAmount, maxAmount, biomes, and tags..."}
 */
public class SpawnConfig {

    public final List<ResourceLocation> biomes;
    public final List<TagKey<Biome>> biomeTags;
    public final int weight;
    public final int minCount;
    public final int maxCount;

    private SpawnConfig(int weight, int minCount, int maxCount, List<ResourceLocation> biomes, List<TagKey<Biome>> biomeTags) {
        this.weight = weight;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.biomes = biomes;
        this.biomeTags = biomeTags;
    }

    public static SpawnConfig parse(String configLine) {
        final String[] parts = configLine.split("\\s*,\\s*");
        final int weight = Integer.parseInt(parts[0]);
        final int min = Integer.parseInt(parts[1]);
        final int max = Integer.parseInt(parts[2]);

        final List<ResourceLocation> biomeList = new ArrayList<>();
        final List<TagKey<Biome>> tagList = new ArrayList<>();

        for (int i = 3; i < parts.length; i++) {
            final String part = parts[i];
            if (part.startsWith("#")) {
                final ResourceLocation tagId = new ResourceLocation(part.substring(1));
                tagList.add(TagKey.create(Registries.BIOME, tagId));
            }
            else {
                biomeList.add(new ResourceLocation(part));
            }

        }

        return new SpawnConfig(weight, min, max, biomeList, tagList);
    }

    public boolean matches(Holder<Biome> biomeHolder) {
        final ResourceLocation biomeName = getBiomeName(biomeHolder);
        if (biomeName == null) {
            return false;
        }

        if (biomes.contains(biomeName)) {
            return true;
        }

        for (final TagKey<Biome> tag : biomeTags) {
            if (biomeHolder.is(tag)) {
                return true;
            }

        }

        return false;
    }

    private static ResourceLocation getBiomeName(Holder<Biome> biomeHolder) {
        return biomeHolder.unwrap().map(ResourceKey::location, noKey -> null);
    }

}
