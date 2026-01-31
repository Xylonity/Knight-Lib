package dev.xylonity.knightlib.client.shader.post.internal;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface MultiTargetPostShaderInstance {
    RenderTarget target(String name);
    String keyId();

}
