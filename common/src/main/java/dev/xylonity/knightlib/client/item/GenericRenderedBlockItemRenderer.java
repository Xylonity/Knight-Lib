package dev.xylonity.knightlib.client.item;

import dev.xylonity.knightlib.api.util.ResourceLocations;
import dev.xylonity.knightlib.client.animation.KnightLibModelSource;
import dev.xylonity.knightlib.client.animation.renderer.KnightLibItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Default renderer for a named animated block item using the knightlib animation system with a custom geo model.
 */
public final class GenericRenderedBlockItemRenderer extends KnightLibItemRenderer {

    private final ResourceLocation geometry;
    private final ResourceLocation texture;

    public GenericRenderedBlockItemRenderer(ResourceLocation id) {
        this.geometry = ResourceLocations.of(id.getNamespace(), "geo/" + id.getPath() + ".geo.json");
        this.texture = ResourceLocations.of(id.getNamespace(), "textures/block/" + id.getPath() + ".png");
    }

    @Override
    protected KnightLibModelSource defineModel(ItemStack stack) {
        return KnightLibModelSource.geo(geometry);
    }

    @Override
    public ResourceLocation getTextureLocation(ItemStack stack) {
        return texture;
    }

}