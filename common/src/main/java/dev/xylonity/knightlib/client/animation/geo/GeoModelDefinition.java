package dev.xylonity.knightlib.client.animation.geo;

import dev.xylonity.knightlib.client.animation.model.GeoCube;

import java.util.List;

/**
 * Immutable parsed {@code .geo.json} geometry. Bone offsets are relative to the parent pivot,
 * already converted to KnightLib's geo model space
 */
public record GeoModelDefinition(List<BoneDefinition> bones) {

    public GeoModelDefinition {
        bones = List.copyOf(bones);
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

}
