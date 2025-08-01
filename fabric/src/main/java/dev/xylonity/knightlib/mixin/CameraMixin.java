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
    private void onSetup(BlockGetter level, Entity cameraEntity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {

        if (!(level instanceof Level lvl) || !lvl.isClientSide) return;

        if (cameraEntity instanceof Player player) {
            CameraShakeManager.clear();
            CameraShakeManager.applyShakeIfPresent(player, (Camera)(Object) this);
        }

    }

}
