package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.client.ClientKeyInputEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("TAIL"))
    private void knightlib$onKeyPress(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().getWindow()) {
            KnightLibEvents.CLIENT.dispatch(new ClientKeyInputEvent(key, scanCode, action, modifiers));
        }

    }

}