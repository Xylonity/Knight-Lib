package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.RenderLayerRegistrationEvent;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public class RenderLayerRegistrationEventFabric extends RenderLayerRegistrationEvent {

    @Override
    public void setBlockLayer(Block block, RenderType renderType) {
        BlockRenderLayerMap.INSTANCE.putBlock(block, renderType);
    }

    @Override
    public void setFluidLayer(Fluid fluid, RenderType renderType) {
        BlockRenderLayerMap.INSTANCE.putFluid(fluid, renderType);
    }

}