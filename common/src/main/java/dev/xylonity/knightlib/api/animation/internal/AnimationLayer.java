package dev.xylonity.knightlib.api.animation.internal;

import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.BitSet;

/**
 * A controller's operation on the pose below it (output = input * factor + offset). This representation can blend additive,
 * override and stopped controllers without baking in the lower controllers
 */
final class AnimationLayer {

    final float[] factors;
    final float[] offsets;
    final BitSet bones = new BitSet();

    AnimationLayer(int count) {
        factors = new float[count * 9];
        offsets = new float[count * 9];
        reset();
    }

    void reset() {
        Arrays.fill(factors, 1f);
        Arrays.fill(offsets, 0f);
        bones.clear();
    }

    void copyFrom(AnimationLayer other) {
        System.arraycopy(other.factors, 0, factors, 0, factors.length);
        System.arraycopy(other.offsets, 0, offsets, 0, offsets.length);
        bones.clear();
        bones.or(other.bones);
    }

    void override(int bone, int channels) {
        bones.set(bone);
        for (int channel = 0; channel < 3; channel++) {
            if ((channels & (1 << channel)) != 0) {
                final int at = bone * 9 + channel * 3;
                Arrays.fill(factors, at, at + 3, 0f);
                Arrays.fill(offsets, at, at + 3, channel == 2 ? 1f : 0f);
            }

        }

    }

    void weight(float weight) {
        if (weight == 1f) {
            return;
        }
        if (weight == 0f) {
            reset();
            return;
        }

        for (int bone = bones.nextSetBit(0); bone >= 0; bone = bones.nextSetBit(bone + 1)) {
            for (int component = 0; component < 9; component++) {
                final int index = bone * 9 + component;
                factors[index] = Mth.lerp(weight, 1f, factors[index]);
                offsets[index] *= weight;
            }

        }

    }

    void channel(int bone, int component, float x, float y, float z, boolean override) {
        bones.set(bone);
        final float[] values = component == 6 && !override ? factors : offsets;
        final int at = bone * 9 + component;
        values[at] = x;
        values[at + 1] = y;
        values[at + 2] = z;
    }

    void blendFrom(AnimationLayer from, float weight, AnimationPose below) {
        bones.or(from.bones);
        for (int bone = bones.nextSetBit(0); bone >= 0; bone = bones.nextSetBit(bone + 1)) {
            for (int component = 0; component < 9; component++) {
                final int index = bone * 9 + component;
                final float factor = Mth.lerp(weight, from.factors[index], factors[index]);
                if (component >= 3 && component < 6) {
                    final float lower = below.values[index];
                    final float previous = lower * from.factors[index] + from.offsets[index];
                    final float current = lower * factors[index] + offsets[index];
                    offsets[index] = Mth.rotLerp(weight, previous, current) - lower * factor;
                }
                else {
                    offsets[index] = Mth.lerp(weight, from.offsets[index], offsets[index]);
                }

                factors[index] = factor;
            }

        }

    }

    void compose(AnimationPose pose) {
        for (int bone = bones.nextSetBit(0); bone >= 0; bone = bones.nextSetBit(bone + 1)) {
            for (int component = 0; component < 9; component++) {
                final int index = bone * 9 + component;
                final float value = pose.values[index] * factors[index] + offsets[index];
                if (Float.isFinite(value)) {
                    pose.values[index] = value;
                }

            }

        }

    }

}