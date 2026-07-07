package dev.xylonity.knightlib.client.camera.path;

import dev.xylonity.knightlib.api.util.KnightLibEasings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Black bars at the top and bottom of the screen (cinematic letterbox)
 */
public final class LetterboxOverlay {

    private static final float ANIM_TICKS = 5f;
    private static final float SCREEN_FRACTION = 0.125f;

    private LetterboxOverlay() {
        ;;
    }

    public static float pathIntensity(float ticks, float totalTicks) {
        final float in = Mth.clamp(ticks / ANIM_TICKS, 0f, 1f);
        final float out = Mth.clamp((totalTicks - ticks) / ANIM_TICKS, 0f, 1f);

        return KnightLibEasings.EASE_OUT_CUBIC.apply(Math.min(in, out));
    }

    public static void render(GuiGraphics graphics, float intensity) {
        if (intensity <= 0f) {
            return;
        }

        final int width = graphics.guiWidth();
        final int height = graphics.guiHeight();

        int barHeight = Math.round(height * SCREEN_FRACTION * Mth.clamp(intensity, 0f, 1f));
        if (barHeight > 0) {
            graphics.fill(0, 0, width, barHeight, 0xFF000000);
            graphics.fill(0, height - barHeight, width, height, 0xFF000000);
        }

    }

}