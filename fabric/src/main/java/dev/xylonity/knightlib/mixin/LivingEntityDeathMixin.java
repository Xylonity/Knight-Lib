package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.server.LivingDeathEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityDeathMixin {

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void knightlib$onDeath(DamageSource source, CallbackInfo ci) {
        final LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) {
            return;
        }

        final LivingDeathEvent event = new LivingDeathEvent(self, source);
        KnightLibEvents.SERVER.dispatch(event);

        if (event.isCancelled()) {
            ci.cancel();
        }

    }

}
