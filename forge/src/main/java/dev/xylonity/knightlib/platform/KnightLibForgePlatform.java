package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.block.GreatChaliceBlock;
import dev.xylonity.knightlib.common.item.blockitem.GenericBlockItem;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class KnightLibForgePlatform implements KnightLibPlatform {

    @Override
    public <T extends Item> Supplier<T> registerItem(String id, Supplier<T> item) {
        return KnightLib.ITEMS.register(id, item);
    }

    @Override
    public <T extends Block> Supplier<T> registerBlock(String id, BlockBehaviour.Properties properties, KnightLibBlocks.BlockType blockType) {
        RegistryObject<T> tr = switch (blockType) {
            default -> // CHALICE
                    (RegistryObject<T>) KnightLib.BLOCKS.register(id, () -> new GreatChaliceBlock(properties));
        };

        registerItem(id, () -> new GenericBlockItem(tr.get(), new Item.Properties(), id));
        return tr;
    }

    //@Override
    //public int getGreatChaliceLightLevel(BlockState state) {
    //    return state.getValue(GreatChaliceBlock.LIT);
    //}

}
