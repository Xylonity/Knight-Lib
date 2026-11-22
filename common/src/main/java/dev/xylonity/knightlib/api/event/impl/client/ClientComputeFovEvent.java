package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

/**
 * Fired every time the game computes the fov. For the level ({@link #usesFovSetting()} true) and for the first
 * person hand (false, vanilla keeps it at 70 there). The value set here is used for that computation only, so a
 * lasting change must be set again every frame
 */
public final class ClientComputeFovEvent extends KnightLibEvent {

    private final Minecraft client;
    private final Camera camera;
    private final float partialTick;
    private final boolean usesFovSetting;
    private double fov;

    public ClientComputeFovEvent(Minecraft client, Camera camera, float partialTick, boolean usesFovSetting, double fov) {
        this.client = client;
        this.camera = camera;
        this.partialTick = partialTick;
        this.usesFovSetting = usesFovSetting;
        this.fov = fov;
    }

    public Minecraft getClient() {
        return client;
    }

    public Camera getCamera() {
        return camera;
    }

    public float getPartialTick() {
        return partialTick;
    }

    /**
     * Whether this is the level's fov (the one the player's FOV setting and effects scale) and not the hand's
     */
    public boolean usesFovSetting() {
        return usesFovSetting;
    }

    /**
     * Vertical fov in degrees
     */
    public double getFov() {
        return fov;
    }

    public void setFov(double fov) {
        this.fov = fov;
    }

}