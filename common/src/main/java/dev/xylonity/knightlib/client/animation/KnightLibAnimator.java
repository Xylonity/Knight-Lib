package dev.xylonity.knightlib.client.animation;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.animation.KnightLibAnim.PlaybackMode;
import dev.xylonity.knightlib.api.animation.KnightLibAnim.Step;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationBlendMode;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import dev.xylonity.knightlib.api.animation.KnightLibKeyframeEvent;
import dev.xylonity.knightlib.api.animation.internal.KnightLibAnimationSequence;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.client.animation.molang.MolangContext;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Samples every controller in a handler and builds the model pose for one frame. This means external models (not linked to a specific
 * entity for example), can be rendered and animated in the current frame.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/animation/AnimationController.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/animation/AnimationProcessor.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/state/BoneSnapshot.java
 */
public final class KnightLibAnimator {

    private static final double EVENT_CATCH_UP_THRESHOLD_TICKS = 40.0;
    private static final double EVENT_CATCH_UP_WINDOW_TICKS = 2.0;

    private static final Set<String> WARNED_ANIMATIONS = new HashSet<>();
    private static final Set<String> WARNED_BONES = new HashSet<>();

    private KnightLibAnimator() {
        ;;
    }

    /**
     * Resets the model and applies every controller in creation order. {@code now} is the client
     * game time in ticks, including the partial tick.
     */
    public static void animate(KnightLibAnimationHandler handler, KnightLibModel model, Function<String, KnightLibAnimation> resolver, double now) {
        final MolangContext molang = clientMolang(handler.animationEntity(), now);

        // Consumes new commands and expires sequences
        for (final KnightLibAnimationHandler.Controller controller : handler.controllers()) {
            final Clock clock = clock(controller);
            if (clock.model != model) {
                clock.model = model;
                clock.sequence = -1;
                clock.steps = List.of();
                clock.blendFrom = null;
                clock.finished = false;
                clock.overridePreviousAnimation = false;
                clock.blendMode = KnightLibAnimationBlendMode.AUTHORED;
            }

            if (controller.sequence() != clock.sequence) {
                final double commandTime = controller.commandGameTime();
                clock.blendFrom = captureAt(clock, model, resolver, commandTime, molang, controller.steps());
                clock.blendStart = commandTime;
                clock.blendTicks = controller.transitionTicks();
                clock.blendEasing = controller.easing();
                clock.steps = controller.steps();
                clock.start = commandTime;
                clock.speed = controller.speed();
                clock.blendMode = controller.blendMode();
                clock.finished = false;
                clock.sequence = controller.sequence();

                final double elapsed = Math.max(0.0, (now - commandTime) * clock.speed);
                if (!clock.steps.isEmpty()) {
                    clock.overridePreviousAnimation = overridesPreviousAnimation(clock.steps, resolver, elapsed, clock.blendMode);
                }

                clock.lastSequenceTick = elapsed > 0.0 ? elapsed : -1.0E-4;
            }
            else if (Float.compare(controller.speed(), clock.speed) != 0) {
                // A speed change alone must not restart the animation
                final double elapsed = Math.max(0.0, (now - clock.start) * clock.speed);
                clock.speed = controller.speed();
                clock.start = now - elapsed / clock.speed;
            }

            if (!clock.steps.isEmpty() && !clock.finished) {
                final double elapsed = Math.max(0.0, (now - clock.start) * clock.speed);
                fireEvents(handler, controller, clock, resolver, elapsed);
                final KnightLibAnimationSequence.Playback playback = KnightLibAnimationSequence.sample(clock.steps, resolver, elapsed);
                if (playback.animation() != null) {
                    clock.overridePreviousAnimation = overridesPreviousAnimation(playback.animation(), clock.blendMode);
                }
                if (playback.finished()) {
                    final double end = clock.start + playback.endTick() / clock.speed;
                    clock.blendFrom = captureAt(clock, model, resolver, end, molang, List.of());
                    clock.blendStart = end;
                    clock.blendEasing = clock.blendEasing == null ? KnightLibEasings.EASE_IN_OUT_QUAD : clock.blendEasing;
                    clock.steps = List.of();
                    clock.finished = true;
                }

            }

        }

        // Evaluates each controller against the same clean rest pose
        model.resetPose();

        final KnightLibPose rest = model.capturePose();
        KnightLibPose composed = rest;

        for (int pass = 0; pass < 2; pass++) {
            final boolean overridePass = pass == 1;
            for (KnightLibAnimationHandler.Controller controller : handler.controllers()) {
                final Clock clock = (Clock) controller.clientState;
                if (clock.overridePreviousAnimation != overridePass) {
                    continue;
                }

                if (overridePass && clock.steps.isEmpty() && clock.blendFrom != null) {
                    final double elapsed = now - clock.blendStart;
                    if (clock.blendTicks <= 0 || elapsed >= clock.blendTicks) {
                        clock.blendFrom = null;
                        continue;
                    }

                    final float alpha = (float) Math.max(0.0, elapsed / clock.blendTicks);
                    final float eased = clock.blendEasing == null ? alpha : clock.blendEasing.apply(alpha);

                    model.resetPose();
                    model.applyPose(composed);
                    model.blendFromPose(clock.blendFrom, Mth.clamp(eased, 0f, 1f));

                    composed = model.capturePose();

                    continue;
                }

                model.resetPose();
                evaluate(clock, model, resolver, now, molang);

                final KnightLibPose layer = model.capturePose();
                layer.retain(touchedBones(clock, resolver));
                if (layer.boneNames().isEmpty()) {
                    continue;
                }

                model.resetPose();
                model.applyPose(composed);
                if (overridePass) {
                    model.applyPose(layer);
                }
                else {
                    model.applyPoseDelta(layer, rest);
                }

                composed = model.capturePose();
            }

        }

        model.resetPose();
        model.applyPose(composed);

    }

