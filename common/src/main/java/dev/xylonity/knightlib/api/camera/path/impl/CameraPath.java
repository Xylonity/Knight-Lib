package dev.xylonity.knightlib.api.camera.path.impl;

import dev.xylonity.knightlib.api.util.KnightLibEasings;
import dev.xylonity.knightlib.network.packets.CameraPathS2C;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract definition of a keyframed camera movement. The helper packet CameraPathS2C should be called to play
 * paths for specific players when working with server-sided logic
 * <br><br>
 * <code>
 *
 *     final CameraPath path = CameraPath.builder()
 *          .fromCurrentCamera()
 *          .keyframe(new Vec3(0, 80, 0), 90f, 20f, 60, KnightLibEasings.EASE_IN_OUT_CUBIC)
 *          .pause(20)
 *          .cut(new Vec3(20, 75, 10), 120f, 15f).blur(8)
 *          .fade(0, 10, EffectMode.IN)
 *          .letterbox()
 *          .holdTicks(30)
 *          .build();
 *
 * </code>
 *
 * @see CameraPathManager the client-side player of paths
 * @see CameraPathS2C
 */
public final class CameraPath {

    public static final float DEFAULT_BLUR_RADIUS = 10f;
    private static final int MAX_KEYFRAMES = 8192;

    private final List<CameraKeyframe> keyframes;
    private final List<EffectKeyframe> effects;
    private final boolean fromCurrentCamera;
    private final boolean smoothPosition;
    private final boolean letterbox;
    private final int holdTicks;
    private final float blurRadius;
    private final int returnTicks;
    private final KnightLibEasings returnEasing;

    private CameraPath(List<CameraKeyframe> keyframes, List<EffectKeyframe> effects, boolean fromCurrentCamera, boolean smoothPosition, boolean letterbox, int holdTicks, float blurRadius, int returnTicks, KnightLibEasings returnEasing) {
        this.keyframes = Collections.unmodifiableList(keyframes);
        this.effects = Collections.unmodifiableList(effects);
        this.fromCurrentCamera = fromCurrentCamera;
        this.smoothPosition = smoothPosition;
        this.letterbox = letterbox;
        this.holdTicks = Math.max(0, holdTicks);
        this.blurRadius = Math.max(0f, blurRadius);
        this.returnTicks = Math.max(0, returnTicks);
        this.returnEasing = returnEasing == null ? KnightLibEasings.SMOOTHSTEP : returnEasing;
    }

    public List<CameraKeyframe> keyframes() {
        return keyframes;
    }

    public List<EffectKeyframe> effects() {
        return effects;
    }

    /**
     * Whether the path starts blending from wherever the camera currently is instead of snapping to the first keyframe
     */
    public boolean fromCurrentCamera() {
        return fromCurrentCamera;
    }

    /**
     * Whether positions are interpolated with a CR spline across keyframes instead of linearly
     */
    public boolean smoothPosition() {
        return smoothPosition;
    }

    /**
     * Whether the letterbox is drawn for the whole duration of the path.
     */
    public boolean letterbox() {
        return letterbox;
    }

    /**
     * Extra ticks the camera stays at the last keyframe before the path ends
     */
    public int holdTicks() {
        return holdTicks;
    }

    /**
     * Blur radius (in pixels) a full-intensity blur window reaches
     */
    public float blurRadius() {
        return blurRadius;
    }

    /**
     * Ticks the camera spends blending from the last keyframe back to the player's own camera once the path ends
     */
    public int returnTicks() {
        return returnTicks;
    }

    public KnightLibEasings returnEasing() {
        return returnEasing;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(fromCurrentCamera);
        buf.writeBoolean(smoothPosition);
        buf.writeBoolean(letterbox);
        buf.writeVarInt(holdTicks);
        buf.writeFloat(blurRadius);
        buf.writeVarInt(returnTicks);
        buf.writeVarInt(returnEasing.id());

        buf.writeVarInt(keyframes.size());
        for (final CameraKeyframe keyframe : keyframes) {
            keyframe.encode(buf);
        }

        buf.writeVarInt(effects.size());
        for (final EffectKeyframe effect : effects) {
            effect.encode(buf);
        }

    }

