package dev.xylonity.knightlib.client.animation;

import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Converts a vanilla code {@link AnimationDefinition} into KnightLib's internal representation.
 */
public final class VanillaAnimationAdapter {

    public static KnightLibAnimation convert(String name, AnimationDefinition definition) {
        return convert(name, definition, null);
    }

    /**
     * Converts a definition, optionally overriding the playback mode inferred from vanilla
     */
    public static KnightLibAnimation convert(String name, AnimationDefinition definition, KnightLibAnimation.LoopMode loopOverride) {
        final Map<String, KnightLibAnimation.Channels> bones = new LinkedHashMap<>();

        for (final Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
            List<KnightLibAnimation.Keyframe> position = null;
            List<KnightLibAnimation.Keyframe> rotation = null;
            List<KnightLibAnimation.Keyframe> scale = null;

            final List<List<KnightLibAnimation.Keyframe>> additionalPositions = new ArrayList<>();
            final List<List<KnightLibAnimation.Keyframe>> additionalRotations = new ArrayList<>();
            final List<List<KnightLibAnimation.Keyframe>> additionalScales = new ArrayList<>();

            for (final AnimationChannel channel : entry.getValue()) {
                if (channel.target() == AnimationChannel.Targets.POSITION) {
                    final List<KnightLibAnimation.Keyframe> converted = convertKeyframes(channel, Kind.POSITION);
                    if (position == null) {
                        position = converted;
                    }
                    else {
                        additionalPositions.add(converted);
                    }

                }
                else if (channel.target() == AnimationChannel.Targets.ROTATION) {
                    final List<KnightLibAnimation.Keyframe> converted = convertKeyframes(channel, Kind.ROTATION);
                    if (rotation == null) {
                        rotation = converted;
                    }
                    else {
                        additionalRotations.add(converted);
                    }

                }
                else if (channel.target() == AnimationChannel.Targets.SCALE) {
                    final List<KnightLibAnimation.Keyframe> converted = convertKeyframes(channel, Kind.SCALE);
                    if (scale == null) {
                        scale = converted;
                    }
                    else {
                        additionalScales.add(converted);
                    }

                }

            }

            bones.put(entry.getKey(), new KnightLibAnimation.Channels(
                    position, rotation, scale,
                    additionalPositions, additionalRotations, additionalScales)
            );

        }

        KnightLibAnimation.LoopMode loopMode = loopOverride != null ? loopOverride :
                definition.looping()
                        ? KnightLibAnimation.LoopMode.LOOP
                        : KnightLibAnimation.LoopMode.ONCE;

        // Blockbench exports static single-keyframe poses as zero-length non-looping definitions, so holding it instead would make the
        // blending up to rest work properly
        if (loopOverride == null && definition.lengthInSeconds() <= 0f && loopMode == KnightLibAnimation.LoopMode.ONCE) {
            loopMode = KnightLibAnimation.LoopMode.HOLD_ON_LAST_FRAME;
        }

        return new KnightLibAnimation(name, definition.lengthInSeconds() * 20f, loopMode, bones);
    }

    private static List<KnightLibAnimation.Keyframe> convertKeyframes(AnimationChannel channel, Kind kind) {
        final List<KnightLibAnimation.Keyframe> frames = new ArrayList<>();

        for (final Keyframe keyframe : channel.keyframes()) {
            final KnightLibAnimation.Lerp lerp = keyframe.interpolation() == AnimationChannel.Interpolations.CATMULLROM ?
                    KnightLibAnimation.Lerp.CATMULLROM : KnightLibAnimation.Lerp.LINEAR;

            final Vector3f value = keyframe.target();
            final Vector3f authored = switch (kind) {
                case POSITION -> new Vector3f(value.x(), -value.y(), value.z());
                case ROTATION -> new Vector3f(
                        (float) Math.toDegrees(value.x()),
                        (float) Math.toDegrees(value.y()),
                        (float) Math.toDegrees(value.z())
                );
                case SCALE -> new Vector3f(value.x() + 1f, value.y() + 1f, value.z() + 1f);
            };

            frames.add(new KnightLibAnimation.Keyframe(keyframe.timestamp() * 20f, null, KnightLibAnimation.KeyframeValue.constant(authored), lerp, KnightLibEasings.LINEAR));
        }

        return frames;
    }

    private enum Kind {
        POSITION,
        ROTATION,
        SCALE
    }

}