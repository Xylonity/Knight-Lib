package dev.xylonity.knightlib.api.client.animation;

import dev.xylonity.knightlib.api.animation.KnightLibKeyframeEvent;
import dev.xylonity.knightlib.api.client.animation.molang.MolangContext;
import dev.xylonity.knightlib.api.client.animation.molang.MolangExpression;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Keyframe animation, where values are stored in space and happen over ticks
 */
public final class KnightLibAnimation {

    private final String name;
    private final float lengthTicks;
    private final LoopMode loopMode;
    private final Map<String, Channels> bones;
    private final List<KeyframeEvent> events;
    private final boolean overridePreviousAnimation;

    public KnightLibAnimation(String name, float lengthTicks, LoopMode loopMode, Map<String, Channels> bones) {
        this(name, lengthTicks, loopMode, bones, List.of());
    }

    public KnightLibAnimation(String name, float lengthTicks, LoopMode loopMode, Map<String, Channels> bones, List<KeyframeEvent> events) {
        this(name, lengthTicks, loopMode, bones, events, false);
    }

    public KnightLibAnimation(String name, float lengthTicks, LoopMode loopMode, Map<String, Channels> bones, List<KeyframeEvent> events, boolean overridePreviousAnimation) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("[KnightLib] Animation name cannot be blank");
        }
        if (!Float.isFinite(lengthTicks) || lengthTicks < 0f) {
            throw new IllegalArgumentException("[KnightLib] Animation length must be finite and non-negative");
        }

        this.name = name;
        this.lengthTicks = Math.max(lengthTicks, 0.01f);
        this.loopMode = Objects.requireNonNull(loopMode, "loopMode");
        this.bones = Map.copyOf(Objects.requireNonNull(bones, "bones"));
        this.events = List.copyOf(Objects.requireNonNull(events, "events"));
        this.overridePreviousAnimation = overridePreviousAnimation;
    }

    public String name() {
        return name;
    }

    public float lengthTicks() {
        return lengthTicks;
    }

    /**
     * Wraps an elapsed tick into this animation without losing precision for open-ended loops
     */
    public float wrapLoopTick(float tick) {
        final float wrapped = tick % lengthTicks;
        return wrapped < 0f ? wrapped + lengthTicks : wrapped;
    }

    public LoopMode loopMode() {
        return loopMode;
    }

    public Set<String> boneNames() {
        return bones.keySet();
    }

    public Channels channels(String bone) {
        return bones.get(bone);
    }

    public Map<String, Channels> allChannels() {
        return bones;
    }

    public List<KeyframeEvent> events() {
        return events;
    }

    /**
     * Whether this animation replaces the pose contributed by earlier controllers for the bones
     * it touches instead of adding another transform delta on top of them
     */
    public boolean overridePreviousAnimation() {
        return overridePreviousAnimation;
    }

    /**
     * Samples a keyframe list at the given tick into the destination
     */
    public static boolean sample(List<Keyframe> frames, float tick, Vector3f destination, MolangContext context) {
        if (frames == null || frames.isEmpty()) {
            return false;
        }

        final Keyframe first = frames.get(0);
        if (tick <= first.tick()) {
            return resolveFinite(first.post(), context, destination);
        }

        final Keyframe last = frames.get(frames.size() - 1);
        if (tick >= last.tick()) {
            return resolveFinite(last.post(), context, destination);
        }

        int index = 0;
        while (index < frames.size() - 1 && frames.get(index + 1).tick() <= tick) {
            index++;
        }

        final Keyframe from = frames.get(index);
        final Keyframe to = frames.get(index + 1);
        final float span = to.tick() - from.tick();
        final float alpha = span <= 0f ? 1f : (tick - from.tick()) / span;

        final Vector3f start = from.post().resolve(context, new Vector3f());
        final Vector3f end = to.target().resolve(context, new Vector3f());
        if (!finite(start) || !finite(end)) {
            return false;
        }

        if (to.lerp() == Lerp.CATMULLROM) {
            final Vector3f p0 = frames.get(Math.max(0, index - 1)).post().resolve(context, new Vector3f());
            final Vector3f p3 = frames.get(Math.min(frames.size() - 1, index + 2)).post().resolve(context, new Vector3f());
            if (!finite(p0) || !finite(p3)) {
                return false;
            }

            final float x = Mth.catmullrom(alpha, p0.x(), start.x(), end.x(), p3.x());
            final float y = Mth.catmullrom(alpha, p0.y(), start.y(), end.y(), p3.y());
            final float z = Mth.catmullrom(alpha, p0.z(), start.z(), end.z(), p3.z());
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                return false;
            }

            destination.set(x, y, z);
            return true;
        }

        final float eased = to.easing().apply(alpha, to.easingArgument());
        if (!Float.isFinite(eased)) {
            return false;
        }

        final float x = Mth.lerp(eased, start.x(), end.x());
        final float y = Mth.lerp(eased, start.y(), end.y());
        final float z = Mth.lerp(eased, start.z(), end.z());
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            return false;
        }

        destination.set(x, y, z);

        return true;
    }

    private static boolean resolveFinite(KeyframeValue value, MolangContext context, Vector3f destination) {
        final Vector3f resolved = value.resolve(context, new Vector3f());
        if (!finite(resolved)) {
            return false;
        }

        destination.set(resolved);

        return true;
    }

    private static boolean finite(Vector3f value) {
        return Float.isFinite(value.x()) && Float.isFinite(value.y()) && Float.isFinite(value.z());
    }

    public enum LoopMode {
        ONCE,
        LOOP,
        HOLD_ON_LAST_FRAME
    }

    public enum Lerp {
        LINEAR,
        CATMULLROM
    }

    public interface KeyframeValue {

        Vector3f resolve(MolangContext context, Vector3f dest);

        static KeyframeValue constant(Vector3f value) {
            final Vector3f immutableValue = new Vector3f(Objects.requireNonNull(value, "value"));
            return (context, dest) -> dest.set(immutableValue);
        }

        static KeyframeValue expression(MolangExpression x, MolangExpression y, MolangExpression z) {
            Objects.requireNonNull(x, "x");
            Objects.requireNonNull(y, "y");
            Objects.requireNonNull(z, "z");
            return (context, dest) -> dest.set(x.evaluate(context), y.evaluate(context), z.evaluate(context));
        }

    }

    public record Keyframe(
            float tick,
            KeyframeValue pre,
            KeyframeValue post,
            Lerp lerp,
            KnightLibEasings easing,
            float easingArgument
    ) {

        public Keyframe {
            if (!Float.isFinite(tick) || tick < 0f) {
                throw new IllegalArgumentException("[KnightLib] Keyframe tick must be finite and non-negative");
            }

            Objects.requireNonNull(post, "post");
            Objects.requireNonNull(lerp, "lerp");
            Objects.requireNonNull(easing, "easing");
        }

        public Keyframe(float tick, KeyframeValue pre, KeyframeValue post, Lerp lerp, KnightLibEasings easing) {
            this(tick, pre, post, lerp, easing, Float.NaN);
        }

        public KeyframeValue target() {
            return pre != null ? pre : post;
        }

    }

    public record Channels(
            List<Keyframe> position,
            List<Keyframe> rotation,
            List<Keyframe> scale,
            List<List<Keyframe>> additionalPositions,
            List<List<Keyframe>> additionalRotations,
            List<List<Keyframe>> additionalScales
    ) {

        public Channels(List<Keyframe> position, List<Keyframe> rotation, List<Keyframe> scale) {
            this(position, rotation, scale, List.of(), List.of(), List.of());
        }

        public Channels {
            position = immutableTrack(position);
            rotation = immutableTrack(rotation);
            scale = immutableTrack(scale);
            additionalPositions = immutableTracks(additionalPositions);
            additionalRotations = immutableTracks(additionalRotations);
            additionalScales = immutableTracks(additionalScales);
        }

        private static List<Keyframe> immutableTrack(List<Keyframe> track) {
            return track == null ? null : List.copyOf(track);
        }

        private static List<List<Keyframe>> immutableTracks(List<List<Keyframe>> tracks) {
            if (tracks == null || tracks.isEmpty()) {
                return List.of();
            }

            return tracks.stream().map(List::copyOf).toList();
        }

    }

    public record KeyframeEvent(
            float tick,
            KnightLibKeyframeEvent.Type type,
            String payload,
            String locator) {

        public KeyframeEvent {
            if (!Float.isFinite(tick) || tick < 0f) {
                throw new IllegalArgumentException("[KnightLib] Event tick must be finite and non-negative");
            }

            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(payload, "payload");
            Objects.requireNonNull(locator, "locator");
        }

    }

}