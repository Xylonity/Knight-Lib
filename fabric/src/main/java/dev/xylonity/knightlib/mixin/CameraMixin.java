package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.camera.CameraShakeManager;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {

    @Inject(method = "setup", at = @At("TAIL"))
    private void knightlib$onSetup(BlockGetter blockGetter, Entity cameraEntity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (blockGetter instanceof Level && cameraEntity instanceof Player player) {
            CameraShakeManager.applyShakeIfPresent(player, (Camera) (Object) this, partialTick);
        }

    }

}