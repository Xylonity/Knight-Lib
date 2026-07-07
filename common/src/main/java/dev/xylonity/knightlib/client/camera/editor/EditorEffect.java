package dev.xylonity.knightlib.client.camera.editor;

import dev.xylonity.knightlib.api.camera.path.impl.CameraEffect;
import dev.xylonity.knightlib.api.camera.path.impl.EffectKeyframe;
import dev.xylonity.knightlib.api.camera.path.impl.EffectMode;

/**
 * Mutable screen-effect window of the editor session
 */
public final class EditorEffect {

    public final CameraEffect effect;
    public int start;
    public int end;
    public EffectMode mode;

    public EditorEffect(CameraEffect effect, int start, int end, EffectMode mode) {
        this.effect = effect;
        this.start = start;
        this.end = end;
        this.mode = mode;
    }

    public int length() {
        return end - start;
    }

    public void cycleMode(int direction) {
        final EffectMode[] modes = EffectMode.values();
        mode = modes[Math.floorMod(mode.ordinal() + direction, modes.length)];
    }

    public EffectKeyframe toImmutable() {
        return new EffectKeyframe(effect, start, end, mode);
    }

}
