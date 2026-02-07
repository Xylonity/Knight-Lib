package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Event to register render layers for blocks and fluids (for example, adding a cutout layer to a custom translucent block)
 */
public abstract class RenderLayerRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

    public void setBlockLayer(ResourceEntry<Block> blockEntry, RenderType renderType) {
        setBlockLayer(blockEntry.get(), renderType);
    }

    public abstract void setBlockLayer(Block block, RenderType renderType);

    public void setFluidLayer(ResourceEntry<Fluid> fluidEntry, RenderType renderType) {
        setFluidLayer(fluidEntry.get(), renderType);
    }

    public abstract void setFluidLayer(Fluid fluid, RenderType renderType);

    public void setCutout(Block block) {
        setBlockLayer(block, RenderType.cutout());
    }

    public void setCutoutMipped(Block block) {
        setBlockLayer(block, RenderType.cutoutMipped());
    }

    public void setTranslucent(Block block) {
        setBlockLayer(block, RenderType.translucent());
    }

    public void setCutout(ResourceEntry<Block> blockEntry) {
        setBlockLayer(blockEntry.get(), RenderType.cutout());
    }

    public void setCutoutMipped(ResourceEntry<Block> blockEntry) {
        setBlockLayer(blockEntry.get(), RenderType.cutoutMipped());
    }

    public void setTranslucent(ResourceEntry<Block> blockEntry) {
        setBlockLayer(blockEntry.get(), RenderType.translucent());
    }

}