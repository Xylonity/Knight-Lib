package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.api.registrar.ResourceType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class KnightLibBlocks {

    public static final ResourceRegistry<Block> BLOCKS = ResourceDispatcher.create(ResourceType.BLOCKS, KnightLib.MOD_ID);

    public static final ResourceEntry<Block> GREAT_CHALICE =
            BLOCKS.register("great_chalice",
                    KnightLib.PLATFORM.createBlock("great_chalice",
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(3f, 6f)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.COPPER)
                                    .noOcclusion(),
                            KnightLibBlocks.BlockType.GREAT_CHALICE
                    )
            );

    public enum BlockType {
        GREAT_CHALICE
    }

}
