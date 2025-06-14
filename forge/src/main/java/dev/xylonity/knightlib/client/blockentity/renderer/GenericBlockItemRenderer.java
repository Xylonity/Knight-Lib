package dev.xylonity.knightlib.client.blockentity.renderer;


import dev.xylonity.knightlib.client.blockentity.model.GenericBlockItemModel;
import dev.xylonity.knightlib.common.item.blockitem.GenericBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GenericBlockItemRenderer extends GeoItemRenderer<GenericBlockItem> {

    public GenericBlockItemRenderer(String name) {
        super(new GenericBlockItemModel(name));
    }

}
