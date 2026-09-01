package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.animation.KnightLibItemAnimations;
import dev.xylonity.knightlib.api.item.KnightLibAnimatedItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ticks itemstack animation controllers through vanilla's inventory tick.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackAnimationMixin {

    @Inject(method = "inventoryTick", at = @At("RETURN"))
    private void knightlib$tickAnimations(Level level, Entity holder, int slot, boolean selected, CallbackInfo ci) {
        final ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof KnightLibAnimatedItem item && (level.isClientSide() || KnightLibItemAnimations.needsServerTick(stack))) {
            KnightLibItemAnimations.getAnimationHandler(item, stack, level, holder).tick();
        }

    }

}
