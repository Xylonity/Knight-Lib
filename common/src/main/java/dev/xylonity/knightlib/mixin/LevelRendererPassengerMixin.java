package dev.xylonity.knightlib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.knightlib.api.entity.passenger.KnightLibPassengerHost;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips the render pass of passengers their {@link KnightLibPassengerHost} entity draws itself
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererPassengerMixin {

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void knightlib$skipHostedPassenger(Entity entity, double camX, double camY, double camZ, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (entity.getVehicle() instanceof KnightLibPassengerHost host && host.rendersPassenger(entity)) {
            ci.cancel();
        }

    }

}