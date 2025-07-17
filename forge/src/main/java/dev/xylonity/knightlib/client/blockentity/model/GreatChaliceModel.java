package dev.xylonity.knightlib.client.blockentity.model;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GreatChaliceModel extends GeoModel<GreatChaliceBlockEntity> {

    @Override
    public ResourceLocation getModelResource(GreatChaliceBlockEntity animatable) {
        return new ResourceLocation(KnightLib.MOD_ID, "geo/great_chalice.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GreatChaliceBlockEntity animatable) {
        return switch (animatable.getState()) {
            case CHAOTIC -> new ResourceLocation(KnightLib.MOD_ID, "textures/block/great_chalice_chaotic.png");
            default -> // EMPTY/NORMAL
                    new ResourceLocation(KnightLib.MOD_ID, "textures/block/great_chalice.png");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(GreatChaliceBlockEntity animatable) {
        return new ResourceLocation(KnightLib.MOD_ID, "animations/great_chalice.animation.json");
    }

}