    public static CameraPath decode(FriendlyByteBuf buf) {
        final boolean fromCurrentCamera = buf.readBoolean();
        final boolean smoothPosition = buf.readBoolean();
        final boolean letterbox = buf.readBoolean();
        final int holdTicks = buf.readVarInt();
        final float blurRadius = buf.readFloat();
        final int returnTicks = buf.readVarInt();
        final KnightLibEasings returnEasing = KnightLibEasings.byId(buf.readVarInt());

        final int count = buf.readVarInt();
        if (count < 0 || count > MAX_KEYFRAMES) {
            throw new DecoderException("[KnightLib] Too many camera keyframes: " + count);
        }

        final List<CameraKeyframe> keyframes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            keyframes.add(CameraKeyframe.decode(buf));
        }

        final int effectCount = buf.readVarInt();
        if (effectCount < 0 || effectCount > MAX_KEYFRAMES) {
            throw new DecoderException("[KnightLib] Too many effect keyframes: " + effectCount);
        }

        final List<EffectKeyframe> effects = new ArrayList<>(effectCount);
        for (int i = 0; i < effectCount; i++) {
            effects.add(EffectKeyframe.decode(buf));
        }

        return new CameraPath(keyframes, effects, fromCurrentCamera, smoothPosition, letterbox, holdTicks, blurRadius, returnTicks, returnEasing);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<CameraKeyframe> keyframes = new ArrayList<>();
        private final List<EffectKeyframe> effects = new ArrayList<>();

        private record PendingTransition(
                int keyframeIndex,
                CameraEffect effect,
                int ticks
        ) {
            ;;
        }
        private final List<PendingTransition> pendingTransitions = new ArrayList<>();

        private boolean fromCurrentCamera = false;
        private boolean smoothPosition = false;
        private boolean letterbox = false;
        private int holdTicks = 0;
        private float blurRadius = DEFAULT_BLUR_RADIUS;
        private int returnTicks = 0;
        private KnightLibEasings returnEasing = KnightLibEasings.SMOOTHSTEP;

        public Builder keyframe(Vec3 position, float yaw, float pitch, int durationTicks) {
            return keyframe(position, yaw, pitch, durationTicks, KnightLibEasings.SMOOTHSTEP);
        }

        public Builder keyframe(Vec3 position, float yaw, float pitch, int durationTicks, KnightLibEasings easing) {
            keyframes.add(new CameraKeyframe(position, yaw, pitch, durationTicks, easing));
            return this;
        }

        /**
         * If the camera snaps to the specified shot instantly
         */
        public Builder cut(Vec3 position, float yaw, float pitch) {
            keyframes.add(new CameraKeyframe(position, yaw, pitch, 0, KnightLibEasings.LINEAR));
            return this;
        }

        /**
         * Keeps the camera still at the previous keyframe for the given ticks
         */
        public Builder pause(int ticks) {
            final CameraKeyframe keyframe = lastKeyframe("pause()");
            keyframes.add(new CameraKeyframe(keyframe.position(), keyframe.yaw(), keyframe.pitch(), ticks, KnightLibEasings.LINEAR));
            return this;
        }

        /**
         * Chains the last added keyframe with the previous keyframe (so the camera movement flows correctly instead of having each camera transition
         * have its own movement curve)
         */
        public Builder chained() {
            final CameraKeyframe keyframe = lastKeyframe("chained()");
            keyframes.set(keyframes.size() - 1, new CameraKeyframe(keyframe.position(), keyframe.yaw(), keyframe.pitch(), keyframe.durationTicks(), keyframe.easing(), true, keyframe.inTangent(), keyframe.outTangent()));
            return this;
        }

        /**
         * Adds a screen-effect window over an explicit tick range
         */
        public Builder effect(CameraEffect effect, int startTick, int endTick, EffectMode mode) {
            effects.add(new EffectKeyframe(effect, startTick, endTick, mode));
            return this;
        }

        /**
         * Fade window over an explicit tick range
         */
        public Builder fade(int startTick, int endTick, EffectMode mode) {
            return effect(CameraEffect.FADE, startTick, endTick, mode);
        }

