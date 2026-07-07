package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.common.block.GreatChaliceBlock;
import dev.xylonity.knightlib.common.item.blockitem.GreatChaliceBlockItem;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.loader.api.FabricLoader;
import io.netty.buffer.Unpooled;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class KnightLibFabricPlatform implements KnightLibPlatform {

    /**
     * ExtendedScreenHandler now serializes a data object via a StreamCodec instead of writing into a buffer directly
     */
    private static final StreamCodec<RegistryFriendlyByteBuf, FriendlyByteBuf> MENU_DATA_CODEC = StreamCodec.of(
            (target, data) -> {
                final int readable = data.readableBytes();
                target.writeVarInt(readable);
                target.writeBytes(data, data.readerIndex(), readable);
            },
            source -> {
                final int readable = source.readVarInt();
                final FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer(Math.max(readable, 1)));
                copy.writeBytes(source, readable);
                return copy;
            }

    );

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
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(ResourceRegistry.BlockEntityFactory<T> supplier, Supplier<Block> block) {
        return BlockEntityType.Builder.of(supplier::create, block.get()).build(null);
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(ResourceRegistry.BlockEntityFactory<T> supplier, Supplier<Block>... blocks) {
        final Block[] blocksArray = Arrays.stream(blocks)
                .map(Supplier::get)
                .toArray(Block[]::new);

        return BlockEntityType.Builder.of(supplier::create, blocksArray).build(null);
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuFactory(ResourceRegistry.MenuFactory<T> supplier) {
        return new ExtendedScreenHandlerType<>(supplier::create, MENU_DATA_CODEC);
    }

    @Override
    public <X extends Mob> Item createSpawnEgg(Supplier<EntityType<X>> entityType, int primaryColor, int secondaryColor, Item.Properties properties) {
        return new SpawnEggItem(entityType.get(), primaryColor, secondaryColor, properties);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData) {
        player.openMenu(new ExtendedScreenHandlerFactory<FriendlyByteBuf>() {
            @Override
            public FriendlyByteBuf getScreenOpeningData(ServerPlayer serverPlayer) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                extraData.accept(buf);
                return buf;
            }

            @Override
            public @NotNull Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(int syncId, @NotNull Inventory inventory, @NotNull Player player) {
                return provider.createMenu(syncId, inventory, player);
            }

        });

    }

    @Override
    public boolean isPhysicalClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path resolveConfigFile(String configFileName) {
        return FabricLoader.getInstance().getConfigDir().resolve(configFileName);
    }

    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }

}
