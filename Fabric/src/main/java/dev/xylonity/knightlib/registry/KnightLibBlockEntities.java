package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class KnightLibBlockEntities {

    public static void init() { ;; }

    public static final BlockEntityType<GreatChaliceBlockEntity> GREAT_CHALICE;

    static {
        GREAT_CHALICE = register("great_chalice", GreatChaliceBlockEntity::new, KnightLibBlocks.GREAT_CHALICE);
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<Block> block) {
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, new ResourceLocation(KnightLib.MOD_ID, name), BlockEntityType.Builder.of(factory, block.get()).build(null));
    }

}