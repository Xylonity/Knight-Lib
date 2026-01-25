package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Fired every frame during GUI rendering, after the game GUI is drawn
 */
public final class ClientRenderGuiEvent extends KnightLibEvent {

    private final Minecraft client;
    private final GuiGraphics guiGraphics;
    private final float partialTick;

    public ClientRenderGuiEvent(Minecraft client, GuiGraphics guiGraphics, float partialTick) {
        this.client = client;
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
    }

    @Override
    public boolean isSticky() {
        return false;
    }

    public Minecraft client() {
        return client;
    }

    public GuiGraphics guiGraphics() {
        return guiGraphics;
    }

    public float partialTick() {
        return partialTick;
    }
}
