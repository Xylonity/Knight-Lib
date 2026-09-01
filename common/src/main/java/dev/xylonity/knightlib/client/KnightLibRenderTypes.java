package dev.xylonity.knightlib.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public final class KnightLibRenderTypes extends RenderType {

    private static final Function<ResourceLocation, RenderType> ENTITY_EMISSIVE = Util.memoize(texture -> createEntityEmissive(texture, false));
    private static final Function<ResourceLocation, RenderType> ENTITY_EMISSIVE_DEPTH = Util.memoize(texture -> createEntityEmissive(texture, true));
    private static final Function<ResourceLocation, RenderType> ENTITY_EMISSIVE_DEPTH_MASK = Util.memoize(KnightLibRenderTypes::createEntityEmissiveDepthMask);

    private KnightLibRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    /**
     * Pass backed by vanilla's eyes shader
     */
    private static RenderType createEntityEmissive(ResourceLocation texture, boolean writeDepth) {
        final RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_EYES_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(writeDepth ? COLOR_DEPTH_WRITE : COLOR_WRITE)
                .createCompositeState(false);

        return create(
                writeDepth ? "knightlib_entity_emissive_depth" : "knightlib_entity_emissive",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, state
        );
    }

    /**
     * Depth alpha pass, computed below the emissive layer, preventing actual geometry from culling clouds
     */
    private static RenderType createEntityEmissiveDepthMask(ResourceLocation texture) {
        final RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(DEPTH_WRITE)
                .createCompositeState(false);

        return create("knightlib_entity_emissive_depth_mask", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, false, state);
    }

    public static final RenderType LINES_SEE_THROUGH = new RenderType(
            "knightlib_editor_lines_see_through", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536, false, false,
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

    public static RenderType entityEmissiveDepthMask(ResourceLocation texture) {
        return ENTITY_EMISSIVE_DEPTH_MASK.apply(texture);
    }

}