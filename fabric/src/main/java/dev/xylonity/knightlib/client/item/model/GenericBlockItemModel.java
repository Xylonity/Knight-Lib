package dev.xylonity.knightlib.client.item.model;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.item.blockitem.GenericBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;


public class GenericBlockItemModel extends GeoModel<GenericBlockItem> {

    private final String name;

    public GenericBlockItemModel(String name) {
        this.name = name;
    }

    @Override
    public ResourceLocation getModelResource(GenericBlockItem animatable) {
        return new ResourceLocation(KnightLib.MOD_ID, "geo/" + name + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GenericBlockItem animatable) {
        return new ResourceLocation(KnightLib.MOD_ID, "textures/block/" + name + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GenericBlockItem animatable) {
        return null;
    }

}
