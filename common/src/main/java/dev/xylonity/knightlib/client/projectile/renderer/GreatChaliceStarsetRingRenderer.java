package dev.xylonity.knightlib.client.projectile.renderer;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.client.animation.KnightLibModelSource;
import dev.xylonity.knightlib.client.animation.renderer.KnightLibEntityRenderer;
import dev.xylonity.knightlib.common.entity.projectile.GreatChaliceStartsetRing;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class GreatChaliceStarsetRingRenderer extends KnightLibEntityRenderer<GreatChaliceStartsetRing> {

    public GreatChaliceStarsetRingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected KnightLibModelSource defineModel(GreatChaliceStartsetRing ring) {
        return KnightLibModelSource.geo(KnightLib.of("geo/great_chalice_starset_ring.geo.json"));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull GreatChaliceStartsetRing ring) {
        return KnightLib.of("textures/entity/great_chalice_starset_ring_" + ring.tickCount % 10 + ".png");
    }

}