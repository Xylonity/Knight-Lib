package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.api.animation.internal.BlockEntityAnimationTicker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks animatable blockentities into KnightLib's independent ticker.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityAnimationMixin {

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void knightlib$registerAnimations(Level level, CallbackInfo ci) {
        BlockEntityAnimationTicker.register((BlockEntity) (Object) this);
    }

    @Inject(method = "clearRemoved", at = @At("RETURN"))
    private void knightlib$reregisterAnimations(CallbackInfo ci) {
        BlockEntityAnimationTicker.register((BlockEntity) (Object) this);
    }

    @Inject(method = "setRemoved", at = @At("RETURN"))
    private void knightlib$unregisterAnimations(CallbackInfo ci) {
        BlockEntityAnimationTicker.unregister((BlockEntity) (Object) this);
    }

    @Inject(method = "saveWithoutMetadata", at = @At("RETURN"))
    private void knightlib$saveAnimations(CallbackInfoReturnable<CompoundTag> cir) {
        if ((Object) this instanceof KnightLibAnimatable animatable) {
            animatable.getAnimationHandler().save(cir.getReturnValue());
        }

    }

    @Inject(method = "load", at = @At("RETURN"))
    private void knightlib$loadAnimations(CompoundTag tag, CallbackInfo ci) {
        if ((Object) this instanceof KnightLibAnimatable animatable) {
            animatable.getAnimationHandler().load(tag);
        }

    }

    @Inject(method = "getUpdateTag", at = @At("RETURN"))
    private void knightlib$syncAnimations(CallbackInfoReturnable<CompoundTag> cir) {
        if ((Object) this instanceof KnightLibAnimatable animatable) {
            animatable.getAnimationHandler().save(cir.getReturnValue());
        }

    }

}
