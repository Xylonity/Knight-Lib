package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.bossbar.BossBarContext;
import dev.xylonity.knightlib.client.screen.bossbar.BossBarApi;
import dev.xylonity.knightlib.client.screen.bossbar.BossBarLinks;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
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
    public abstract void knightlib$drawBarAccessor(GuiGraphics gui, int x, int y, BossEvent event);

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V"))
    private void knightlib$drawBar(BossHealthOverlay self, GuiGraphics gui, int x, int y, BossEvent rawEvent) {
        LerpingBossEvent boss = (LerpingBossEvent) rawEvent;
        this.knightlib$lastEvent = boss;

        Optional<BossBarApi.BossBarEntry> match = BossBarApi.match(boss);
        if (match.isPresent()) {
            BossBarApi.BossBarEntry entry = match.get();

            BossBarLinks.Reference reference = BossBarLinks.INSTANCE.get(boss.getId());
            Entity entity = reference != null ? reference.resolve() : null;
            BossBarContext ctx = new BossBarContext(boss, entity, reference != null ? reference.entityType() : null);

            this.knightlib$skipNextName = entry.hideVanillaName();

            if (entry.renderer() != null) {
                entry.renderer().render(gui, ctx, x, y);
                return;
            }

            if (entry.legacyRenderer() != null) {
                entry.legacyRenderer().render(gui, boss, x, y);
                return;
            }

            this.knightlib$skipNextName = false;
            knightlib$drawBarAccessor(gui, x, y, boss);
            return;
        }

        this.knightlib$skipNextName = false;
        knightlib$drawBarAccessor(gui, x, y, boss);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    private int knightlib$shouldSkipName(GuiGraphics gui, Font font, Component text, int x, int y, int color) {
        if (this.knightlib$skipNextName) {
            this.knightlib$skipNextName = false;
            return x;
        }

        return gui.drawString(font, text, x, y, color);
    }

    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 1), index = 3)
    private int knightlib$extraPadding(int j) {
        if (this.knightlib$lastEvent == null) return j;

        return j + BossBarApi.match(this.knightlib$lastEvent).map(BossBarApi.BossBarEntry::extraYPadding).orElse(0);
    }

}
