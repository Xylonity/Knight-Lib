package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Event to register block entity renderers
 */
public abstract class BlockEntityRendererRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

    public <T extends BlockEntity> void register(ResourceEntry<BlockEntityType<T>> blockEntityEntry, BlockEntityRendererProvider<T> rendererProvider) {
        register(blockEntityEntry.get(), rendererProvider);
    }

    public abstract <T extends BlockEntity> void register(BlockEntityType<T> blockEntityType, BlockEntityRendererProvider<T> rendererProvider);

}