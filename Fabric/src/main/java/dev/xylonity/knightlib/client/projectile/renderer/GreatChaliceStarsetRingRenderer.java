package dev.xylonity.knightlib.client.projectile.renderer;

import dev.xylonity.knightlib.client.projectile.model.GreatChaliceStarsetRingModel;
import dev.xylonity.knightlib.common.entity.projectile.GreatChaliceStartsetRing;
import mod.azure.azurelib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GreatChaliceStarsetRingRenderer extends GeoEntityRenderer<GreatChaliceStartsetRing> {

    public GreatChaliceStarsetRingRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GreatChaliceStarsetRingModel());
    }

}