package dev.xylonity.knightlib.client.projectile.renderer;

import dev.xylonity.knightlib.client.projectile.model.GreatChaliceStarsetRingModel;
import dev.xylonity.knightlib.common.entity.projectile.GreatChaliceStartsetRing;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GreatChaliceStarsetRingRenderer extends GeoEntityRenderer<GreatChaliceStartsetRing> {

    public GreatChaliceStarsetRingRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GreatChaliceStarsetRingModel());
    }

}