package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import dev.xylonity.knightlib.api.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.api.registrar.ResourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class KnightLibBlockEntities {

    public static final ResourceRegistry<BlockEntityType<?>> BLOCK_ENTITIES = ResourceDispatcher.create(ResourceType.BLOCK_ENTITIES, KnightLib.MOD_ID);

    public static final ResourceEntry<BlockEntityType<GreatChaliceBlockEntity>> GREAT_CHALICE;

    static {
        GREAT_CHALICE = BLOCK_ENTITIES.register("great_chalice", KnightLib.PLATFORM.createBlockEntityType(GreatChaliceBlockEntity::new, KnightLibBlocks.GREAT_CHALICE, "great_chalice"));
    }

    @FunctionalInterface
    public interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

}
