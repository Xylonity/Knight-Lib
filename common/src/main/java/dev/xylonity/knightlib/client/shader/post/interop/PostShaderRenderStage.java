package dev.xylonity.knightlib.client.shader.post.interop;

public enum PostShaderRenderStage {
    FRAME_BEGIN,
    DEPTH_READY,
    AFTER_TRANSLUCENT_BLOCKS,
    AFTER_LEVEL,
    AFTER_ENTITIES,
    GAME_RENDERER_TAIL
}