    private static Clock clock(KnightLibAnimationHandler.Controller controller) {
        if (controller.clientState instanceof final Clock clock) {
            return clock;
        }

        final Clock clock = new Clock();
        controller.clientState = clock;

        return clock;
    }

    /**
     * Evaluates the clock's current state at an arbitrary time on a clean model and captures the resulting
     * pose, restricted to the bones this controller can influence
     */
    private static KnightLibPose captureAt(Clock clock, KnightLibModel model, Function<String, KnightLibAnimation> resolver, double time, MolangContext molang, List<Step> incomingSteps) {
        final Set<String> touched = touchedBones(clock, resolver);
        if (clock.blendFrom != null) {
            touched.addAll(clock.blendFrom.boneNames());
        }

        for (final Step step : incomingSteps) {
            final KnightLibAnimation incoming = resolve(resolver, step.animation());
            if (incoming != null) {
                touched.addAll(incoming.boneNames());
            }

        }

        model.resetPose();
        evaluate(clock, model, resolver, time, molang);

        final KnightLibPose pose = model.capturePose();
        model.resetPose();

        pose.retain(touched);

        return pose;
    }

    private static Set<String> touchedBones(Clock clock, Function<String, KnightLibAnimation> resolver) {
        final Set<String> touched = new HashSet<>();
        if (clock == null) {
            return touched;
        }

        if (clock.blendFrom != null) {
            touched.addAll(clock.blendFrom.boneNames());
        }

        for (final Step step : clock.steps) {
            final KnightLibAnimation animation = resolve(resolver, step.animation());
            if (animation != null) {
                touched.addAll(animation.boneNames());
            }

        }

        return touched;
    }

    private static boolean overridesPreviousAnimation(List<Step> steps, Function<String, KnightLibAnimation> resolver, double elapsed, KnightLibAnimationBlendMode blendMode) {
        final KnightLibAnimation animation = KnightLibAnimationSequence.sample(steps, resolver, elapsed).animation();
        return animation != null && overridesPreviousAnimation(animation, blendMode);
    }

    private static boolean overridesPreviousAnimation(KnightLibAnimation animation, KnightLibAnimationBlendMode blendMode) {
        return switch (blendMode) {
            case AUTHORED -> animation.overridePreviousAnimation();
            case ADDITIVE -> false;
            case OVERRIDE -> true;
        };

    }

    private static void evaluate(Clock clock, KnightLibModel model, Function<String, KnightLibAnimation> resolver, double now, MolangContext molang) {
        if (clock == null) {
            return;
        }

        molang.setNow(now);
        molang.setControllerSpeed(clock.speed);

        if (!clock.steps.isEmpty()) {
            final double elapsed = Math.max(0.0, (now - clock.start) * clock.speed);
            final KnightLibAnimationSequence.Playback playback = KnightLibAnimationSequence.sample(clock.steps, resolver, elapsed);
            final KnightLibAnimation animation = playback.animation();
            if (animation != null) {
                final float tick = playback.sampleTick();
                molang.setAnimTime(Math.max(tick, 0f) / 20f);
                molang.setTotalAnimTime((float) Math.max(playback.rawTick(), 0.0) / 20f);
                applyChannels(animation, tick, model, molang);
            }

        }

        if (clock.blendFrom != null) {
            final double elapsed = now - clock.blendStart;
            if (clock.blendTicks <= 0 || elapsed >= clock.blendTicks) {
                clock.blendFrom = null;
            }
            else {
                final float alpha = (float) (elapsed / clock.blendTicks);
                final float eased = clock.blendEasing == null ? alpha : clock.blendEasing.apply(alpha);
                model.blendFromPose(clock.blendFrom, Mth.clamp(eased, 0f, 1f));
            }

        }

    }

