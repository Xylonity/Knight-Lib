package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.common.block.GreatChaliceBlock;
import dev.xylonity.knightlib.common.item.blockitem.GreatChaliceBlockItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class KnightLibBlocks {

    public static final ResourceRegistry<Block> BLOCKS = ResourceDispatcher.create(BuiltInRegistries.BLOCK, KnightLib.MOD_ID);

    public static final ResourceEntry<Block> GREAT_CHALICE = BLOCKS.registerBlock("great_chalice",
                    () -> new GreatChaliceBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(3f, 6f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.COPPER)
                            .noOcclusion()
                    ),
                    KnightLibItems.ITEMS,
                    block -> new GreatChaliceBlockItem(block, new Item.Properties(), KnightLib.of("great_chalice"))
            );

}