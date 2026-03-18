package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.common.block.GreatChaliceBlock;
import dev.xylonity.knightlib.common.item.blockitem.GreatChaliceBlockItem;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class KnightLibForgePlatform implements KnightLibPlatform {

    @Override
    public <T extends ParticleType<?>> Supplier<T> createParticle(boolean overrideLimiter) {
        return () -> (T) new SimpleParticleType(overrideLimiter);
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

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuFactory(ResourceRegistry.MenuFactory<T> supplier) {
        return IForgeMenuType.create(supplier::create);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData) {
        NetworkHooks.openScreen(player, provider, extraData);
    }

    @Override
    public boolean isPhysicalClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

}