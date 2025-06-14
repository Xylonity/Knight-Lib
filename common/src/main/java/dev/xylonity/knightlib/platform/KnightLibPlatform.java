package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.registry.KnightLibBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public interface KnightLibPlatform {

    <T extends Item> Supplier<T> registerItem(String id, Supplier<T> item);
    <T extends Block> Supplier<T> registerBlock(String id, BlockBehaviour.Properties properties, KnightLibBlocks.BlockType blockType);

    //int getGreatChaliceLightLevel(BlockState state);

}
