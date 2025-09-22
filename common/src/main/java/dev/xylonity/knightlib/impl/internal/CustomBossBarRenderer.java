package dev.xylonity.knightlib.impl.internal;

import dev.xylonity.knightlib.api.bossbar.BossBarContext;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface CustomBossBarRenderer {
    void render(GuiGraphics gui, BossBarContext ctx, int x, int y);
}
