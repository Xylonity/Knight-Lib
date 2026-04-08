package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.server.PlayerItemSmithedEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin extends ItemCombinerMenu {

    private SmithingMenuMixin() {
        super(null, 0, null, null);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void knightlib$onSmithingTake(Player player, ItemStack stack, CallbackInfo ci) {
        KnightLibEvents.SERVER.dispatch(new PlayerItemSmithedEvent(player, stack, this.inputSlots));
    }

}