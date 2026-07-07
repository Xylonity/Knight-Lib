package dev.xylonity.knightlib.client.camera.editor;

import dev.xylonity.knightlib.api.camera.path.impl.CameraEffect;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPath;
import dev.xylonity.knightlib.api.camera.path.impl.CameraPathSampler;
import dev.xylonity.knightlib.api.camera.path.impl.EffectMode;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Working state of the camera path editor
 */
public final class CameraEditorSession {

    public static final int DEFAULT_DURATION_TICKS = 40;
    public static final int DEFAULT_EFFECT_TICKS = 20;
    public static final int DEFAULT_RETURN_TICKS = 30;

    private final List<EditorKeyframe> keyframes = new ArrayList<>();
    private final List<EditorEffect> effects = new ArrayList<>();

    private int selected = -1;
    @Nullable
    private EditorEffect selectedEffect;

    public boolean fromCurrentCamera = false;
    public boolean smoothPosition = true;
    public boolean letterbox = false;
    public boolean returnToPlayer = false;

    // Timeline end
    private int endTicks = -1;

    public float scrubTicks = 0f;

    @Nullable
    private CameraPathSampler cachedSampler;
    private boolean dirty = true;

    public List<EditorKeyframe> keyframes() {
        return keyframes;
    }

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public int keyframeCount() {
        return keyframes.size();
    }

    public int selectedIndex() {
        return selected;
    }

    @Nullable
    public EditorKeyframe selectedKeyframe() {
        return (selected >= 0 && selected < keyframes.size()) ? keyframes.get(selected) : null;
    }

    public void select(int index) {
        selected = Mth.clamp(index, keyframes.isEmpty() ? -1 : 0, keyframes.size() - 1);
        selectedEffect = null;
    }

    /**
     * Captures a new keyframe right after the selected one (or at the end) and selects it
     */
    public EditorKeyframe addKeyframe(Vec3 position, float yaw, float pitch) {
        final EditorKeyframe keyframe = new EditorKeyframe(position, yaw, pitch, DEFAULT_DURATION_TICKS, KnightLibEasings.SMOOTHSTEP);

        final int index = (selected >= 0 && selected < keyframes.size() - 1) ? selected + 1 : keyframes.size();
        keyframes.add(index, keyframe);
        select(index);

        markDirty();
        return keyframe;
    }

    public void removeSelected() {
        if (selectedEffect != null) {
            effects.remove(selectedEffect);
            selectedEffect = null;
            return;
        }

        if (selected < 0 || selected >= keyframes.size()) {
            return;
        }

        keyframes.remove(selected);
        selected = Math.min(selected, keyframes.size() - 1);

        markDirty();
    }

    /**
     * Swaps the selected keyframe with its neighbor along the path
     */
    public void moveSelected(int direction) {
        final int target = selected + direction;
        if (selected < 0 || target < 0 || target >= keyframes.size()) {
            return;
        }

        keyframes.set(selected, keyframes.set(target, keyframes.get(selected)));
        selected = target;

        markDirty();
    }

    public List<EditorEffect> effects() {
        return effects;
    }

    @Nullable
    public EditorEffect selectedEffect() {
        return selectedEffect;
    }

    public void selectEffect(@Nullable EditorEffect effect) {
        selectedEffect = effect;
        if (effect != null) {
            selected = -1;
        }

    }

    /**
     * Drops a new effect window on its track
     */
    public EditorEffect addEffect(CameraEffect type, int atTick) {
        final int half = DEFAULT_EFFECT_TICKS / 2;
        final int start = Math.max(0, atTick - half);

        final EditorEffect effect = new EditorEffect(type, start, start + DEFAULT_EFFECT_TICKS, EffectMode.FULL);
        effects.add(effect);
        selectEffect(effect);

        return effect;
    }

    /**
     * Ticks spent traveling across all segments
     */
    public int travelTicks() {
        int travel = 0;
        for (int i = 1; i < keyframes.size(); i++) {
            travel += keyframes.get(i).durationTicks;
        }

        return travel;
    }

    /**
     * Timeline end
     */
    public int endTicks() {
        int travel = travelTicks();
        return endTicks < 0 ? travel : Math.max(travel, endTicks);
    }

    /**
     * Ticks the camera holds at the last keyframe before the path ends
     */
    public int holdTicks() {
        return endTicks() - travelTicks();
    }

    public void setEndTicks(int value) {
        endTicks = Math.max(travelTicks(), Math.max(1, value));
        markDirty();
    }

