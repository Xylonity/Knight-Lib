package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.block.GreatChaliceBlock;
import dev.xylonity.knightlib.common.item.EmptyGrailItem;
import dev.xylonity.knightlib.common.item.FilledGrailItem;
import dev.xylonity.knightlib.common.item.GreatEssenceItem;
import dev.xylonity.knightlib.common.item.SmallEssenceItem;
import dev.xylonity.knightlib.common.item.blockitem.GenericBlockItem;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
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
    public <T extends Item> Supplier<T> registerSpecificItem(String id, Item.Properties properties, KnightLibItems.ItemType itemType) {
        return switch (itemType) {
            case SMALL_ESSENCE -> (Supplier<T>) registerItem(id, () -> new SmallEssenceItem(properties));
            case GREAT_ESSENCE -> (Supplier<T>) registerItem(id, () -> new GreatEssenceItem(properties));
            case EMPTY_GRAIL -> (Supplier<T>) registerItem(id, () -> new EmptyGrailItem(properties));
            default -> // FILLED_GRAIL
                    (Supplier<T>) registerItem(id, () -> new FilledGrailItem(properties));
        };
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
