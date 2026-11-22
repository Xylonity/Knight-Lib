package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.ClientInteractionInputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dispatches {@link ClientInteractionInputEvent} for the clicks vanilla is about to act on
 */
@Mixin(Minecraft.class)
public abstract class MinecraftInputMixin {

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Nullable
    public HitResult hitResult;

    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Shadow
    protected int missTime;

    @Shadow
    private int rightClickDelay;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void knightlib$attackInput(CallbackInfoReturnable<Boolean> cir) {
        // Same outs as vanilla
        if (player == null || missTime > 0 || hitResult == null || player.isHandsBusy()) {
            return;
        }

        final ClientInteractionInputEvent event = knightlib$dispatch(ClientInteractionInputEvent.Type.ATTACK);
        if (event.isCancelled()) {
            cir.setReturnValue(event.isConsumed());
        }

    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void knightlib$continueAttackInput(boolean leftClick, CallbackInfo ci) {
        if (!leftClick || player == null || missTime > 0 || player.isUsingItem()) {
            return;
        }

        if (knightlib$dispatch(ClientInteractionInputEvent.Type.CONTINUE_ATTACK).isCancelled()) {
            ci.cancel();
        }

    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void knightlib$useInput(CallbackInfo ci) {
        if (player == null || gameMode == null || gameMode.isDestroying() || player.isHandsBusy()) {
            return;
        }

        if (knightlib$dispatch(ClientInteractionInputEvent.Type.USE).isCancelled()) {
            // Vanilla's repeat delay, so holding the key doesn't fire it every tick
            rightClickDelay = 4;
            ci.cancel();
        }

    }

    @Unique
    private ClientInteractionInputEvent knightlib$dispatch(ClientInteractionInputEvent.Type type) {
        final ClientInteractionInputEvent event = new ClientInteractionInputEvent((Minecraft) (Object) this, player, type);
        KnightLibEvents.CLIENT.dispatch(event);
        return event;
    }

}
