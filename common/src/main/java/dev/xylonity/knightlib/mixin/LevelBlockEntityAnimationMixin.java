package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.animation.internal.BlockEntityAnimationTicker;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the independent blockentity animation registry once at the end of each logical level tick.
 */
@Mixin(Level.class)
public abstract class LevelBlockEntityAnimationMixin {

    @Inject(method = "tickBlockEntities", at = @At("RETURN"))
    private void knightlib$tickBlockEntityAnimations(CallbackInfo ci) {
        BlockEntityAnimationTicker.tick((Level) (Object) this);
    }

}