package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Fired right before a HUD element is drawn. Cancelling it skips that element for this frame
 */
public final class ClientRenderHudElementEvent extends KnightLibEvent {

    private final Minecraft client;
    private final GuiGraphics guiGraphics;
    private final Element element;
    @Nullable
    private final ResourceLocation layerId;
    private boolean cancelled = false;

    public ClientRenderHudElementEvent(Minecraft client, GuiGraphics guiGraphics, Element element) {
        this(client, guiGraphics, element, null);
    }

    public ClientRenderHudElementEvent(Minecraft client, GuiGraphics guiGraphics, Element element, @Nullable ResourceLocation layerId) {
        this.client = client;
        this.guiGraphics = guiGraphics;
        this.element = element;
        this.layerId = layerId;
    }

    public Minecraft getClient() {
        return client;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public Element getElement() {
        return element;
    }

    /**
     * Id of the gui layer being drawn, only for {@link Element#OTHER} on forge, null otherwise
     */
    @Nullable
    public ResourceLocation getLayerId() {
        return layerId;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum Element {
        CROSSHAIR,
        /// Hotbar slots and the offhand slot
        HOTBAR,
        /// Hearts, armor, food and air
        STATUS_BARS,
        /// Hearts of the ridden mount
        VEHICLE_HEALTH,
        /// Experience bar and level number
        EXPERIENCE_BAR,
        JUMP_METER,
        /// Name shown above the hotbar after switching items
        SELECTED_ITEM_NAME,
        /// Status effect icons in the top right corner
        EFFECTS,
        ///HUD elements added by other mods
        OTHER
    }

}