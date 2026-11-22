package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.ClientRenderHudElementEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.PlayerRideableJumping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dispatches {@link ClientRenderHudElementEvent} before each vanilla HUD element
 */
@Mixin(Gui.class)
public abstract class GuiHudElementMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void knightlib$crosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
        knightlib$dispatch(guiGraphics, ClientRenderHudElementEvent.Element.CROSSHAIR, ci);
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void knightlib$hotbar(float partialTick, GuiGraphics guiGraphics, CallbackInfo ci) {
        knightlib$dispatch(guiGraphics, ClientRenderHudElementEvent.Element.HOTBAR, ci);
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void knightlib$statusBars(GuiGraphics guiGraphics, CallbackInfo ci) {
        knightlib$dispatch(guiGraphics, ClientRenderHudElementEvent.Element.STATUS_BARS, ci);
    }

    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void knightlib$vehicleHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        knightlib$dispatch(guiGraphics, ClientRenderHudElementEvent.Element.VEHICLE_HEALTH, ci);
    }

    /**
     * Also draws the level number on this version
     */
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void knightlib$experienceBar(GuiGraphics guiGraphics, int x, CallbackInfo ci) {
        knightlib$dispatch(guiGraphics, ClientRenderHudElementEvent.Element.EXPERIENCE_BAR, ci);
    }

    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    private void knightlib$jumpMeter(PlayerRideableJumping rideable, GuiGraphics guiGraphics, int x, CallbackInfo ci) {
        knightlib$dispatch(guiGraphics, ClientRenderHudElementEvent.Element.JUMP_METER, ci);
    }

    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void knightlib$selectedItemName(GuiGraphics guiGraphics, CallbackInfo ci) {
        knightlib$dispatch(guiGraphics, ClientRenderHudElementEvent.Element.SELECTED_ITEM_NAME, ci);
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void knightlib$effects(GuiGraphics guiGraphics, CallbackInfo ci) {
        knightlib$dispatch(guiGraphics, ClientRenderHudElementEvent.Element.EFFECTS, ci);
    }

    @Unique
    private void knightlib$dispatch(GuiGraphics guiGraphics, ClientRenderHudElementEvent.Element element, CallbackInfo ci) {
        final ClientRenderHudElementEvent event = new ClientRenderHudElementEvent(minecraft, guiGraphics, element);
        KnightLibEvents.CLIENT.dispatch(event);
        if (event.isCancelled()) {
            ci.cancel();
        }

    }

}
