package dev.xylonity.knightlib.api.animation.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.animation.KnightLibKeyframeEvent;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.client.animation.molang.MolangContext;
import dev.xylonity.knightlib.api.client.animation.molang.MolangExpression;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns a Bedrock animation JSON into the small runtime format used by both rendering and server sided hitbox rigs
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/loading/json/typeadapter/BakedAnimationsAdapter.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/loading/json/typeadapter/KeyFramesAdapter.java
 */
public final class GeoAnimationParser {

    private static final Set<String> WARNED_EASINGS = ConcurrentHashMap.newKeySet();

    public static Map<String, KnightLibAnimation> parse(JsonObject root) {
        final Map<String, KnightLibAnimation> animations = new LinkedHashMap<>();
        final JsonObject entries = root.getAsJsonObject("animations");
        if (entries == null) {
            return Map.of();
        }

        for (final Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            // One broken animation should not make every other animation in the file disappear
            try {
                animations.put(entry.getKey(), parseAnimation(entry.getKey(), entry.getValue().getAsJsonObject()));
            }
            catch (Exception exception) {
                KnightLib.LOGGER.error("Failed to parse animation '{}', skipping it", entry.getKey(), exception);
            }

        }

        return Collections.unmodifiableMap(animations);
    }

    private static KnightLibAnimation parseAnimation(String name, JsonObject animation) {
        KnightLibAnimation.LoopMode loopMode = KnightLibAnimation.LoopMode.ONCE;
        if (animation.has("loop")) {
            final JsonPrimitive loop = animation.getAsJsonPrimitive("loop");
            if (loop.isBoolean()) {
                loopMode = loop.getAsBoolean() ? KnightLibAnimation.LoopMode.LOOP : KnightLibAnimation.LoopMode.ONCE;
            }
            else if (loop.getAsString().equals("hold_on_last_frame")) {
                loopMode = KnightLibAnimation.LoopMode.HOLD_ON_LAST_FRAME;
            }
            else if (loop.getAsString().equals("true")) {
                loopMode = KnightLibAnimation.LoopMode.LOOP;
            }

        }

        final Map<String, KnightLibAnimation.Channels> bones = new LinkedHashMap<>();
        final List<KnightLibAnimation.KeyframeEvent> events = new ArrayList<>();
        float maxTick = 0f;

        if (animation.has("bones")) {
            for (final Map.Entry<String, JsonElement> bone : animation.getAsJsonObject("bones").entrySet()) {
                final JsonObject channels = bone.getValue().getAsJsonObject();
                final List<KnightLibAnimation.Keyframe> position = channels.has("position") ? parseKeyframes(name, channels.get("position")) : null;
                final List<KnightLibAnimation.Keyframe> rotation = channels.has("rotation") ? parseKeyframes(name, channels.get("rotation")) : null;
                final List<KnightLibAnimation.Keyframe> scale = channels.has("scale") ? parseKeyframes(name, channels.get("scale")) : null;

                maxTick = Math.max(maxTick, lastTick(position));
                maxTick = Math.max(maxTick, lastTick(rotation));
                maxTick = Math.max(maxTick, lastTick(scale));

                bones.put(bone.getKey(), new KnightLibAnimation.Channels(position, rotation, scale));
            }

        }

        parseEventTrack(animation, "sound_effects", KnightLibKeyframeEvent.Type.SOUND, events);
        parseEventTrack(animation, "particle_effects", KnightLibKeyframeEvent.Type.PARTICLE, events);
        parseEventTrack(animation, "timeline", KnightLibKeyframeEvent.Type.TIMELINE, events);

        events.sort(Comparator.comparingDouble(KnightLibAnimation.KeyframeEvent::tick));

        if (!events.isEmpty()) {
            maxTick = Math.max(maxTick, events.get(events.size() - 1).tick());
        }

        final boolean explicitLength = animation.has("animation_length");
        float lengthTicks = explicitLength ? animation.get("animation_length").getAsFloat() * 20f : maxTick;
        if (!explicitLength && lengthTicks <= 0f) {
            lengthTicks = Float.MAX_VALUE;
        }
        if (lengthTicks <= 0f && loopMode == KnightLibAnimation.LoopMode.ONCE) {
            loopMode = KnightLibAnimation.LoopMode.HOLD_ON_LAST_FRAME;
        }

        final boolean overridePreviousAnimation = animation.has("override_previous_animation")
                && animation.get("override_previous_animation").getAsBoolean();
        return new KnightLibAnimation(name, lengthTicks, loopMode, bones, events, overridePreviousAnimation);
    }

