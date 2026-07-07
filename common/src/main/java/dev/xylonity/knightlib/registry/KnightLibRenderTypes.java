package dev.xylonity.knightlib.registry;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;

public final class KnightLibRenderTypes {

    public static final RenderType LINES_SEE_THROUGH = new RenderType(
            "knightlib_editor_lines_see_through",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            false,
            false,
            () -> {
                RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                RenderSystem.lineWidth(Math.max(2.5f, Minecraft.getInstance().getWindow().getWidth() / 1920f * 2.5f));
                RenderSystem.disableCull();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
            },
            () -> {
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.lineWidth(1f);
            }
    ) {
        ;;
    };

    private KnightLibRenderTypes() {
        ;;
    }

}
