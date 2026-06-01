package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.server.LivingUseItemFinishEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class LivingEntityUseItemMixin {

    @Redirect(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack knightlib$onFinishUsingItem(ItemStack instance, Level level, LivingEntity entity) {
        final ItemStack result = instance.finishUsingItem(level, entity);
        final LivingUseItemFinishEvent event = new LivingUseItemFinishEvent(entity, instance, result);
        KnightLibEvents.SERVER.dispatch(event);
        return event.getResult();
    }

}