    private static void parseEventTrack(JsonObject animation, String key, KnightLibKeyframeEvent.Type type, List<KnightLibAnimation.KeyframeEvent> output) {
        if (!animation.has(key) || !animation.get(key).isJsonObject()) {
            return;
        }
        for (final Map.Entry<String, JsonElement> entry : animation.getAsJsonObject(key).entrySet()) {
            final float tick = Float.parseFloat(entry.getKey()) * 20f;
            final JsonElement value = entry.getValue();
            if (value.isJsonArray()) {
                for (final JsonElement item : value.getAsJsonArray()) {
                    addEvent(tick, type, item, output);
                }

            }
            else {
                addEvent(tick, type, value, output);
            }

        }

    }

    private static void addEvent(float tick, KnightLibKeyframeEvent.Type type, JsonElement value, List<KnightLibAnimation.KeyframeEvent> output) {
        String payload = "";
        String locator = "";
        if (value.isJsonPrimitive()) {
            payload = value.getAsString();
        }
        else if (value.isJsonObject()) {
            final JsonObject object = value.getAsJsonObject();
            final String payloadKey = type == KnightLibKeyframeEvent.Type.TIMELINE ? "event" : "effect";
            if (object.has(payloadKey)) {
                payload = object.get(payloadKey).getAsString();
            }
            else if (object.has("sound")) {
                payload = object.get("sound").getAsString();
            }

            if (object.has("locator")) {
                locator = object.get("locator").getAsString();
            }
            if (type == KnightLibKeyframeEvent.Type.PARTICLE && object.has("pre_effect_script")) {
                String script = object.get("pre_effect_script").getAsString();
                payload = payload.isEmpty() ? script : payload + "\n" + script;
            }

        }

        if (!payload.isEmpty()) {
            output.add(new KnightLibAnimation.KeyframeEvent(tick, type, payload, locator));
        }

    }

    private static float lastTick(List<KnightLibAnimation.Keyframe> frames) {
        return frames == null || frames.isEmpty() ? 0f : frames.get(frames.size() - 1).tick();
    }

