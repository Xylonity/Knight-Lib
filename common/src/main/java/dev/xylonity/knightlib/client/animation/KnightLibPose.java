package dev.xylonity.knightlib.client.animation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Absolute per-bone transform snapshot in the owning backend's space. Layout per bone:
 * [x, y, z, xRot, yRot, zRot, xScale, yScale, zScale].
 */
public final class KnightLibPose {

    private final Map<String, float[]> bones = new HashMap<>();

    public float[] bone(String name) {
        return bones.computeIfAbsent(name, ignored -> new float[9]);
    }

    public float[] get(String name) {
        return bones.get(name);
    }

    public Set<String> boneNames() {
        return Collections.unmodifiableSet(bones.keySet());
    }

    /**
     * Drops every bone not in {@code names}, limiting blends to the bones a controller touches
     */
    public void retain(Set<String> names) {
        bones.keySet().retainAll(names);
    }

}
