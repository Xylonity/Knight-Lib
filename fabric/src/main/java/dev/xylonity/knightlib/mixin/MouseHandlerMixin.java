package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.ClientMouseScrollEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void knightlib$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (window != minecraft.getWindow().getWindow() || minecraft.screen != null || minecraft.player == null) {
            return;
        }

        final ClientMouseScrollEvent event = new ClientMouseScrollEvent(minecraft, yOffset);
        KnightLibEvents.CLIENT.dispatch(event);

        if (event.isCancelled()) {
            ci.cancel();
        }

    }

}
