package dev.xylonity.knightlib.client.shader.post.internal;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

public final class PostShaderRenderContext {

    public final PostShaderRenderStage stage;
    public final float partialTicks;

    @Nullable
    public final Matrix4f projection;
    @Nullable
    public final Matrix4f modelView;

    @Nullable
    public final Matrix4f inverseProjection;

    public final Camera camera;
    public final Vec3 cameraPosition;

    public PostShaderRenderContext(PostShaderRenderStage stage, float partialTicks, Matrix4f projection, Matrix4f modelView, Camera camera, Vec3 cameraPosition) {
        this.stage = stage;
        this.partialTicks = partialTicks;
        this.projection = projection;
        this.modelView = modelView;
        this.camera = camera;
        this.cameraPosition = cameraPosition;

        this.inverseProjection = (projection == null) ? null : new Matrix4f(projection).invert(new Matrix4f());
    }

}