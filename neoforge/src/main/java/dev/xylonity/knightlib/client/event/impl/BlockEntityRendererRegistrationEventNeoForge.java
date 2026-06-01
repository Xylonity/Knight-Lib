package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.BlockEntityRendererRegistrationEvent;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityRendererRegistrationEventNeoForge extends BlockEntityRendererRegistrationEvent {

    @Override
    public <T extends BlockEntity> void register(BlockEntityType<T> blockEntityType, BlockEntityRendererProvider<T> rendererProvider) {
        BlockEntityRenderers.register(blockEntityType, rendererProvider);
    }

}