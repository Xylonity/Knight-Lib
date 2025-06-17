package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.common.block.GreatChaliceBlock;
import dev.xylonity.knightlib.common.item.*;
import dev.xylonity.knightlib.common.item.blockitem.GenericBlockItem;
import dev.xylonity.knightlib.common.item.blockitem.GreatChaliceBlockItem;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class KnightLibFabricPlatform implements KnightLibPlatform {

    @Override
    public <T extends Item> Supplier<T> registerItem(String id, Supplier<T> item) {
        return registerSupplier(BuiltInRegistries.ITEM, id, item);
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
                    (Supplier<T>) registerSupplier(BuiltInRegistries.BLOCK, id, () -> new GreatChaliceBlock(properties));
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
        return registerSupplier(BuiltInRegistries.PARTICLE_TYPE, id, () -> (T) FabricParticleTypes.simple(overrideLimiter));
    }

    private static <T, R extends Registry<? super T>> Supplier<T> registerSupplier(R registry, String id, Supplier<T> factory) {
        T value = factory.get();
        Registry.register((Registry<T>) registry, new ResourceLocation(KnightLibCommon.MOD_ID, id), value);
        return () -> value;
    }


}
