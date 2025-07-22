package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLibCommon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

import java.util.function.Supplier;

public class KnightLibBlocks {

    public static void init() { ;; }

    public static final Supplier<Block> GREAT_CHALICE;

    static {

        GREAT_CHALICE = registerBlock("great_chalice",
                BlockBehaviour.Properties.of(Material.METAL)
                        .strength(3f, 6f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.COPPER)
                        .noOcclusion(), BlockType.GREAT_CHALICE, BlockItem.GREAT_CHALICE);
    }

    private static <T extends Block> Supplier<T> registerBlock(String id, BlockBehaviour.Properties properties, BlockType blockType, BlockItem blockItem) {
        return KnightLibCommon.PLATFORM.registerBlock(id, properties, blockType, blockItem);
    }

    public enum BlockType {
        GREAT_CHALICE
    }

    public enum BlockItem {
        GREAT_CHALICE,
        GENERIC
    }

}
