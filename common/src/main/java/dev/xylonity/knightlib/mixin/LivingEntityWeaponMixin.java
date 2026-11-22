package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.item.KnightLibWeaponItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swing duration and swing start hooks of {@link KnightLibWeaponItem}
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityWeaponMixin {

    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void knightlib$weaponSwingDuration(CallbackInfoReturnable<Integer> cir) {
        final LivingEntity self = (LivingEntity) (Object) this;
        final ItemStack stack = self.getItemInHand(self.swingingArm == null ? InteractionHand.MAIN_HAND : self.swingingArm);
        if (stack.getItem() instanceof KnightLibWeaponItem weapon) {
            cir.setReturnValue(weapon.getSwingDuration(self, stack, cir.getReturnValueI()));
        }

    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("TAIL"))
    private void knightlib$weaponSwingStart(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        final LivingEntity self = (LivingEntity) (Object) this;
        // swingTime is only reset when a new swing starts
        if (self.swingTime != -1 || self.swingingArm != hand) {
            return;
        }

        final ItemStack stack = self.getItemInHand(hand);
        if (stack.getItem() instanceof KnightLibWeaponItem weapon) {
            weapon.onSwingStart(self, hand, stack);
        }

    }

}