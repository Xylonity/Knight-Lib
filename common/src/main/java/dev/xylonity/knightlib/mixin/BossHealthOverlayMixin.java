package dev.xylonity.knightlib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xylonity.knightlib.api.impl.BossBarApi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {

    @Unique
    private LerpingBossEvent knightlib$lastEvent;
    @Unique
    private boolean knightlib$skipNextName;

    @Invoker("drawBar")
    public abstract void knightlib$drawBarAccessor(PoseStack poseStack, int x, int y, BossEvent event);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lcom/mojang/blaze3d/vertex/PoseStack;IILnet/minecraft/world/BossEvent;)V"))
    private void knightlib$drawBar(BossHealthOverlay self, PoseStack poseStack, int x, int y, BossEvent rawEvent) {
        LerpingBossEvent event = (LerpingBossEvent) rawEvent;
        this.knightlib$lastEvent = event;

        Optional<BossBarApi.BossBarEntry> entry = BossBarApi.match(event);
        if (entry.isPresent()) {
            BossBarApi.BossBarEntry e = entry.get();
            this.knightlib$skipNextName = e.hideVanillaName();
            e.renderer().render(poseStack, event, x, y);
        } else {
            this.knightlib$skipNextName = false;
            knightlib$drawBarAccessor(poseStack, x, y, event);
        }

    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I"))
    private int knightlib$shouldSkipName(Font font, PoseStack poseStack, Component text, float x, float y, int color) {
        if (this.knightlib$skipNextName) {
            this.knightlib$skipNextName = false;
            return 0;
        }

        return font.drawShadow(poseStack, text, x, y, color);
    }

    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 1), index = 3)
    private int knightlib$extraPadding(int currentYOffset) {
        if (this.knightlib$lastEvent == null) return currentYOffset;

        return currentYOffset + BossBarApi.match(this.knightlib$lastEvent).map(BossBarApi.BossBarEntry::extraYPadding).orElse(0);
    }

}
