package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.RenderLayerRegistrationEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class RenderLayerRegistrationEventNeoForge extends RenderLayerRegistrationEvent {

    @Override
    public void setBlockLayer(Block block, RenderType renderType) {
        ItemBlockRenderTypes.setRenderLayer(block, renderType);
    }

    @Override
    public void setFluidLayer(Fluid fluid, RenderType renderType) {
        ItemBlockRenderTypes.setRenderLayer(fluid, renderType);
    }

}