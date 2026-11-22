package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.server.MobChangeTargetEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Mob.class)
public abstract class MobSetTargetMixin {

    @Shadow
    @Nullable
    private LivingEntity target;

    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity knightlib$onChangeTarget(LivingEntity newTarget) {
        final Mob self = (Mob) (Object) this;
        if (self.level().isClientSide) {
            return newTarget;
        }

        final MobChangeTargetEvent event = new MobChangeTargetEvent(self, newTarget);
        KnightLibEvents.SERVER.dispatch(event);
        return event.isCancelled() ? target : event.getNewTarget();
    }

}
