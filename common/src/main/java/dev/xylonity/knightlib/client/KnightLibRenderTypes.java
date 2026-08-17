package dev.xylonity.knightlib.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.xylonity.knightlib.client.shader.KnightLibShaders;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public final class KnightLibRenderTypes {

    private static final Function<ResourceLocation, RenderType> ENTITY_EMISSIVE = Util.memoize(texture -> createEntityUnshadedEmissive(texture, false));
    private static final Function<ResourceLocation, RenderType> ENTITY_EMISSIVE_DEPTH = Util.memoize(texture -> createEntityUnshadedEmissive(texture, true));

    private static RenderType createEntityUnshadedEmissive(ResourceLocation texture, boolean writeDepth) {
        final RenderType emissive = RenderType.entityTranslucentEmissive(texture);
        return new RenderType(
                writeDepth ? "knightlib_entity_unshaded_emissive_depth" : "knightlib_entity_unshaded_emissive",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                () -> {
                    emissive.setupRenderState();
                    RenderSystem.setShader(KnightLibShaders::getEntityEmissive);
                    if (writeDepth) {
                        RenderSystem.depthMask(true);
                    }

                },
                emissive::clearRenderState
        ) {
            ;;
        };

    }

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

    public static RenderType entityEmissive(ResourceLocation texture) {
        return entityEmissive(texture, true);
    }

    public static RenderType entityEmissive(ResourceLocation texture, boolean writeDepth) {
        return (writeDepth ? ENTITY_EMISSIVE_DEPTH : ENTITY_EMISSIVE).apply(texture);
    }

}