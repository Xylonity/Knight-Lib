package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Fired once per frame to roll the view around the crosshair. The roll applies to the level and the first person
 * hand, on top of vanilla's damage tilt, and starts at 0 every frame
 */
public final class ClientComputeCameraRollEvent extends KnightLibEvent {

    private final Minecraft client;
    private final Entity cameraEntity;

    private final float partialTick;

    private float roll = 0f;

    public ClientComputeCameraRollEvent(Minecraft client, @Nullable Entity cameraEntity, float partialTick) {
        this.client = client;
        this.cameraEntity = cameraEntity;
        this.partialTick = partialTick;
    }

    public Minecraft getClient() {
        return client;
    }

    @Nullable
    public Entity getCameraEntity() {
        return cameraEntity;
    }

    public float getPartialTick() {
        return partialTick;
    }

    /**
     * Roll in degrees
     */
    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public void addRoll(float roll) {
        this.roll += roll;
    }

}
