package dev.xylonity.knightlib.client.projectile.model;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.entity.projectile.GreatChaliceStartsetRing;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

public class GreatChaliceStarsetRingModel extends GeoModel<GreatChaliceStartsetRing> {

    @Override
    public ResourceLocation getModelResource(GreatChaliceStartsetRing animatable) {
        return new ResourceLocation(KnightLib.MOD_ID, "geo/great_chalice_starset_ring.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GreatChaliceStartsetRing animatable) {
        int frames = 10;
        int perTick = 1;

        int frameIndex = (animatable.tickCount / perTick) % frames;
        return new ResourceLocation(KnightLib.MOD_ID, String.format("textures/entity/great_chalice_starset_ring_%d.png", frameIndex));
    }

    @Override
    public ResourceLocation getAnimationResource(GreatChaliceStartsetRing animatable) {
        return null;
    }

}
