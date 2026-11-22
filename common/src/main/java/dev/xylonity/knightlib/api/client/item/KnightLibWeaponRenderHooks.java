package dev.xylonity.knightlib.api.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Client transforms of a {@link dev.xylonity.knightlib.api.item.KnightLibWeaponItem}, supplied by its weaponRenderHooks()
 */
public interface KnightLibWeaponRenderHooks {

    /**
     * First person, transforms the pose (pushed before and popped after) for the item in this hand. Returning true
     * replaces vanilla's equip and swing transforms and KnightLib renders the item with this pose alone
     */
    default boolean applyFirstPersonTransform(PoseStack poseStack, AbstractClientPlayer player, InteractionHand hand, HumanoidArm arm, ItemStack stack, float partialTick, float equippedProgress, float swingProgress) {
        return false;
    }

    /**
     * Third person, called every frame while this item is in the swinging hand (the main hand when nothing swung
     * yet), even with no attack in progress. Returning true replaces vanilla's attack animation of the model
     */
    default boolean setupThirdPersonAttack(HumanoidModel<?> model, LivingEntity entity, InteractionHand hand, float ageInTicks) {
        return false;
    }

    /**
     * Third person, transforms this held item right before it renders in the hand, with the pose already at the hand and vanilla's
     * held item offset applied
     */
    default void applyThirdPersonItemTransform(PoseStack poseStack, LivingEntity entity, ItemStack stack, HumanoidArm arm, ItemDisplayContext displayContext, float partialTick) {
        ;;
    }

}