package dev.xylonity.knightlib.client.camera.path;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Full-screen dip to black used by fade transitions
 */
public final class FadeOverlay {

    private FadeOverlay() {
        ;;
    }

    public static void render(GuiGraphics graphics, float alpha) {
        if (alpha <= 0f) {
            return;
        }

        final int clampedAlpha = Mth.clamp((int) (alpha * 255f), 0, 255);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), clampedAlpha << 24);
    }

}
