package dev.xylonity.knightlib.api.camera.path.impl;

import net.minecraft.network.FriendlyByteBuf;

/**
 * One screen-effect window on the timeline of a {@link CameraPath}
 *
 * @param effect the screen effect this window plays
 * @param startTick path time at which the window begins (inclusive)
 * @param endTick path time at which the window ends
 * @param mode intensity shape over the window
 */
public record EffectKeyframe(
        CameraEffect effect,
        int startTick,
        int endTick,
        EffectMode mode
) {

    public EffectKeyframe {
        effect = (effect == null) ? CameraEffect.FADE : effect;
        mode = (mode == null) ? EffectMode.FULL : mode;
        startTick = Math.max(0, startTick);
        endTick = Math.max(startTick + 1, endTick);
    }

    public int lengthTicks() {
        return endTick - startTick;
    }

    /**
     * Intensity of this window at the given path time
     */
    public float intensityAt(float ticks) {
        if (ticks < startTick || ticks > endTick) {
            return 0f;
        }

        return mode.intensityAt((ticks - startTick) / (float) lengthTicks());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(effect.id());
        buf.writeVarInt(startTick);
        buf.writeVarInt(endTick);
        buf.writeVarInt(mode.id());
    }

    public static EffectKeyframe decode(FriendlyByteBuf buf) {
        final CameraEffect effect = CameraEffect.byId(buf.readVarInt());
        final int startTick = buf.readVarInt();
        final int endTick = buf.readVarInt();
        final EffectMode mode = EffectMode.byId(buf.readVarInt());

        return new EffectKeyframe(effect, startTick, endTick, mode);
    }

}