    public void clear() {
        keyframes.clear();
        effects.clear();
        selected = -1;
        selectedEffect = null;
        scrubTicks = 0f;
        endTicks = -1;

        markDirty();
    }

    /**
     * Must be called after any mutation that affects the trajectory, so the cached sampler is rebuilt
     */
    public void markDirty() {
        dirty = true;
    }

    /**
     * Shared sampler over the authored keyframes (without the 'from current camera' as its only present in the playback)
     */
    @Nullable
    public CameraPathSampler sampler() {
        if (dirty) {
            cachedSampler = keyframes.isEmpty() ? null : CameraPathSampler.of(keyframes.stream().map(EditorKeyframe::toImmutable).toList(), smoothPosition, holdTicks());
            dirty = false;
        }

        return cachedSampler;
    }

    public CameraPath buildPath() {
        final CameraPath.Builder builder = CameraPath.builder();

        if (fromCurrentCamera) {
            builder.fromCurrentCamera();
        }
        if (smoothPosition) {
            builder.smoothPosition();
        }
        if (letterbox) {
            builder.letterbox();
        }
        if (returnToPlayer) {
            builder.returnToPlayer(DEFAULT_RETURN_TICKS);
        }

        builder.holdTicks(holdTicks());

        for (final EditorKeyframe keyframe : keyframes) {
            builder.keyframe(keyframe.position, keyframe.yaw, keyframe.pitch, keyframe.durationTicks, keyframe.easing);

            if (keyframe.chained) {
                builder.chained();
            }

            if (keyframe.hasCustomTangents()) {
                builder.tangents(keyframe.inTangent, keyframe.outTangent);
            }

        }

        for (final EditorEffect effect : effects) {
            builder.effect(effect.effect, effect.start, effect.end, effect.mode);
        }

        return builder.build();
    }

    /**
     * Copy-pasteable builder code from the current cutscene session
     */
    public String toCode() {
        final StringBuilder codeString = new StringBuilder("CameraPath path = CameraPath.builder()\n");

        if (fromCurrentCamera) {
            codeString.append("        .fromCurrentCamera()\n");
        }

        for (final EditorKeyframe keyframe : keyframes) {
            if (keyframe.durationTicks == 0) {
                codeString.append(String.format(Locale.ROOT, "        .cut(new Vec3(%.2f, %.2f, %.2f), %.1ff, %.1ff)%n",
                        keyframe.position.x, keyframe.position.y, keyframe.position.z, keyframe.yaw, keyframe.pitch));
            }
            else {
                codeString.append(String.format(Locale.ROOT, "        .keyframe(new Vec3(%.2f, %.2f, %.2f), %.1ff, %.1ff, %d, KnightLibEasings.%s)%n",
                        keyframe.position.x, keyframe.position.y, keyframe.position.z, keyframe.yaw, keyframe.pitch, keyframe.durationTicks, keyframe.easing.name()));
            }

            if (keyframe.chained) {
                codeString.append("        .chained()\n");
            }

            if (keyframe.hasCustomTangents()) {
                codeString.append(String.format(Locale.ROOT, "        .tangents(%s, %s)%n", tangentCode(keyframe.inTangent), tangentCode(keyframe.outTangent)));
            }

        }

        for (final EditorEffect effect : effects) {
            String method = switch (effect.effect) {
                case FADE -> "fade";
                case BLUR -> "blur";
                case LETTERBOX -> "letterbox";
            };

            codeString.append(String.format(Locale.ROOT, "        .%s(%d, %d, EffectMode.%s)%n", method, effect.start, effect.end, effect.mode.name()));
        }

        if (smoothPosition) {
            codeString.append("        .smoothPosition()\n");
        }
        if (letterbox) {
            codeString.append("        .letterbox()\n");
        }
        if (returnToPlayer) {
            codeString.append("        .returnToPlayer(").append(DEFAULT_RETURN_TICKS).append(")\n");
        }
        if (holdTicks() > 0) {
            codeString.append("        .holdTicks(").append(holdTicks()).append(")\n");
        }

        codeString.append("        .build();");

        return codeString.toString();
    }

    private static String tangentCode(@Nullable Vec3 tangent) {
        if (tangent == null) {
            return "null";
        }

        return String.format(Locale.ROOT, "new Vec3(%.2f, %.2f, %.2f)", tangent.x, tangent.y, tangent.z);
    }

}
