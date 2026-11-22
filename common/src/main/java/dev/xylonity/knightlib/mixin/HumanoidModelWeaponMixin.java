package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.client.item.KnightLibWeaponRenderHooks;
import dev.xylonity.knightlib.client.item.KnightLibWeaponClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third person attack pose of {@link dev.xylonity.knightlib.api.item.KnightLibWeaponItem}
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelWeaponMixin<T extends LivingEntity> {

    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true)
    private void knightlib$weaponThirdPersonAttack(T entity, float ageInTicks, CallbackInfo ci) {
        final InteractionHand hand = entity.swingingArm == null ? InteractionHand.MAIN_HAND : entity.swingingArm;
        final KnightLibWeaponRenderHooks hooks = KnightLibWeaponClient.hooks(entity.getItemInHand(hand));
        if (hooks != null && hooks.setupThirdPersonAttack((HumanoidModel<?>) (Object) this, entity, hand, ageInTicks)) {
            ci.cancel();
        }

    }

}