package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.common.block.GreatChaliceBlock;
import dev.xylonity.knightlib.common.item.*;
import dev.xylonity.knightlib.common.item.blockitem.GenericBlockItem;
import dev.xylonity.knightlib.common.item.blockitem.GreatChaliceBlockItem;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class KnightLibFabricPlatform implements KnightLibPlatform {

    @Override
    public <T extends ParticleType<?>> Supplier<T> createParticle(boolean overrideLimiter) {
        return () -> (T) FabricParticleTypes.simple(overrideLimiter);
    }

    @Override
    public <T extends Block> Supplier<T> createBlock(String id, BlockBehaviour.Properties properties, KnightLibBlocks.BlockType blockType) {
        return switch (blockType) {
            default -> // CHALICE
                    () -> (T) new GreatChaliceBlock(properties);
        };
    }

    @Override
    public <T extends Item> Supplier<T> createItem(String id, Item.Properties properties, KnightLibItems.ItemType itemType) {
        return switch (itemType) {
            default -> // CHALICE
                    () -> (T) new GreatChaliceBlockItem(KnightLibBlocks.GREAT_CHALICE.get(), properties, id);
        };
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> createBlockEntityType(KnightLibBlockEntities.BlockEntityFactory<T> supplier, Supplier<Block> block, String id) {
        return () -> BlockEntityType.Builder.of(supplier::create, block.get()).build(null);
    }

}