    private static List<KnightLibAnimation.Keyframe> parseKeyframes(String animationName, JsonElement channel) {
        final List<KnightLibAnimation.Keyframe> frames = new ArrayList<>();
        if (channel.isJsonPrimitive() || channel.isJsonArray()) {
            frames.add(new KnightLibAnimation.Keyframe(0f, null, parseValue(channel), KnightLibAnimation.Lerp.LINEAR, KnightLibEasings.LINEAR));
            return frames;
        }

        final JsonObject channelObject = channel.getAsJsonObject();
        if (channelObject.has("vector")) {
            frames.add(new KnightLibAnimation.Keyframe(0f, null, parseValue(channelObject.get("vector")), KnightLibAnimation.Lerp.LINEAR, KnightLibEasings.LINEAR));
            return frames;
        }

        for (final Map.Entry<String, JsonElement> entry : channelObject.entrySet()) {
            final float tick = Float.parseFloat(entry.getKey()) * 20f;
            final JsonElement value = entry.getValue();
            if (value.isJsonPrimitive() || value.isJsonArray()) {
                frames.add(new KnightLibAnimation.Keyframe(tick, null, parseValue(value), KnightLibAnimation.Lerp.LINEAR, KnightLibEasings.LINEAR));
                continue;
            }

            final JsonObject frame = value.getAsJsonObject();
            final KnightLibAnimation.Lerp lerp = frame.has("lerp_mode")
                    && frame.get("lerp_mode").getAsString().equals("catmullrom")
                    ? KnightLibAnimation.Lerp.CATMULLROM : KnightLibAnimation.Lerp.LINEAR;

            final KnightLibEasings easing = frame.has("easing")
                    ? mapEasing(frame.get("easing").getAsString()) : KnightLibEasings.LINEAR;

            float easingArgument = Float.NaN;
            if (frame.has("easingArgs") && frame.get("easingArgs").isJsonArray() && !frame.getAsJsonArray("easingArgs").isEmpty()) {
                final JsonElement argument = frame.getAsJsonArray("easingArgs").get(0);
                if (argument.isJsonPrimitive() && argument.getAsJsonPrimitive().isNumber()) {
                    easingArgument = argument.getAsFloat();
                }

            }

            final KnightLibAnimation.KeyframeValue pre = frame.has("pre") ? parseValue(unwrapVector(frame.get("pre"))) : null;

            // pre arrives, post leaves the timestamp
            final KnightLibAnimation.KeyframeValue post;
            if (frame.has("post")) {
                post = parseValue(unwrapVector(frame.get("post")));
            }
            else if (frame.has("vector")) {
                post = parseValue(frame.get("vector"));
            }
            else {
                throw new IllegalStateException("[KnightLib] Keyframe without value in animation " + animationName);
            }

            frames.add(new KnightLibAnimation.Keyframe(tick, pre, post, lerp, easing, easingArgument));
        }

        frames.sort(Comparator.comparingDouble(KnightLibAnimation.Keyframe::tick));

        return frames;
    }

    private static JsonElement unwrapVector(JsonElement element) {
        return element.isJsonObject() && element.getAsJsonObject().has("vector") ? element.getAsJsonObject().get("vector") : element;
    }

    private static KnightLibAnimation.KeyframeValue parseValue(JsonElement value) {
        if (value.isJsonPrimitive()) {
            final MolangExpression scalar = parseComponent(value.getAsJsonPrimitive());
            if (scalar instanceof Constant constant) {
                return KnightLibAnimation.KeyframeValue.constant(new Vector3f(constant.value, constant.value, constant.value));
            }

            return KnightLibAnimation.KeyframeValue.expression(scalar, scalar, scalar);
        }

        final JsonArray array = value.getAsJsonArray();
        final MolangExpression x = parseComponent(array.get(0).getAsJsonPrimitive());
        final MolangExpression y = parseComponent(array.get(1).getAsJsonPrimitive());
        final MolangExpression z = parseComponent(array.get(2).getAsJsonPrimitive());

        if (x instanceof Constant cx && y instanceof Constant cy && z instanceof Constant cz) {
            return KnightLibAnimation.KeyframeValue.constant(new Vector3f(cx.value, cy.value, cz.value));
        }

        return KnightLibAnimation.KeyframeValue.expression(x, y, z);
    }

    private static MolangExpression parseComponent(JsonPrimitive primitive) {
        if (primitive.isNumber()) {
            return new Constant(primitive.getAsFloat());
        }

        final String text = primitive.getAsString().trim();
        try {
            return new Constant(Float.parseFloat(text));
        }
        catch (NumberFormatException ignored) {
            return MolangExpression.parse(text);
        }

    }

    private static KnightLibEasings mapEasing(String name) {
        final String normalized = name.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        if (normalized.equals("linear") || normalized.equals("none")) {
            return KnightLibEasings.LINEAR;
        }
        if (normalized.equals("step")) {
            return KnightLibEasings.STEP;
        }

        for (final KnightLibEasings easing : KnightLibEasings.values()) {
            if (easing.name().replace("_", "").toLowerCase(Locale.ROOT).equals(normalized)) {
                return easing;
            }

        }

        if (WARNED_EASINGS.add(name)) {
            KnightLib.LOGGER.warn("[KnightLib] Unknown animation easing '{}', falling back to linear", name);
        }

        return KnightLibEasings.LINEAR;
    }

    private record Constant(
            float value
    ) implements MolangExpression {

        @Override
        public float evaluate(MolangContext context) {
            return value;
        }

    }

}