    private static void applyChannels(KnightLibAnimation animation, float tick, KnightLibModel model, MolangContext molang) {
        final Vector3f scratch = new Vector3f();
        final Vector3f contribution = new Vector3f();

        for (final Map.Entry<String, KnightLibAnimation.Channels> entry : animation.allChannels().entrySet()) {
            final String bone = entry.getKey();
            if (!model.hasBone(bone)) {
                if (WARNED_BONES.add(animation.name() + "\u0000" + bone)) {
                    KnightLib.LOGGER.warn("Animation '{}' targets missing bone '{}'", animation.name(), bone);
                }

                continue;
            }

            final KnightLibAnimation.Channels channels = entry.getValue();
            if (sampleAdditive(channels.position(), channels.additionalPositions(), tick, scratch, contribution, molang, false)) {
                model.applyPosition(bone, scratch.x(), scratch.y(), scratch.z());
            }

            if (sampleAdditive(channels.rotation(), channels.additionalRotations(), tick, scratch, contribution, molang, false)) {
                model.applyRotation(bone, scratch.x(), scratch.y(), scratch.z());
            }

            if (sampleAdditive(channels.scale(), channels.additionalScales(), tick, scratch, contribution, molang, true)) {
                model.applyScale(bone, scratch.x(), scratch.y(), scratch.z());
            }

        }

    }

    private static boolean sampleAdditive(List<KnightLibAnimation.Keyframe> primary, List<List<KnightLibAnimation.Keyframe>> additional, float tick, Vector3f result, Vector3f contribution, MolangContext molang, boolean scale) {
        boolean sampled = KnightLibAnimation.sample(primary, tick, result, molang);
        if (!sampled) {
            result.set(scale ? 1f : 0f);
        }

        for (final List<KnightLibAnimation.Keyframe> track : additional) {
            if (!KnightLibAnimation.sample(track, tick, contribution, molang)) {
                continue;
            }

            sampled = true;
            if (scale) {
                result.add(contribution.x() - 1f, contribution.y() - 1f, contribution.z() - 1f);
            }
            else {
                result.add(contribution);
            }

        }

        return sampled;
    }

    /**
     * Stateless evaluation for targets without an animation handler (item renderers)
     */
    public static void applyAmbient(KnightLibModel model, KnightLibAnimation animation, double now, float speed) {
        final MolangContext molang = clientMolang(null, now);
        molang.setControllerSpeed(speed);

        model.resetPose();

        final float rawTick = (float) (now * speed);
        final float tick = animation.wrapLoopTick(rawTick);

        molang.setAnimTime(tick / 20f);
        molang.setTotalAnimTime(rawTick / 20f);

        applyChannels(animation, tick, model, molang);
    }

    public static void applyAt(KnightLibModel model, KnightLibAnimation animation, double elapsedTicks, float speed) {
        final MolangContext molang = clientMolang(null, elapsedTicks);
        final float rawTick = (float) Math.max(0.0, elapsedTicks * speed);

        molang.setNow(elapsedTicks);
        molang.setControllerSpeed(speed);
        model.resetPose();

        final float tick = switch (animation.loopMode()) {
            case LOOP -> animation.wrapLoopTick(rawTick);
            case HOLD_ON_LAST_FRAME -> Mth.clamp(rawTick, 0f, animation.lengthTicks());
            case ONCE -> rawTick > animation.lengthTicks() ? -1f : rawTick;
        };

        if (tick >= 0f) {
            molang.setAnimTime(tick / 20f);
            molang.setTotalAnimTime(rawTick / 20f);
            applyChannels(animation, tick, model, molang);
        }

    }

    private static KnightLibAnimation resolve(Function<String, KnightLibAnimation> resolver, String name) {
        final KnightLibAnimation animation = resolver.apply(name);
        if (animation == null && WARNED_ANIMATIONS.add(name)) {
            KnightLib.LOGGER.warn("Unknown animation '{}' (not found or ambiguous short name)", name);
        }

        return animation;
    }

