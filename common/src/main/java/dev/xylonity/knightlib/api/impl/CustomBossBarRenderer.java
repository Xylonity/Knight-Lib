package dev.xylonity.knightlib.api.impl;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;

@FunctionalInterface
public interface CustomBossBarRenderer {

    void render(GuiGraphics gui, LerpingBossEvent boss, int x, int y);

}