package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.armor.KnightLibArmorMaterial;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.common.item.armor.GenericArmorItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class KnightLibFabricPlatform implements KnightLibPlatform {

    @Override
    public <T extends ParticleType<?>> Supplier<T> createParticle(boolean overrideLimiter) {
        return () -> (T) FabricParticleTypes.simple(overrideLimiter);
    }

    @Override
    public ArmorItem createArmorItem(KnightLibArmorMaterial material, ArmorItem.Type type, Item.Properties properties, ResourceLocation modelId) {
        return new GenericArmorItem(material.get(), type, properties, modelId);
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
        return new ExtendedScreenHandlerType<>(supplier::create);
    }

    @Override
    public <X extends Mob> Item createSpawnEgg(Supplier<EntityType<X>> entityType, int primaryColor, int secondaryColor, Item.Properties properties) {
        return new SpawnEggItem(entityType.get(), primaryColor, secondaryColor, properties);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData) {
        player.openMenu(new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
                extraData.accept(buf);
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
