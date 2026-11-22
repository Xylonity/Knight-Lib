package dev.xylonity.knightlib.api.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * Capability for item subclasses with their own melee swing (spears, gauntlets...).
 * Every hook defaults to vanilla, so an item only overrides what it changes. The client transforms live in
 * {@link dev.xylonity.knightlib.api.client.item.KnightLibWeaponRenderHooks}, supplied by {@link #weaponRenderHooks()}
 */
public interface KnightLibWeaponItem {

    /**
     * Ticks a swing of this item lasts, which also stretches the arm animation and {@link LivingEntity#getAttackAnim}.
     * vanillaDuration already includes haste and mining fatigue
     */
    default int getSwingDuration(LivingEntity entity, ItemStack stack, int vanillaDuration) {
        return vanillaDuration;
    }

    /**
     * Called on both sides when a swing of this item starts. Swings ignored by vanilla because the previous one is still in its first half don't call it
     */
    default void onSwingStart(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        ;;
    }

    /**
     * Client only, whether a new stack of this same item synced into the same slot (damage, components...) keeps the
     * first person item raised instead of playing the equip animation again
     */
    default boolean keepEquippedOnUpdate(ItemStack held, ItemStack updated) {
        return false;
    }

    /**
     * Client only, whether the first person item stays raised while the attack strength recharges after a swing, instead of lowering
     */
    default boolean keepRaisedWhileRecharging(Player player, ItemStack stack) {
        return false;
    }

    /**
     * Returns a supplier for this item's client render hooks, or null for none. Implementations must return a
     * synthetic reference (() -> ()) to avoid calling client-sided packages on the server
     */
    default Supplier<Object> weaponRenderHooks() {
        return null;
    }

}