package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.block.GreatChaliceBlock;
import dev.xylonity.knightlib.common.item.*;
import dev.xylonity.knightlib.common.item.blockitem.GenericBlockItem;
import dev.xylonity.knightlib.common.item.blockitem.GreatChaliceBlockItem;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class KnightLibNeoForgePlatform implements KnightLibPlatform {

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
            case HOMUNCULUS -> (Supplier<T>) registerItem(id, () -> new HomunculusItem(properties));
            default -> // FILLED_GRAIL
                    (Supplier<T>) registerItem(id, () -> new FilledGrailItem(properties));
        };
    }

    @Override
    public <T extends Block> Supplier<T> registerBlock(String id, BlockBehaviour.Properties properties, KnightLibBlocks.BlockType blockType, KnightLibBlocks.BlockItem blockItem) {
        Supplier<T> tr = switch (blockType) {
            default -> // CHALICE
                    (Supplier<T>) KnightLib.BLOCKS.register(id, () -> new GreatChaliceBlock(properties));
        };

        Supplier<Item> item = switch (blockItem) {
            case GREAT_CHALICE -> () -> new GreatChaliceBlockItem(tr.get(), new Item.Properties(), id);
            default -> // GENERIC
                    () -> new GenericBlockItem(tr.get(), new Item.Properties(), id);
        };

        registerItem(id, item);

        return tr;
    }

    @Override
    public <T extends ParticleType<?>> Supplier<T> registerParticle(String id, boolean overrideLimiter) {
        return KnightLib.PARTICLES.register(id, () -> (T) new SimpleParticleType(overrideLimiter));
    }

}
