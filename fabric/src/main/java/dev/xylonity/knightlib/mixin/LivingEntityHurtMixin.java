package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.server.LivingHurtEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityHurtMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float knightlib$onHurt(float amount, DamageSource source) {
        final LivingEntity self = (LivingEntity) (Object) this;
        final LivingHurtEvent event = new LivingHurtEvent(self, source, amount);
        KnightLibEvents.SERVER.dispatch(event);

        if (event.isCancelled()) {
            return 0f;
        }

        return event.getAmount();
    }

}