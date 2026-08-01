package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.client.entity.layer.BoneHitboxPicker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Shadow
    protected abstract void ensureHasSentCarriedItem();

    /**
     * Replaces the vanilla attack packet with a bone aware one
     */
    @Inject(
            method = "attack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void knightlib$attackBoneHitbox(Player player, Entity target, CallbackInfo ci) {
        if (!BoneHitboxPicker.hasSelection(target)) {
            return;
        }

        // Must precede the attack packet
        ensureHasSentCarriedItem();
        BoneHitboxPicker.attack(target);

        if (!player.isSpectator()) {
            player.attack(target);
            // Forge already resets it inside Player#attack but Fabric does not (thanks again fabric)
            player.resetAttackStrengthTicker();
        }

        ci.cancel();
    }

}