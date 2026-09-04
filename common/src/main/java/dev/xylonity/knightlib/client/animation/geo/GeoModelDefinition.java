package dev.xylonity.knightlib.client.animation.geo;

import dev.xylonity.knightlib.client.animation.model.GeoCube;

import java.util.List;

/**
 * Immutable point between a geo model file and a live {@code GeoModel}. Pivots, rotations, UVs and cubes are already converted
 * here, leaving model instances free to focus on mutable pose.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/cache/object/BakedGeoModel.java
 */
public record GeoModelDefinition(List<BoneDefinition> bones, List<LocatorDefinition> locators) {

    public GeoModelDefinition {
        bones = List.copyOf(bones);
        locators = List.copyOf(locators);
    }

    public GeoModelDefinition(List<BoneDefinition> bones) {
        this(bones, List.of());
    }

    public record BoneDefinition(
            String name,
            String parent,
            float offsetX, float offsetY, float offsetZ,
            float rotX, float rotY, float rotZ,
            boolean visible,
            List<GeoCube> cubes
    ) {

        public BoneDefinition {
            cubes = List.copyOf(cubes);
        }

    }

    /**
     * Meant for keyframe event anchors (locator) in order to snapshot their position per keyframe
     */
    public record LocatorDefinition(
            String name,
            String bone,
            float offsetX, float offsetY, float offsetZ,
            float rotX, float rotY, float rotZ
    ) {
        ;;
    }

}
