package dev.xylonity.knightlib.api.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.LerpingBossEvent;

@FunctionalInterface
public interface CustomBossBarRenderer {

    void render(PoseStack poseStack, LerpingBossEvent boss, int x, int y);

}