package dev.xylonity.knightlib.client.projectile.model;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.entity.projectile.GreatChaliceStartsetRing;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GreatChaliceStarsetRingModel extends GeoModel<GreatChaliceStartsetRing> {

    @Override
    public ResourceLocation getModelResource(GreatChaliceStartsetRing animatable) {
        return KnightLib.of("geo/great_chalice_starset_ring.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GreatChaliceStartsetRing animatable) {
        final int frames = 10;
        final int perTick = 1;

        int frameIndex = (animatable.tickCount / perTick) % frames;
        return KnightLib.of(String.format("textures/entity/great_chalice_starset_ring_%d.png", frameIndex));
    }

    @Override
    public ResourceLocation getAnimationResource(GreatChaliceStartsetRing animatable) {
        return null;
    }

}