        /**
         * Blur window over an explicit tick range
         */
        public Builder blur(int startTick, int endTick, EffectMode mode) {
            return effect(CameraEffect.BLUR, startTick, endTick, mode);
        }

        /**
         * Letterbox window over an explicit tick range
         */
        public Builder letterbox(int startTick, int endTick, EffectMode mode) {
            return effect(CameraEffect.LETTERBOX, startTick, endTick, mode);
        }

        /**
         * Plays the blur post shader around the start of the travel toward the last added keyframe
         */
        public Builder blur(int ticks) {
            return transition(CameraEffect.BLUR, ticks);
        }

        /**
         * Dips the screen to black around the start of the travel toward the last added keyframe
         */
        public Builder fadeToBlack(int ticks) {
            return transition(CameraEffect.FADE, ticks);
        }

        /**
         * Overrides the curve tangents of the last added keyframe
         */
        public Builder tangents(@Nullable Vec3 inTangent, @Nullable Vec3 outTangent) {
            final CameraKeyframe keyframe = lastKeyframe("tangents()");
            keyframes.set(keyframes.size() - 1, new CameraKeyframe(keyframe.position(), keyframe.yaw(), keyframe.pitch(), keyframe.durationTicks(), keyframe.easing(), keyframe.chained(), inTangent, outTangent));
            return this;
        }

        /**
         * Blends from the current camera transform to the first keyframe (using its duration and
         * easing) instead of snapping (otherwise the first keyframe duration would be ignored)
         */
        public Builder fromCurrentCamera() {
            this.fromCurrentCamera = true;
            return this;
        }

        /**
         * Interpolates positions with a CR spline across the keyframes
         */
        public Builder smoothPosition() {
            this.smoothPosition = true;
            return this;
        }

        public Builder letterbox() {
            this.letterbox = true;
            return this;
        }

        public Builder holdTicks(int value) {
            this.holdTicks = value;
            return this;
        }

        public Builder blurRadius(float radius) {
            this.blurRadius = radius;
            return this;
        }

        public Builder returnToPlayer(int ticks) {
            return returnToPlayer(ticks, KnightLibEasings.SMOOTHSTEP);
        }

        public Builder returnToPlayer(int ticks, KnightLibEasings easing) {
            this.returnTicks = ticks;
            this.returnEasing = easing;
            return this;
        }

        public CameraPath build() {
            if (keyframes.isEmpty()) {
                throw new IllegalStateException("[KnightLib] Camera paths require at least one keyframe");
            }

            float travel = 0f;
            for (int i = fromCurrentCamera ? 0 : 1; i < keyframes.size(); i++) {
                travel += keyframes.get(i).durationTicks();
            }

            final List<EffectKeyframe> resolvedEffects = new ArrayList<>(effects);
            for (final PendingTransition pendingTransition : pendingTransitions) {
                resolvedEffects.add(resolveCentered(pendingTransition, travel + Math.max(0, holdTicks)));
            }

            return new CameraPath(new ArrayList<>(keyframes), resolvedEffects, fromCurrentCamera, smoothPosition, letterbox, holdTicks, blurRadius, returnTicks, returnEasing);
        }

        private Builder transition(CameraEffect effect, int ticks) {
            lastKeyframe(effect == CameraEffect.BLUR ? "blur()" : "fadeToBlack()");
            pendingTransitions.add(new PendingTransition(keyframes.size() - 1, effect, Math.max(1, ticks)));
            return this;
        }

        private EffectKeyframe resolveCentered(PendingTransition pending, float totalTicks) {
            float boundary = 0f;
            for (int i = fromCurrentCamera ? 0 : 1; i < pending.keyframeIndex(); i++) {
                boundary += keyframes.get(i).durationTicks();
            }

            int start = Math.round(boundary - pending.ticks() / 2f);
            start = Math.max(0, Math.min(start, Math.round(totalTicks) - pending.ticks()));

            return new EffectKeyframe(pending.effect(), start, start + pending.ticks(), EffectMode.FULL);
        }

        private CameraKeyframe lastKeyframe(String operation) {
            if (keyframes.isEmpty()) {
                throw new IllegalStateException("[KnightLib] " + operation + " requires a previous keyframe");
            }

            return keyframes.get(keyframes.size() - 1);
        }

    }

}