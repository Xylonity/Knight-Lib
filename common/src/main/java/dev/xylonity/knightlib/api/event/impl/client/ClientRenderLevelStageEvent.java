package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.client.shader.post.interop.PostShaderRenderStage;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Fired at specific stages during level rendering (e.g. AFTER_WEATHER, AFTER_TRANSLUCENT, etc.)
 */
public class ClientRenderLevelStageEvent extends KnightLibEvent {

    private final Minecraft client;
    private final PostShaderRenderStage stage;
    private final float partialTick;

    private final Matrix4f projection;
    private final Matrix4f modelView;

    private final Camera camera;
    private final Vec3 cameraPos;

    public ClientRenderLevelStageEvent(Minecraft client, PostShaderRenderStage stage, float partialTick, Matrix4f projection, Matrix4f modelView, Camera camera, Vec3 cameraPos) {
        this.client = client;
        this.stage = stage;
        this.partialTick = partialTick;
        this.projection = projection;
        this.modelView = modelView;
        this.camera = camera;
        this.cameraPos = cameraPos;
    }

    public Minecraft getClient() {
        return client;
    }

    public PostShaderRenderStage getStage() {
        return stage;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public Matrix4f getProjection() {
        return projection;
    }

    public Matrix4f getModelView() {
        return modelView;
    }

    public Camera getCamera() {
        return camera;
    }

    public Vec3 getCameraPosition() {
        return cameraPos;
    }

}
