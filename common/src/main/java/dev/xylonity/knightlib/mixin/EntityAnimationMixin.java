package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Persistence and ticking for every animatable entity
 */
@Mixin(Entity.class)
public abstract class EntityAnimationMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void knightlib$tickAnimations(CallbackInfo ci) {
        if ((Object) this instanceof KnightLibAnimatable animatable) {
            animatable.tickAnimations();
        }

    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void knightlib$saveAnimations(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
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

}