package dev.xylonity.knightlib.client.animation;

import dev.xylonity.knightlib.client.animation.model.GeoModel;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import dev.xylonity.knightlib.client.animation.model.VanillaModel;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Immutable recipe for building a renderer model.
 */
public interface KnightLibModelSource {

    Object cacheKey();

    KnightLibModel create();

    /**
     * Generation of the backing resource or zero for coded models
     */
    default int generation() {
        return 0;
    }

    /**
     * A geo (bedrock format) geometry file
     */
    static KnightLibModelSource geo(ResourceLocation location) {
        Objects.requireNonNull(location, "location");
        return new GeoSource(location);
    }

    /**
     * A coded vanilla model with an explicit stable cache key
     */
    static KnightLibModelSource vanilla(ResourceLocation key, Supplier<LayerDefinition> definition) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(definition, "definition");
        return new VanillaSource(key, definition);
    }

    static KnightLibModelSource vanilla(Supplier<LayerDefinition> definition) {
        Objects.requireNonNull(definition, "definition");
        return new VanillaSource(definition.getClass(), definition);
    }

    record GeoSource(
            ResourceLocation location
    ) implements KnightLibModelSource {

        @Override
        public Object cacheKey() {
            return this;
        }

        @Override
        public KnightLibModel create() {
            return new GeoModel(KnightLibAnimationAssets.getModel(location));
        }

        @Override
        public int generation() {
            return KnightLibAnimationAssets.generation();
        }

    }

    record VanillaSource(Object cacheKey, Supplier<LayerDefinition> definition) implements KnightLibModelSource {

        @Override
        public KnightLibModel create() {
            return new VanillaModel(definition.get().bakeRoot());
        }

    }

    final class InstanceCache {

        private final Map<Object, Entry> models = new HashMap<>();

        public KnightLibModel resolve(KnightLibModelSource source) {
            Objects.requireNonNull(source, "source");

            final int generation = source.generation();
            Entry entry = models.get(source.cacheKey());
            if (entry == null || entry.generation != generation) {
                entry = new Entry(generation, source.create());
                models.put(source.cacheKey(), entry);
            }

            return entry.model;
        }

        public void clear() {
            models.clear();
        }

        private record Entry(
                int generation,
                KnightLibModel model
        ) {
            ;;
        }

    }

}