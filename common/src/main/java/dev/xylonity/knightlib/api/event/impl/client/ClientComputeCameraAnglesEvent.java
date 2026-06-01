package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Fired when camera angles can be adjusted
 */
public final class ClientComputeCameraAnglesEvent extends KnightLibEvent {

    private final Minecraft client;
    private final Camera camera;
    private final Entity cameraEntity;
    private final float partialTick;

    public ClientComputeCameraAnglesEvent(Minecraft client, Camera camera, Entity cameraEntity, float partialTick) {
        this.client = client;
        this.camera = camera;
        this.cameraEntity = cameraEntity;
        this.partialTick = partialTick;
    }

    public Minecraft getClient() {
        return client;
    }

    public Camera getCamera() {
        return camera;
    }

    public Entity getCameraEntity() {
        return cameraEntity;
    }

    public float getPartialTick() {
        return partialTick;
    }

}
