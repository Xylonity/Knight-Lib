package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public interface KnightLibPlatform {
    <T extends ParticleType<?>> Supplier<T> createParticle(boolean overrideLimiter);
    <T extends Block> Supplier<T> createBlock(String id, BlockBehaviour.Properties properties, KnightLibBlocks.BlockType blockType);
    <T extends Item> Supplier<T> createItem(String id, Item.Properties properties, KnightLibItems.ItemType itemType);
    <T extends BlockEntity> Supplier<BlockEntityType<T>> createBlockEntityType(KnightLibBlockEntities.BlockEntityFactory<T> factory, Supplier<Block> block, String id);

    <T extends AbstractContainerMenu> MenuType<T> createMenuFactory(ResourceRegistry.MenuFactory<T> supplier);

    CreativeModeTab.Builder creativeTabBuilder();
}