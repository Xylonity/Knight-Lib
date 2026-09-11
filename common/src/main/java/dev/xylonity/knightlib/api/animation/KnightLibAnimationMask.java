package dev.xylonity.knightlib.api.animation;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable selection of exact bone names and transform channels (bone children are not selected). An empty selection affects no bones.
 */
public record KnightLibAnimationMask(Set<String> boneNames, int channelBits, boolean allBones) {

    public static final int MAX_BONES = 256;
    public static final KnightLibAnimationMask ALL = new KnightLibAnimationMask(Set.of(), 7, true);

    public KnightLibAnimationMask {
        Objects.requireNonNull(boneNames, "boneNames");
        if (boneNames.size() > MAX_BONES || (channelBits & ~7) != 0) {
            throw new IllegalArgumentException("[KnightLib] Invalid animation mask size or channels");
        }

        for (final String bone : boneNames) {
            if (bone == null || bone.isBlank() || bone.length() > 256) {
                throw new IllegalArgumentException("[KnightLib] Mask bone names must contain 1..256 characters");
            }

        }

        boneNames = allBones ? Set.of() : Set.copyOf(boneNames);
    }

    public static KnightLibAnimationMask bones(String... names) {
        return new KnightLibAnimationMask(Set.copyOf(java.util.Arrays.asList(names)), 7, false);
    }

    /**
     * Keeps this bone selection and replaces its allowed channels
     */
    public KnightLibAnimationMask channels(Channel... channels) {
        int bits = 0;
        for (final Channel channel : channels) {
            bits |= Objects.requireNonNull(channel, "channel").bit;
        }

        return new KnightLibAnimationMask(boneNames, bits, allBones);
    }

    public boolean includesBone(String name) {
        return allBones || boneNames.contains(name);
    }

    public boolean includesChannel(Channel channel) {
        return (channelBits & channel.bit) != 0;
    }

    public enum Channel {
        POSITION(1),
        ROTATION(2),
        SCALE(4);

        private final int bit;

        Channel(int bit) {
            this.bit = bit;
        }

    }

}
