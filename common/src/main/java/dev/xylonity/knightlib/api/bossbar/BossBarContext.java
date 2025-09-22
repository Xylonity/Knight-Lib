package dev.xylonity.knightlib.api.bossbar;

import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public record BossBarContext(LerpingBossEvent event, @Nullable Entity entity, @Nullable ResourceLocation entityType) {
    ;;
}
