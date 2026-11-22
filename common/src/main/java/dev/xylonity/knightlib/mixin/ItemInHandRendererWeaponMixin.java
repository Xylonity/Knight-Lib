package dev.xylonity.knightlib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.knightlib.api.client.item.KnightLibWeaponRenderHooks;
import dev.xylonity.knightlib.api.item.KnightLibWeaponItem;
import dev.xylonity.knightlib.client.item.KnightLibWeaponClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First person hooks of {@link KnightLibWeaponItem}
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererWeaponMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private ItemStack offHandItem;

    @Unique
    private int knightlib$selectedSlot = -1;

    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int seed);

    /**
     * Otherwise a new stack of the same weapon synced into the same slot would be taken as a different item being equipped
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void knightlib$keepWeaponEquipped(CallbackInfo ci) {
        final LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        final int selectedSlot = player.getInventory().selected;
        if (selectedSlot == knightlib$selectedSlot && knightlib$keepsEquipped(mainHandItem, player.getMainHandItem())) {
            mainHandItem = player.getMainHandItem();
        }

        if (knightlib$keepsEquipped(offHandItem, player.getOffhandItem())) {
            offHandItem = player.getOffhandItem();
        }

        knightlib$selectedSlot = selectedSlot;
    }

    /**
     * Doesn't lower the main hand weapon while the attack strength recharges
     */
    @ModifyVariable(method = "tick", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private float knightlib$keepWeaponRaised(float attackStrength) {
        final LocalPlayer player = minecraft.player;
        final ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof KnightLibWeaponItem weapon && weapon.keepRaisedWhileRecharging(player, stack) ? 1f : attackStrength;
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void knightlib$weaponFirstPerson(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, CallbackInfo ci) {
        if (player.isScoping()) {
            return;
        }

        final KnightLibWeaponRenderHooks hooks = KnightLibWeaponClient.hooks(stack);
        if (hooks == null) {
            return;
        }

        final HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        final boolean handled;

        poseStack.pushPose();

        try {
            handled = hooks.applyFirstPersonTransform(poseStack, player, hand, arm, stack, partialTicks, equippedProgress, swingProgress);
            if (handled) {
                renderItem(player, stack, arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, arm == HumanoidArm.LEFT, poseStack, buffer, combinedLight);
            }

        }
        finally {
            poseStack.popPose();
        }

        if (handled) {
            ci.cancel();
        }

    }

    @Unique
    private static boolean knightlib$keepsEquipped(ItemStack held, ItemStack current) {
        return held != current && held.getItem() instanceof KnightLibWeaponItem weapon && ItemStack.isSameItem(held, current) && weapon.keepEquippedOnUpdate(held, current);
    }

}