package dev.xylonity.knightlib.impl.internal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;

@FunctionalInterface
public interface LegacyCustomBossBarRenderer {
    void render(GuiGraphics gui, LerpingBossEvent boss, int x, int y);
}