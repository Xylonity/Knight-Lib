package dev.xylonity.knightlib.api.animation.internal;

import java.util.Arrays;
import java.util.List;

/**
 * Animation deltas in specific coordinates, valid per evaluation (doesn't contain any rendering state)
 */
public final class AnimationPose {

    private final List<String> bones;
    final float[] values;

    AnimationPose(List<String> bones) {
        this.bones = List.copyOf(bones);
        this.values = new float[bones.size() * 9];
        reset();
    }

    void reset() {
        Arrays.fill(values, 0f);
        for (int i = 0; i < bones.size(); i++) {
            Arrays.fill(values, i * 9 + 6, i * 9 + 9, 1f);
        }

    }

    public int boneCount() {
        return bones.size();
    }

    public String boneName(int index) {
        return bones.get(index);
    }

    // Position, rotation and scale, in that order (XYZ)
    public float value(int bone, int component) {
        return values[bone * 9 + component];
    }

}