    private static MolangContext clientMolang(Entity entity, double now) {
        final MolangContext context = new MolangContext();
        context.setEntity(entity);
        context.setNow(now);

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return context;
        }
        if (minecraft.level != null) {
            context.setLevel(minecraft.level);
            context.setActorCount(minecraft.level.getEntityCount());
        }
        if (entity != null && minecraft.gameRenderer != null) {
            context.setDistanceFromCamera((float) minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(entity.position()));
        }

        return context;
    }

    public static void clearWarnings() {
        WARNED_ANIMATIONS.clear();
        WARNED_BONES.clear();
    }

    private static void fireEvents(KnightLibAnimationHandler handler, KnightLibAnimationHandler.Controller controller, Clock clock, Function<String, KnightLibAnimation> resolver, double sequenceTick) {
        if (sequenceTick < clock.lastSequenceTick) {
            clock.lastSequenceTick = sequenceTick;
            return;
        }

        final double sequenceFrom = clock.lastSequenceTick;
        double eventFrom = sequenceFrom;
        if (sequenceTick - sequenceFrom > EVENT_CATCH_UP_THRESHOLD_TICKS) {
            eventFrom = sequenceTick - EVENT_CATCH_UP_WINDOW_TICKS;
        }

        int fired = 0;
        double stepStart = 0.0;
        for (final Step step : clock.steps) {
            final KnightLibAnimation animation = resolve(resolver, step.animation());
            if (animation == null) {
                break;
            }

            final double length = animation.lengthTicks();
            final double localTo = sequenceTick - stepStart;
            if (localTo < 0.0) {
                break;
            }

            if (step.mode() == PlaybackMode.LOOP) {
                if (animation.events().isEmpty()) {
                    break;
                }

                final double localFrom = eventFrom - stepStart;
                final long firstCycle = Math.max(0L, (long) Math.floor(Math.max(localFrom, 0.0) / length));
                final long lastCycle = Math.max(firstCycle, (long) Math.floor(localTo / length));
                for (long cycle = firstCycle; cycle <= lastCycle && fired < 128; cycle++) {
                    for (final KnightLibAnimation.KeyframeEvent event : animation.events()) {
                        final double occurrence = stepStart + cycle * length + event.tick();
                        if (occurrence > eventFrom && occurrence <= sequenceTick && fired++ < 128) {
                            notifyEvent(handler, controller.name(), animation.name(), event);
                        }

                    }

                }

                break;
            }

            final double eventEnd = stepStart + Math.min(localTo, length);
            for (final KnightLibAnimation.KeyframeEvent event : animation.events()) {
                final double occurrence = stepStart + event.tick();
                if (occurrence > eventFrom && occurrence <= eventEnd && fired++ < 128) {
                    notifyEvent(handler, controller.name(), animation.name(), event);
                }

            }

            if (step.mode() == PlaybackMode.HOLD_ON_LAST_FRAME) {
                break;
            }

            final double stepEnd = stepStart + length;
            if (stepEnd > sequenceFrom && stepEnd <= sequenceTick) {
                notifyFinished(handler, controller.name(), animation.name());
            }

            stepStart = stepEnd;
            if (sequenceTick < stepStart) {
                break;
            }

        }

        clock.lastSequenceTick = sequenceTick;
    }

    private static void notifyEvent(KnightLibAnimationHandler handler, String controller, String animation, KnightLibAnimation.KeyframeEvent event) {
        try {
            handler.dispatchKeyframe(new KnightLibKeyframeEvent(controller, animation, event.type(), event.payload(), event.locator()));
        }
        catch (Exception exception) {
            KnightLib.LOGGER.error("Animation keyframe callback failed for '{}'", animation, exception);
        }

    }

    private static void notifyFinished(KnightLibAnimationHandler handler, String controller, String animation) {
        try {
            handler.dispatchFinished(controller, animation);
        }
        catch (Exception exception) {
            KnightLib.LOGGER.error("Animation completion callback failed for '{}'", animation, exception);
        }

    }

    private static final class Clock {

        KnightLibModel model;
        long sequence = -1;
        List<Step> steps = List.of();
        double start;
        float speed = 1f;
        boolean finished;
        double lastSequenceTick = -1.0E-4;

        KnightLibPose blendFrom;
        double blendStart;
        int blendTicks;
        KnightLibEasings blendEasing;
        boolean overridePreviousAnimation;
        KnightLibAnimationBlendMode blendMode = KnightLibAnimationBlendMode.AUTHORED;

    }

}