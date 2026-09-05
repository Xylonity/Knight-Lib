package dev.xylonity.knightlib.api.animation.internal;

import dev.xylonity.knightlib.api.animation.KnightLibAnim.Step;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationBlendMode;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationMask;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.client.animation.molang.MolangContext;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Common animation pipeline used by rendered models and hitbox rigs (server). One instance belongs to one target and logical side.
 * Result and notification views are borrowed until the next evaluation.
 */
public final class KnightLibAnimationEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger("KnightLib");

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private final Map<String, Clock> clocks = new HashMap<>();
    private final Map<String, Resolved> animations = new HashMap<>();
    private final Map<String, Integer> indices = new HashMap<>();
    private final List<AnimationNotification> notifications = new ArrayList<>();
    private final List<AnimationNotification> notificationView = java.util.Collections.unmodifiableList(notifications);
    private final Function<String, KnightLibAnimation> cachedResolver = this::resolveAnimation;
    private final KnightLibAnimation.SampleScratch sampleScratch = new KnightLibAnimation.SampleScratch();

    private final Vector3f sample = new Vector3f();
    private final Vector3f contribution = new Vector3f();

    private Collection<String> skeleton;

    private AnimationPose pose;
    private AnimationLayer layer;
    private Function<String, KnightLibAnimation> resolver;
    private MolangContext context;

    private long frame;
    private int resourceGeneration = Integer.MIN_VALUE;

    public void bindSkeleton(Collection<String> names) {
        if (skeleton == names) {
            return;
        }
        if (skeleton != null && skeleton.size() == names.size() && indices.keySet().containsAll(names)) {
            skeleton = names;
            return;
        }

        skeleton = names;

        final List<String> ordered = List.copyOf(names);
        indices.clear();
        for (int i = 0; i < ordered.size(); i++) {
            if (indices.put(ordered.get(i), i) != null) {
                throw new IllegalArgumentException("[KnightLib] Duplicated skeleton bone: " + ordered.get(i));
            }

        }

        pose = new AnimationPose(ordered);
        layer = new AnimationLayer(ordered.size());
        animations.clear();

        for (final Clock clock : clocks.values()) {
            clock.from = new AnimationLayer(ordered.size());
            clock.blending = false;
        }

    }

    /**
     * Releases stale clip bindings after reload (preserving playtime and such)
     */
    public void resourceGeneration(int generation) {
        if (resourceGeneration != generation) {
            resourceGeneration = generation;
            invalidateAnimations();
        }

    }

    public void invalidateAnimations() {
        animations.clear();
    }

    public AnimationPose evaluate(Collection<KnightLibAnimationHandler.Controller> controllers, Function<String, KnightLibAnimation> resolver, double now, MolangContext context, boolean collectEvents) {
        if (pose == null || !Double.isFinite(now)) {
            throw new IllegalStateException("[KnightLib] Bind a skeleton and supply a finite animation time");
        }

        this.resolver = resolver;
        this.context = context;

        frame++;

        notifications.clear();
        pose.reset();

        try {
            for (final KnightLibAnimationHandler.Controller controller : controllers) {
                Clock clock = clocks.get(controller.name());
                if (clock == null || clock.controller != controller) {
                    clock = new Clock(controller, pose.boneCount());
                    clocks.put(controller.name(), clock);
                }

                clock.seen = frame;
                if (clock.sequence != controller.sequence()) {
                    // Captures the outgoing operation without including any future sequence step
                    evaluateLayer(clock, controller.commandGameTime());
                    clock.from.copyFrom(layer);
                    clock.blending = controller.transitionTicks() > 0;
                    clock.blendStart = controller.commandGameTime();
                    clock.blendTicks = controller.transitionTicks();
                    clock.easing = controller.easing();
                    clock.steps = controller.steps();
                    clock.blendMode = controller.blendMode();
                    clock.mask = controller.mask();
                    clock.finished = false;
                    clock.sequence = controller.sequence();
                    clock.origin = controller.playbackOrigin();
                    clock.offset = controller.playbackOffset();
                    clock.speed = controller.speed();
                    clock.weight = controller.weight();
                    clock.events.start(clock.elapsed(now), controller.isSnapshot());
                }
                else {
                    clock.origin = controller.playbackOrigin();
                    clock.offset = controller.playbackOffset();
                    clock.speed = controller.speed();
                    clock.weight = controller.weight();
                }

                final double elapsed = clock.elapsed(now);
                if (!clock.steps.isEmpty() && !clock.finished) {
                    if (collectEvents) {
                        clock.events.collect(controller.name(), clock.steps, cachedResolver, elapsed, notifications);
                    }

                    clock.playback = KnightLibAnimationSequence.sample(clock.steps, cachedResolver, elapsed);
                    if (clock.playback.finished()) {
                        final double end = clock.origin + (clock.playback.endTick() - clock.offset) / clock.speed;
                        evaluateLayer(clock, end);
                        clock.from.copyFrom(layer);
                        clock.blendStart = end;
                        clock.blending = clock.blendTicks > 0;
                        clock.steps = List.of();
                        clock.finished = true;
                    }

                }
                else if (!clock.finished) {
                    clock.playback = null;
                }

                controller.updatePlayback(clock.playback, elapsed, now);
                evaluateLayer(clock, now, clock.playback);

                layer.compose(pose);
            }

            if (clocks.size() != controllers.size()) {
                clocks.values().removeIf(clock -> clock.seen != frame);
            }

            return pose;
        }
        finally {
            this.resolver = null;
            this.context = null;
        }

    }

    public List<AnimationNotification> notifications() {
        return notificationView;
    }

    /**
     * Current evaluated step
     */
    public KnightLibAnimationSequence.Playback playback(String controller) {
        final Clock clock = clocks.get(controller);
        return clock == null ? null : clock.playback;
    }

    public boolean isAnimationWithin(String name, float minTick, float maxTick) {
        for (final Clock clock : clocks.values()) {
            final KnightLibAnimationSequence.Playback playback = clock.playback;
            if (clock.finished || playback == null || playback.animation() == null) {
                continue;
            }

            final String fullName = playback.animation().name();
            if ((fullName.equals(name) || fullName.endsWith("." + name)) && playback.sampleTick() >= minTick && playback.sampleTick() < maxTick) {
                return true;
            }

        }

        return false;
    }

    public AnimationPose sample(KnightLibAnimation animation, double elapsedTicks, float speed, MolangContext context, boolean forceLoop) {
        this.context = context;
        try {
            pose.reset();
            layer.reset();
            if (!Double.isFinite(elapsedTicks) || !Float.isFinite(speed)) {
                throw new IllegalArgumentException("[KnightLib] Animation time and speed must be finite");
            }

            final double elapsed = forceLoop ? elapsedTicks * speed : Math.max(0.0, elapsedTicks * speed);
            final float tick = forceLoop || animation.loopMode() == KnightLibAnimation.LoopMode.LOOP ? animation.wrapLoopTick(elapsed) : (float) Math.min(elapsed, animation.lengthTicks());
            if (forceLoop || animation.loopMode() != KnightLibAnimation.LoopMode.ONCE || elapsed <= animation.lengthTicks()) {
                context.setControllerSpeed(speed);
                applyChannels(binding(animation), tick, elapsed, false, KnightLibAnimationMask.ALL);
                layer.compose(pose);
            }

            return pose;
        }
        finally {
            this.context = null;
        }

    }

    private void evaluateLayer(Clock clock, double time) {
        final KnightLibAnimationSequence.Playback playback = !clock.steps.isEmpty() && clock.weight != 0f ? KnightLibAnimationSequence.sample(clock.steps, cachedResolver, clock.elapsed(time)) : null;
        evaluateLayer(clock, time, playback);
    }

    private void evaluateLayer(Clock clock, double time, KnightLibAnimationSequence.Playback playback) {
        layer.reset();
        context.setNow(time);
        context.setControllerSpeed(clock.speed);

        if (!clock.steps.isEmpty() && clock.weight != 0f && playback != null && playback.animation() != null) {
            final boolean override = switch (clock.blendMode) {
                case AUTHORED -> playback.animation().overridePreviousAnimation();
                case ADDITIVE -> false;
                case OVERRIDE -> true;
            };

            applyChannels(binding(playback.animation()), playback.sampleTick(), playback.rawTick(), override, clock.mask);

            layer.weight(clock.weight);
        }

        if (clock.blending) {
            final double elapsed = time - clock.blendStart;
            if (elapsed >= clock.blendTicks) {
                clock.blending = false;
            }
            else {
                final float progress = Mth.clamp((float) (elapsed / clock.blendTicks), 0f, 1f);
                layer.blendFrom(clock.from, Mth.clamp(clock.easing.apply(progress), 0f, 1f), pose);
            }

        }

    }

    private KnightLibAnimation resolveAnimation(String name) {
        Resolved resolved = animations.get(name);
        if (resolved == null) {
            resolved = new Resolved();
            animations.put(name, resolved);
        }
        if (resolved.frame != frame) {
            final KnightLibAnimation animation = resolver.apply(name);
            if (resolved.animation != animation) {
                resolved.animation = animation;
                resolved.binding = animation == null ? null : bind(animation);
            }

            resolved.frame = frame;
            if (animation == null && WARNED.add("animation:" + name)) {
                LOGGER.warn("Unknown animation '{}' (not found or ambiguous short name)", name);
            }

        }

        return resolved.animation;
    }

    private BoundAnimation binding(KnightLibAnimation animation) {
        Resolved resolved = animations.get(animation.name());
        if (resolved == null) {
            resolved = new Resolved();
            animations.put(animation.name(), resolved);
        }
        if (resolved.animation != animation || resolved.binding == null) {
            resolved.animation = animation;
            resolved.binding = bind(animation);
        }

        return resolved.binding;
    }

    private BoundAnimation bind(KnightLibAnimation animation) {
        final int[] bones = new int[animation.allChannels().size()];
        final KnightLibAnimation.Channels[] channels = new KnightLibAnimation.Channels[bones.length];
        int i = 0;
        for (final Map.Entry<String, KnightLibAnimation.Channels> entry : animation.allChannels().entrySet()) {
            bones[i] = indices.getOrDefault(entry.getKey(), -1);
            channels[i] = entry.getValue();
            if (bones[i] < 0 && WARNED.add(animation.name() + "\u0000" + entry.getKey())) {
                LOGGER.warn("Animation '{}' targets missing bone '{}'", animation.name(), entry.getKey());
            }

            i++;
        }

        return new BoundAnimation(bones, channels);
    }

    private void applyChannels(BoundAnimation animation, float tick, double rawTick, boolean override, KnightLibAnimationMask mask) {
        context.setAnimTime(tick / 20f);
        context.setTotalAnimTime((float) (rawTick / 20.0));

        for (int i = 0; i < animation.bones.length; i++) {
            final int bone = animation.bones[i];
            if (bone < 0 || !mask.includesBone(pose.boneName(bone))) {
                continue;
            }

            final KnightLibAnimation.Channels channels = animation.channels[i];
            if (override) {
                layer.override(bone, mask.channelBits());
            }
            if ((mask.channelBits() & 1) != 0 && sample(channels.position(), channels.additionalPositions(), tick, false)) {
                layer.channel(bone, 0, sample.x(), sample.y(), sample.z(), override);
            }
            if ((mask.channelBits() & 2) != 0 && sample(channels.rotation(), channels.additionalRotations(), tick, false)) {
                layer.channel(bone, 3, sample.x(), sample.y(), sample.z(), override);
            }
            if ((mask.channelBits() & 4) != 0 && sample(channels.scale(), channels.additionalScales(), tick, true)) {
                layer.channel(bone, 6, sample.x(), sample.y(), sample.z(), override);
            }

        }

    }

    private boolean sample(List<KnightLibAnimation.Keyframe> primary, List<List<KnightLibAnimation.Keyframe>> additional, float tick, boolean scale) {
        boolean sampled = KnightLibAnimation.sample(primary, tick, sample, context, sampleScratch);
        if (!sampled) {
            sample.set(scale ? 1f : 0f);
        }

        for (final List<KnightLibAnimation.Keyframe> track : additional) {
            if (KnightLibAnimation.sample(track, tick, contribution, context, sampleScratch)) {
                sampled = true;
                if (scale) {
                    sample.add(contribution.x() - 1f, contribution.y() - 1f, contribution.z() - 1f);
                }
                else {
                    sample.add(contribution);
                }

            }

        }

        return sampled && Float.isFinite(sample.x()) && Float.isFinite(sample.y()) && Float.isFinite(sample.z());
    }

    public static void clearWarnings() {
        WARNED.clear();
    }

    private static final class Clock {
        final KnightLibAnimationHandler.Controller controller;
        final AnimationEventCursor events = new AnimationEventCursor();
        AnimationLayer from;
        long sequence = -1;
        long seen;
        List<Step> steps = List.of();
        KnightLibAnimationSequence.Playback playback;
        double origin;
        double offset;
        float speed = 1f;
        boolean finished;
        boolean blending;
        double blendStart;
        int blendTicks;
        KnightLibEasings easing = KnightLibEasings.LINEAR;
        KnightLibAnimationBlendMode blendMode = KnightLibAnimationBlendMode.AUTHORED;
        KnightLibAnimationMask mask = KnightLibAnimationMask.ALL;
        float weight = 1f;

        Clock(KnightLibAnimationHandler.Controller controller, int bones) {
            this.controller = controller;
            from = new AnimationLayer(bones);
        }

        double elapsed(double now) {
            return Math.max(0.0, offset + (now - origin) * speed);
        }

    }

    private static final class Resolved {
        long frame = Long.MIN_VALUE;
        KnightLibAnimation animation;
        BoundAnimation binding;
    }

    private record BoundAnimation(
            int[] bones,
            KnightLibAnimation.Channels[] channels
    ) {
        ;;
    }

}
