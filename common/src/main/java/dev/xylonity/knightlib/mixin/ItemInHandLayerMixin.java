package dev.xylonity.knightlib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.knightlib.api.client.item.KnightLibWeaponRenderHooks;
import dev.xylonity.knightlib.client.animation.layer.impl.KnightLibPassengerLayer;
import dev.xylonity.knightlib.client.item.KnightLibWeaponClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third person held item transforms of {@link dev.xylonity.knightlib.api.item.KnightLibWeaponItem}, and passengers
 * drawn without their held items by {@link KnightLibPassengerLayer}
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Unique
    private float knightlib$partialTick;

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true
    )
    private void knightlib$beginRender(PoseStack poseStack, MultiBufferSource buffer, int packedLight, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (KnightLibPassengerLayer.isHidingHeldItems()) {
            ci.cancel();
            return;
        }

        knightlib$partialTick = partialTick;
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void knightlib$weaponThirdPersonItem(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        final KnightLibWeaponRenderHooks hooks = KnightLibWeaponClient.hooks(stack);
        if (hooks != null) {
            hooks.applyThirdPersonItemTransform(poseStack, entity, stack, arm, displayContext, knightlib$partialTick);
        }

    }

}